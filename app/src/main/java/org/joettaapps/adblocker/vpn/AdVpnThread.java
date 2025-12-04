package org.joettaapps.adblocker.vpn;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;

import org.joettaapps.adblocker.Configuration;
import org.joettaapps.adblocker.FileHelper;
import org.joettaapps.adblocker.MainActivity;
import org.pcap4j.packet.IpPacket;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

class AdVpnThread implements Runnable, DnsPacketProxy.EventLoop {
    private static final String TAG = "AdVpnThread";

    private static final int MIN_RETRY_TIME = 5;
    private static final int MAX_RETRY_TIME = 120;
    private static final long RETRY_RESET_SEC = 60;
    private static final int DNS_MAXIMUM_WAITING = 1024;
    private static final long DNS_TIMEOUT_SEC = 10;

    final ArrayList<InetAddress> upstreamDnsServers = new ArrayList<>();
    private final VpnService vpnService;
    private final Notify notify;

    private final Queue<byte[]> deviceWrites = new LinkedList<>();
    private final WospList dnsIn = new WospList();
    private final DnsPacketProxy dnsPacketProxy = new DnsPacketProxy(this);
    private final VpnWatchdog vpnWatchDog = new VpnWatchdog();

    private Thread thread = null;
    private FileDescriptor mBlockFd = null;
    private FileDescriptor mInterruptFd = null;

    public AdVpnThread(VpnService vpnService, Notify notify) {
        this.vpnService = vpnService;
        this.notify = notify;
    }

    private static List<InetAddress> getDnsServers(Context context) throws VpnNetworkException {
        Set<InetAddress> known = new HashSet<>();
        List<InetAddress> out = new ArrayList<>();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network network = cm.getActiveNetwork();
        if (network == null) throw new VpnNetworkException("No active network");

        for (Network nw : cm.getAllNetworks()) {
            for (InetAddress addr : cm.getLinkProperties(nw).getDnsServers()) {
                if (known.add(addr)) out.add(addr);
            }
        }
        return out;
    }

    public void startThread() {
        Log.i(TAG, "Starting VPN Thread");
        thread = new Thread(this, "AdVpnThread");
        thread.start();
    }

    public void stopThread() {
        Log.i(TAG, "Stopping VPN Thread");
        if (thread != null) thread.interrupt();

        mInterruptFd = FileHelper.closeOrWarn(mInterruptFd, TAG, "stopThread: Could not close interruptFd");
        try {
            if (thread != null) thread.join(2000);
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted while joining thread", e);
        }
        thread = null;
        Log.i(TAG, "VPN Thread stopped");
    }

    @Override
    public synchronized void run() {
        Log.i(TAG, "VPN Thread running");

        try {
            dnsPacketProxy.initialize(vpnService, upstreamDnsServers);
            vpnWatchDog.initialize(FileHelper.loadCurrentSettings(vpnService).watchDog);
        } catch (InterruptedException e) {
            return;
        }

        if (notify != null) notify.run(AdVpnService.VPN_STATUS_STARTING);

        int retryTimeout = MIN_RETRY_TIME;
        while (true) {
            long startTime = System.currentTimeMillis();
            try {
                runVpn();
                notify.run(AdVpnService.VPN_STATUS_STOPPING);
                break;
            } catch (InterruptedException e) {
                break;
            } catch (VpnNetworkException e) {
                Log.w(TAG, "Network exception, retrying...", e);
                if (notify != null) notify.run(AdVpnService.VPN_STATUS_RECONNECTING_NETWORK_ERROR);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in VPN thread", e);
                if (notify != null) notify.run(AdVpnService.VPN_STATUS_RECONNECTING_NETWORK_ERROR);
            }

            if (System.currentTimeMillis() - startTime >= RETRY_RESET_SEC * 1000) {
                retryTimeout = MIN_RETRY_TIME;
            }

            try {
                Thread.sleep(retryTimeout * 1000L);
            } catch (InterruptedException e) {
                break;
            }

            if (retryTimeout < MAX_RETRY_TIME) retryTimeout *= 2;
        }

        if (notify != null) notify.run(AdVpnService.VPN_STATUS_STOPPED);
        Log.i(TAG, "VPN Thread exiting");
    }

    private void runVpn() throws InterruptedException, IOException, VpnNetworkException {
        byte[] packet = new byte[32767];
        FileDescriptor[] pipes = null;
        try {
            pipes = Os.pipe();
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
        mInterruptFd = pipes[0];
        mBlockFd = pipes[1];

        try (ParcelFileDescriptor pfd = configure()) {
            FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
            FileOutputStream outputStream = new FileOutputStream(pfd.getFileDescriptor());

            if (notify != null) notify.run(AdVpnService.VPN_STATUS_RUNNING);

            while (doOne(inputStream, outputStream, packet));
        } finally {
            mBlockFd = FileHelper.closeOrWarn(mBlockFd, TAG, "runVpn: Could not close blockFd");
        }
    }

    private boolean doOne(FileInputStream inputStream, FileOutputStream outputStream, byte[] packet)
            throws IOException, InterruptedException, VpnNetworkException {

        StructPollfd deviceFd = new StructPollfd();
        deviceFd.fd = inputStream.getFD();
        deviceFd.events = (short) OsConstants.POLLIN;
        StructPollfd blockFd = new StructPollfd();
        blockFd.fd = mBlockFd;
        blockFd.events = (short) (OsConstants.POLLHUP | OsConstants.POLLERR);

        if (!deviceWrites.isEmpty()) deviceFd.events |= OsConstants.POLLOUT;

        StructPollfd[] polls = new StructPollfd[2 + dnsIn.size()];
        polls[0] = deviceFd;
        polls[1] = blockFd;
        int i = 0;
        for (WaitingOnSocketPacket wosp : dnsIn) {
            polls[2 + i] = new StructPollfd();
            polls[2 + i].fd = ParcelFileDescriptor.fromDatagramSocket(wosp.socket).getFileDescriptor();
            polls[2 + i].events = (short) OsConstants.POLLIN;
            i++;
        }

        int result = 0;
        try {
            result = FileHelper.poll(polls, vpnWatchDog.getPollTimeout());
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
        if (result == 0) {
            vpnWatchDog.handleTimeout();
            return true;
        }

        if (blockFd.revents != 0) return false;

        i = 0;
        Iterator<WaitingOnSocketPacket> iter = dnsIn.iterator();
        while (iter.hasNext()) {
            WaitingOnSocketPacket wosp = iter.next();
            if ((polls[2 + i].revents & OsConstants.POLLIN) != 0) {
                iter.remove();
                handleRawDnsResponse(wosp.packet, wosp.socket);
                wosp.socket.close();
            }
            i++;
        }

        if ((deviceFd.revents & OsConstants.POLLOUT) != 0) writeToDevice(outputStream);
        if ((deviceFd.revents & OsConstants.POLLIN) != 0) readPacketFromDevice(inputStream, packet);

        return true;
    }

    private void writeToDevice(FileOutputStream out) throws VpnNetworkException {
        try {
            byte[] data = deviceWrites.poll();
            if (data != null) out.write(data);
        } catch (IOException e) {
            throw new VpnNetworkException("Cannot write to VPN device", e);
        }
    }

    private void readPacketFromDevice(FileInputStream inputStream, byte[] packet)
            throws VpnNetworkException {
        try {
            int length = inputStream.read(packet);
            if (length <= 0) return;

            byte[] readPacket = Arrays.copyOf(packet, length);
            vpnWatchDog.handlePacket(readPacket);
            dnsPacketProxy.handleDnsRequest(readPacket);
        } catch (IOException e) {
            throw new VpnNetworkException("Cannot read from VPN device", e);
        }
    }

    public void forwardPacket(DatagramPacket outPacket, IpPacket parsedPacket) throws VpnNetworkException {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            vpnService.protect(socket);
            socket.send(outPacket);

            if (parsedPacket != null) dnsIn.add(new WaitingOnSocketPacket(socket, parsedPacket));
            else FileHelper.closeOrWarn(socket, TAG, "forwardPacket: Cannot close socket");
        } catch (IOException e) {
            FileHelper.closeOrWarn(socket, TAG, "forwardPacket: Error sending packet");
        }
    }

    private void handleRawDnsResponse(IpPacket packet, DatagramSocket socket) throws IOException {
        byte[] data = new byte[1024];
        DatagramPacket dp = new DatagramPacket(data, data.length);
        socket.receive(dp);
        dnsPacketProxy.handleDnsResponse(packet, data);
    }

    public void queueDeviceWrite(IpPacket ipOutPacket) {
        deviceWrites.add(ipOutPacket.getRawData());
    }

    // Configure the VPN interface
    private ParcelFileDescriptor configure() throws VpnNetworkException {
        Configuration config = FileHelper.loadCurrentSettings(vpnService);
        List<InetAddress> dnsServers = getDnsServers(vpnService);

        VpnService.Builder builder = vpnService.new Builder();
        builder.addAddress("192.168.50.1", 24);

        // Add IPv6 if supported
        if (hasIpV6Servers(config, dnsServers)) {
            try {
                builder.addAddress(Inet6Address.getByName("fd00::1"), 120);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        upstreamDnsServers.clear();
        for (InetAddress addr : dnsServers) {
            upstreamDnsServers.add(addr);
            builder.addDnsServer(addr);
        }

        builder.setBlocking(true)
                .allowBypass()
                .allowFamily(OsConstants.AF_INET)
                .allowFamily(OsConstants.AF_INET6);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            builder.setMetered(false);

        configurePackages(builder, config);

        return builder.setSession("DNS66")
                .setConfigureIntent(
                        PendingIntent.getActivity(vpnService, 1, new Intent(vpnService, MainActivity.class),
                                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .establish();
    }

    boolean hasIpV6Servers(Configuration config, List<InetAddress> dnsServers) {
        if (!config.ipV6Support) return false;
        for (InetAddress addr : dnsServers) if (addr instanceof Inet6Address) return true;
        return false;
    }

    void configurePackages(VpnService.Builder builder, Configuration config) {
        Set<String> allow = new HashSet<>();
        Set<String> disallow = new HashSet<>();
        config.allowlist.resolve(vpnService.getPackageManager(), allow, disallow);

        if (config.allowlist.defaultMode == Configuration.Allowlist.DEFAULT_MODE_NOT_ON_VPN) {
            for (String pkg : allow) {
                try { builder.addAllowedApplication(pkg); } catch (Exception e) { Log.w(TAG, e); }
            }
        } else {
            for (String pkg : disallow) {
                try { builder.addDisallowedApplication(pkg); } catch (Exception e) { Log.w(TAG, e); }
            }
        }
    }

    public interface Notify {
        void run(int value);
    }

    static class VpnNetworkException extends Exception {
        VpnNetworkException(String s) { super(s); }
        VpnNetworkException(String s, Throwable t) { super(s, t); }
    }

    private static class WaitingOnSocketPacket {
        final DatagramSocket socket;
        final IpPacket packet;
        private final long time;

        WaitingOnSocketPacket(DatagramSocket socket, IpPacket packet) {
            this.socket = socket;
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        long ageSeconds() { return (System.currentTimeMillis() - time) / 1000; }
    }

    private static class WospList implements Iterable<WaitingOnSocketPacket> {
        private final LinkedList<WaitingOnSocketPacket> list = new LinkedList<>();

        void add(WaitingOnSocketPacket wosp) {
            if (list.size() > DNS_MAXIMUM_WAITING) {
                FileHelper.closeOrWarn(list.removeFirst().socket, TAG, "WospList: Remove old socket");
            }
            while (!list.isEmpty() && list.getFirst().ageSeconds() > DNS_TIMEOUT_SEC) {
                FileHelper.closeOrWarn(list.removeFirst().socket, TAG, "WospList: Timeout socket");
            }
            list.add(wosp);
        }

        public Iterator<WaitingOnSocketPacket> iterator() { return list.iterator(); }
        int size() { return list.size(); }
    }
}
