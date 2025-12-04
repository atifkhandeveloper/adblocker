package org.joettaapps.adblocker.main;

import static android.app.Activity.RESULT_OK;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import org.joettaapps.adblocker.MainActivity;
import org.joettaapps.adblocker.R;
import org.joettaapps.adblocker.vpn.AdVpnService;
import org.joettaapps.adblocker.vpn.Command;

public class StartFragment extends Fragment {

    private static final int REQUEST_START_VPN = 100;
    private static final String TAG = "StartFragment";

    private InterstitialAd mInterstitialAd;
    private TextView statusTextView; // TextView to display VPN status

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_start, container, false);

        // Initialize status TextView (make sure fragment_start.xml has this view)
//        statusTextView = root.findViewById(R.id.state_textview);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadAdmobInterstitial();

        view.findViewById(R.id.start_button).setOnClickListener(v -> {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(requireActivity());
            } else {
                startVpnFlow();
            }
        });
    }

    // ---------- ADMOB ----------
    private void loadAdmobInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(requireContext(),
                getString(R.string.inter),
                adRequest,
                new InterstitialAdLoadCallback() {

                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        mInterstitialAd = ad;
                        setupFullScreenCallback();
                        Log.d(TAG, "AdMob interstitial loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "AdMob failed: " + loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
    }

    private void setupFullScreenCallback() {
        if (mInterstitialAd == null) return;

        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mInterstitialAd = null; // prevent reuse
                startVpnFlow();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Failed to show ad");
                startVpnFlow();
            }
        });
    }

    // ---------- VPN START FLOW ----------
    private void startVpnFlow() {
        Intent vpnIntent = VpnService.prepare(getContext());

        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, REQUEST_START_VPN);
        } else {
            onActivityResult(REQUEST_START_VPN, RESULT_OK, null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_START_VPN) {
            if (resultCode == RESULT_OK) {
                startVpnForegroundService();
            } else {
                Toast.makeText(getContext(),
                        R.string.could_not_configure_vpn_service,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ---------- FOREGROUND SERVICE START ----------
    private void startVpnForegroundService() {

        Context ctx = getContext();
        if (ctx == null) return;

        Intent intent = new Intent(ctx, AdVpnService.class);
        intent.putExtra("COMMAND", Command.START.ordinal());

        PendingIntent notificationIntent = PendingIntent.getActivity(
                ctx,
                0,
                new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        intent.putExtra("NOTIFICATION_INTENT", notificationIntent);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(ctx, intent);
            } else {
                ctx.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "FGS Start Error: " + e.getMessage());
            Toast.makeText(ctx,
                    "Unable to start VPN service on this device",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------- NEW INSTANCE METHOD ----------
    public void updateStatus(int status) {
        if (statusTextView != null) {
            statusTextView.setText(getString(status)); // status should be string resource ID
        }
    }
}
