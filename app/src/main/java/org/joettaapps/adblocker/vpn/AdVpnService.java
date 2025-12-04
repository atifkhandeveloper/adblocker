package org.joettaapps.adblocker.vpn;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.joettaapps.adblocker.Configuration;
import org.joettaapps.adblocker.FileHelper;
import org.joettaapps.adblocker.MainActivity;
import org.joettaapps.adblocker.NotificationChannels;
import org.joettaapps.adblocker.R;

import java.lang.ref.WeakReference;

public class AdVpnService extends VpnService implements Handler.Callback {

    public static final int NOTIFICATION_ID_STATE = 10;
    public static final int REQUEST_CODE_START = 43;
    public static final int REQUEST_CODE_PAUSE = 42;

    public static final int VPN_STATUS_STARTING = 0;
    public static final int VPN_STATUS_RUNNING = 1;
    public static final int VPN_STATUS_STOPPING = 2;
    public static final int VPN_STATUS_WAITING_FOR_NETWORK = 3;
    public static final int VPN_STATUS_RECONNECTING = 4;
    public static final int VPN_STATUS_RECONNECTING_NETWORK_ERROR = 5;
    public static final int VPN_STATUS_STOPPED = 6;
    public static final String VPN_UPDATE_STATUS_INTENT = "org.jak_linux.dns66.VPN_UPDATE_STATUS";
    public static final String VPN_UPDATE_STATUS_EXTRA = "VPN_STATUS";

    private static final int VPN_MSG_STATUS_UPDATE = 0;
    private static final int VPN_MSG_NETWORK_CHANGED = 1;
    private static final String TAG = "AdVpnService";

    public static int vpnStatus = VPN_STATUS_STOPPED;

    // Handler with WeakReference to avoid memory leaks
    private static class MyHandler extends Handler {
        private final WeakReference<Handler.Callback> callback;
        public MyHandler(Handler.Callback callback) {
            this.callback = new WeakReference<>(callback);
        }
        @Override
        public void handleMessage(Message msg) {
            Handler.Callback cb = this.callback.get();
            if (cb != null) cb.handleMessage(msg);
            super.handleMessage(msg);
        }
    }

    private final Handler handler = new MyHandler(this);
    private AdVpnThread vpnThread = new AdVpnThread(this, value ->
            handler.sendMessage(handler.obtainMessage(VPN_MSG_STATUS_UPDATE, value, 0))
    );

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private final NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this, NotificationChannels.SERVICE_RUNNING)
                    .setSmallIcon(R.drawable.ic_state_deny)
                    .setPriority(Notification.PRIORITY_MIN);

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannels.onCreate(this);

        notificationBuilder.addAction(R.drawable.ic_pause_black_24dp,
                        getString(R.string.notification_action_pause),
                        PendingIntent.getService(this, REQUEST_CODE_PAUSE,
                                new Intent(this, AdVpnService.class).putExtra("COMMAND", Command.PAUSE.ordinal()),
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
    }

    public static void checkStartVpnOnBoot(Context context) {
        Configuration config = FileHelper.loadCurrentSettings(context);
        if (config == null || !config.autoStart) return;
        if (!context.getSharedPreferences("state", MODE_PRIVATE).getBoolean("isActive", false)) return;

        if (VpnService.prepare(context) != null) {
            Log.i("BOOT", "VPN not prepared by user, skipping auto-start");
            return;
        }

        Intent intent = getStartIntent(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @NonNull
    private static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, AdVpnService.class);
        intent.putExtra("COMMAND", Command.START.ordinal());
        intent.putExtra("NOTIFICATION_INTENT",
                PendingIntent.getActivity(context, 0,
                        new Intent(context, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return intent;
    }

    @NonNull
    private static Intent getResumeIntent(Context context) {
        Intent intent = new Intent(context, AdVpnService.class);
        intent.putExtra("COMMAND", Command.RESUME.ordinal());
        intent.putExtra("NOTIFICATION_INTENT",
                PendingIntent.getActivity(context, 0,
                        new Intent(context, MainActivity.class),
                        PendingIntent.FLAG_IMMUTABLE));
        return intent;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Command command = intent == null ? Command.START :
                Command.values()[intent.getIntExtra("COMMAND", Command.START.ordinal())];

        switch (command) {
            case RESUME:
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancelAll();
                // fallthrough
            case START:
                getSharedPreferences("state", MODE_PRIVATE).edit().putBoolean("isActive", true).apply();
                startVpn(intent == null ? null : (PendingIntent) intent.getParcelableExtra("NOTIFICATION_INTENT"));
                break;
            case STOP:
                getSharedPreferences("state", MODE_PRIVATE).edit().putBoolean("isActive", false).apply();
                stopVpn();
                break;
            case PAUSE:
                pauseVpn();
                break;
        }

        return Service.START_STICKY;
    }

    private void pauseVpn() {
        stopVpn();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID_STATE, new NotificationCompat.Builder(this, NotificationChannels.SERVICE_PAUSED)
                .setSmallIcon(R.drawable.ic_state_deny)
                .setPriority(Notification.PRIORITY_LOW)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                .setContentTitle(getString(R.string.notification_paused_title))
                .setContentText(getString(R.string.notification_paused_text))
                .setContentIntent(PendingIntent.getService(this, REQUEST_CODE_START, getResumeIntent(this),
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE))
                .build());
    }

    private void updateVpnStatus(int status) {
        vpnStatus = status;
        int notificationTextId = vpnStatusToTextId(status);
        notificationBuilder.setContentTitle(getString(notificationTextId));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || FileHelper.loadCurrentSettings(getApplicationContext()).showNotification) {
            startForeground(NOTIFICATION_ID_STATE, notificationBuilder.build());
        }

        Intent intent = new Intent(VPN_UPDATE_STATUS_INTENT);
        intent.putExtra(VPN_UPDATE_STATUS_EXTRA, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startVpn(PendingIntent notificationIntent) {
        notificationBuilder.setContentTitle(getString(R.string.notification_title));
        if (notificationIntent != null) notificationBuilder.setContentIntent(notificationIntent);
        updateVpnStatus(VPN_STATUS_STARTING);

        registerNetworkCallback();
        restartVpnThread();
    }

    private void restartVpnThread() {
        if (vpnThread != null) {
            vpnThread.stopThread();
            vpnThread.startThread();
        } else {
            Log.i(TAG, "VPN thread not initialized.");
        }
    }

    private void stopVpnThread() {
        if (vpnThread != null) vpnThread.stopThread();
    }

    private void waitForNetVpn() {
        stopVpnThread();
        updateVpnStatus(VPN_STATUS_WAITING_FOR_NETWORK);
    }

    private void reconnect() {
        updateVpnStatus(VPN_STATUS_RECONNECTING);
        restartVpnThread();
    }

    private void stopVpn() {
        Log.i(TAG, "Stopping VPN Service");
        stopVpnThread();
        vpnThread = null;
        unregisterNetworkCallback();
        updateVpnStatus(VPN_STATUS_STOPPED);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed");
        stopVpn();
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message == null) return true;
        switch (message.what) {
            case VPN_MSG_STATUS_UPDATE:
                updateVpnStatus(message.arg1);
                break;
            case VPN_MSG_NETWORK_CHANGED:
                // legacy support, currently unused
                break;
            default:
                throw new IllegalArgumentException("Invalid message what=" + message.what);
        }
        return true;
    }

    private void registerNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.i(TAG, "Network available, reconnecting VPN");
                reconnect();
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.i(TAG, "Network lost, waiting for network");
                waitForNetVpn();
            }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    public static int vpnStatusToTextId(int status) {
        switch (status) {
            case VPN_STATUS_STARTING: return R.string.notification_starting;
            case VPN_STATUS_RUNNING: return R.string.notification_running;
            case VPN_STATUS_STOPPING: return R.string.notification_stopping;
            case VPN_STATUS_WAITING_FOR_NETWORK: return R.string.notification_waiting_for_net;
            case VPN_STATUS_RECONNECTING: return R.string.notification_reconnecting;
            case VPN_STATUS_RECONNECTING_NETWORK_ERROR: return R.string.notification_reconnecting_error;
            case VPN_STATUS_STOPPED: return R.string.notification_stopped;
            default: throw new IllegalArgumentException("Invalid VPN status " + status);
        }
    }
}
