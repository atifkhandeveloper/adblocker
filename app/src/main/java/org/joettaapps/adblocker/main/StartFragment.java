/* Copyright (C) 2016-2019 Julian Andres Klode <jak@jak-linux.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.joettaapps.adblocker.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.joettaapps.adblocker.Configuration;
import org.joettaapps.adblocker.FileHelper;
import org.joettaapps.adblocker.MainActivity;
import org.joettaapps.adblocker.R;
import org.joettaapps.adblocker.UpgradeActivityTwo;
import org.joettaapps.adblocker.vpn.AdVpnService;
import org.joettaapps.adblocker.vpn.Command;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.PurchaseInfo;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class StartFragment extends Fragment /*implements BillingProcessor.IBillingHandler */{
    public static final int REQUEST_START_VPN = 1;
    private static final String TAG = "StartFragment";

    private boolean readyToPurchase = false;
//    private BillingProcessor bp;
    private final String PURCHASE_ID = "remove_ads";
    RelativeLayout adlayout;
    Button ad_free;
    ImageView view;

    SharedPreferences adprefrences;
    SharedPreferences.Editor adprefseditor ;

    private InterstitialAd mInterstitialAd;

    public StartFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        View rootView = inflater.inflate(R.layout.fragment_start, container, false);
        Switch switchOnBoot = (Switch) rootView.findViewById(R.id.switch_onboot);

        ad_free = (Button) rootView.findViewById(R.id.ad_free);
        ImageView view = (ImageView) rootView.findViewById(R.id.state_image);

        loadAdMobInterstitialAd();

//        bp = BillingProcessor.newBillingProcessor(getContext(), "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi9xmBpamVFlCM5lm3/dhxabKAallShhMTbRSu7sg7F+IHnHEuWgiIaW12XO+enjXz6S/0LkoqkpJBOUTQ0DTkiUxJZSNUy7l1I7Z8iU4l8Xy0QyBkJ7yeKCYO7AwUQmeaQm2Nf39XAxPexhOdAzNGSzVnhOKw33Ez/m1CyvUilS2CuQZo3lBdlXBbkqYzpgNxQovA2zaqtKMuETqXO4+GxBg7xgEZo8ub3v870hO36kNLUaKPmtD9P/VT0WYrid0ZUA2is4Ly8d2RzFCErRHRcdzsrAFb3e7TeQE35wgCNmEqmZFVXjq+vVfXb9kdsjg3XQw/EW8lwMSE6NAcdgO2QIDAQAB", this); // doesn't bind
//        bp.initialize();

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return startStopService();
            }
        });

        ad_free.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (readyToPurchase) {
//                    bp.purchase(getActivity(), PURCHASE_ID);
//
//                } else {
//                    Toast.makeText(getContext(), "Unable to initiate purchase", Toast.LENGTH_SHORT).show();
//                }

                Intent intent = new Intent(getActivity(), UpgradeActivityTwo.class);
                startActivity(intent);
            }
        });

        Button startButton = (Button) rootView.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInterstitialAd();
                startStopService();
            }
        });

        updateStatus(rootView, AdVpnService.vpnStatus);

        switchOnBoot.setChecked(MainActivity.config.autoStart);
        switchOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainActivity.config.autoStart = isChecked;
                FileHelper.writeSettings(getContext(), MainActivity.config);
            }
        });

        Switch watchDog = (Switch) rootView.findViewById(R.id.watchdog);
        watchDog.setChecked(MainActivity.config.watchDog);
        watchDog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainActivity.config.watchDog = isChecked;
                FileHelper.writeSettings(getContext(), MainActivity.config);

                if (isChecked) {
                    new AlertDialog.Builder(getActivity())
                            .setIcon(R.drawable.ic_warning)
                            .setTitle(R.string.unstable_feature)
                            .setMessage(R.string.unstable_watchdog_message)
                            .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    watchDog.setChecked(false);
                                    MainActivity.config.watchDog = false;
                                    FileHelper.writeSettings(getContext(), MainActivity.config);
                                }
                            })
                            .setPositiveButton(R.string.button_continue, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    /* Do nothing */
                                }
                            })
                            .show();
                    return;
                } else {
                }
            }
        });

        Switch ipV6Support = (Switch) rootView.findViewById(R.id.ipv6_support);
        ipV6Support.setChecked(MainActivity.config.ipV6Support);
        ipV6Support.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainActivity.config.ipV6Support = isChecked;
                FileHelper.writeSettings(getContext(), MainActivity.config);
            }
        });

        ExtraBar.setup(rootView.findViewById(R.id.extra_bar), "start");

        return rootView;
    }

    private boolean startStopService() {
        ((MainActivity)getActivity()).showInterstitialAd();
        if (AdVpnService.vpnStatus != AdVpnService.VPN_STATUS_STOPPED) {
            Log.i(TAG, "Attempting to disconnect");

            Intent intent = new Intent(getActivity(), AdVpnService.class);
            intent.putExtra("COMMAND", Command.STOP.ordinal());
            getActivity().startService(intent);
        } else {
            checkHostsFilesAndStartService();
        }
        return true;
    }

    public static void updateStatus(View rootView, int status) {
        Context context = rootView.getContext();
        TextView stateText = (TextView) rootView.findViewById(R.id.state_textview);
        ImageView stateImage = (ImageView) rootView.findViewById(R.id.state_image);
        Button startButton = (Button) rootView.findViewById(R.id.start_button);

        if (stateImage == null || stateText == null)
            return;

        stateText.setText(rootView.getContext().getString(AdVpnService.vpnStatusToTextId(status)));
        stateImage.setContentDescription(rootView.getContext().getString(AdVpnService.vpnStatusToTextId(status)));
        stateImage.setImageAlpha(255);
        stateImage.setImageTintList(ContextCompat.getColorStateList(context, R.color.colorStateImage));
        switch(status) {
            case AdVpnService.VPN_STATUS_RECONNECTING:
            case AdVpnService.VPN_STATUS_STARTING:
            case AdVpnService.VPN_STATUS_STOPPING:
                stateImage.setImageDrawable(context.getDrawable(R.drawable.ic_settings_black_24dp));
                startButton.setText(R.string.action_stop);
                break;
            case AdVpnService.VPN_STATUS_STOPPED:
                stateImage.setImageAlpha(32);
                stateImage.setImageTintList(null);
                stateImage.setImageDrawable(context.getDrawable(R.mipmap.app_icon_large));
                startButton.setText(R.string.action_start);
                break;
            case AdVpnService.VPN_STATUS_RUNNING:
                stateImage.setImageDrawable(context.getDrawable(R.drawable.ic_verified_user_black_24dp));
                startButton.setText(R.string.action_stop);
                break;
            case AdVpnService.VPN_STATUS_RECONNECTING_NETWORK_ERROR:
                stateImage.setImageDrawable(context.getDrawable(R.drawable.ic_error_black_24dp));
                startButton.setText(R.string.action_stop);
                break;
        }
    }

    private void checkHostsFilesAndStartService() {
        if (!areHostsFilesExistant()) {
            AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
            builder
                    .setIcon(R.drawable.ic_warning)
                    .setTitle(R.string.missing_hosts_files_title)
                    .setMessage(R.string.missing_hosts_files_message)
                    .setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            /* Do nothing */
                        }
                    })
                    .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startService();
                        }
                    });
            AlertDialog dlg=builder.create();
            dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            dlg.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
                dlg.show();
           /* new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_warning)
                    .setTitle(R.string.missing_hosts_files_title)
                    .setMessage(R.string.missing_hosts_files_message)
                    .setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            *//* Do nothing *//*
                        }
                    })
                    .setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startService();
                        }
                    })
                    .show();*/
            return;
        }
        startService();
    }

    private void startService() {
        ((MainActivity)getActivity()).showInterstitialAd();
        Log.i(TAG, "Attempting to connect");
        Intent intent = VpnService.prepare(getContext());
        if (intent != null) {
            startActivityForResult(intent, REQUEST_START_VPN);
        } else {
            onActivityResult(REQUEST_START_VPN, RESULT_OK, null);
        }
    }

    /**
     * Check if all configured hosts files exist.
     *
     * @return true if all host files exist or no host files were configured.
     */
    private boolean areHostsFilesExistant() {
        if (!MainActivity.config.hosts.enabled)
            return true;

        for (Configuration.Item item : MainActivity.config.hosts.items) {
            if (item.state != Configuration.Item.STATE_IGNORE) {
                try {
                    InputStreamReader reader = FileHelper.openItemFile(getContext(), item);
                    if (reader == null)
                        continue;

                    reader.close();
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: Received result=" + resultCode + " for request=" + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_START_VPN && resultCode == RESULT_CANCELED) {
            Toast.makeText(getContext(), R.string.could_not_configure_vpn_service, Toast.LENGTH_LONG).show();
        }
        if (requestCode == REQUEST_START_VPN && resultCode == RESULT_OK) {
            Log.d("MainActivity", "onActivityResult: Starting service");
            Intent intent = new Intent(getContext(), AdVpnService.class);
            intent.putExtra("COMMAND", Command.START.ordinal());
            intent.putExtra("NOTIFICATION_INTENT",
                    PendingIntent.getActivity(getContext(), 0,
                            new Intent(getContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getContext().startForegroundService(intent);
            } else {
                getContext().startService(intent);
            }

        }
    }

    public void loadAdMobInterstitialAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(getContext(),getResources().getString(R.string.inter), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                        loadAdMobInterstitialAd();

                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdClicked() {
                                // Called when a click is recorded for an ad.
                                Log.d(TAG, "Ad was clicked.");
                                loadAdMobInterstitialAd();
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                Log.d(TAG, "Ad dismissed fullscreen content.");
                                mInterstitialAd = null;
                                loadAdMobInterstitialAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when ad fails to show.
                                Log.e(TAG, "Ad failed to show fullscreen content.");
                                mInterstitialAd = null;
                                loadAdMobInterstitialAd();
                            }

                            @Override
                            public void onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                Log.d(TAG, "Ad recorded an impression.");
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                Log.d(TAG, "Ad showed fullscreen content.");
                                loadAdMobInterstitialAd();
                            }
                        });
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i(TAG, loadAdError.getMessage());
                        mInterstitialAd = null;
                        loadAdMobInterstitialAd();
                    }

                });
    }


    public void showInterstitialAd() {

        final int min = 1;
        final int max = 9;
        final int random = new Random().nextInt((max - min) + 1) + min;

        if(random < 5) {

            if (mInterstitialAd != null) {
                mInterstitialAd.show((Activity) getContext());
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.");
                loadAdMobInterstitialAd();
            }
        }
    }


   /* @Override
    public void onProductPurchased(@NonNull String productId, @Nullable PurchaseInfo details) {


        Toast.makeText(getContext(), "Thanks for your Purchased!", Toast.LENGTH_SHORT).show();

        if(adprefrences.getString("ispurchased", "0").toString().equals("0")) {
            adprefseditor.putString("ispurchased", "1");
            adprefseditor.commit();
        }

        if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
            adlayout.setVisibility(View.GONE);

        }else{
            adlayout.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {

        Toast.makeText(getContext(), "Unable to process billing", Toast.LENGTH_SHORT).show();
        if(adprefrences.getString("ispurchased", "0").toString().equals("1")) {
            adprefseditor.putString("ispurchased", "0");
            adprefseditor.commit();
        }
    }

    @Override
    public void onBillingInitialized() {

    }*/
}
