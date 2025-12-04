package org.joettaapps.adblocker;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.android.billingclient.api.BillingClient;
import com.android.installreferrer.api.InstallReferrerClient;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.joettaapps.adblocker.db.RuleDatabaseUpdateJobService;
import org.joettaapps.adblocker.db.RuleDatabaseUpdateTask;
import org.joettaapps.adblocker.main.FloatingActionButtonFragment;
import org.joettaapps.adblocker.main.MainFragmentPagerAdapter;
import org.joettaapps.adblocker.main.StartFragment;
import org.joettaapps.adblocker.vpn.AdVpnService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import io.sentry.Sentry;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_FILE_OPEN = 1;
    private static final int REQUEST_FILE_STORE = 2;
    private static final int REQUEST_ITEM_EDIT = 3;

    public static Configuration config;
    private ViewPager viewPager;
    private MainFragmentPagerAdapter fragmentPagerAdapter;
    private FloatingActionButton floatingActionButton;
    private ViewPager.SimpleOnPageChangeListener pageChangeListener;
    private BottomSheetDialog mBottomSheetDialog;

    private InterstitialAd mInterstitialAd;
    private AdView mAdView;
    private NativeAd nativeAd;
    private NativeAdView adView;

    private RelativeLayout pager;
    private SharedPreferences preferences, rewardPreferences, adPreferences;
    private SharedPreferences.Editor prefsEditor, rewardPrefsEditor, adPrefsEditor;

    private final BroadcastReceiver vpnServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int strId = intent.getIntExtra(AdVpnService.VPN_UPDATE_STATUS_EXTRA, R.string.notification_stopped);
            updateStatus(strId);
        }
    };

    private ItemChangedListener itemChangedListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationChannels.onCreate(this);

        if (savedInstanceState == null || config == null) {
            config = FileHelper.loadCurrentSettings(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if (config.nightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_main);

        loadAdMobInterstitialAd();

        Sentry.captureMessage("testing SDK setup");

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });

        mAdView = findViewById(R.id.adView);
        mAdView.loadAd(new AdRequest.Builder().build());

        rewardPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        rewardPrefsEditor = rewardPreferences.edit();
        rewardPrefsEditor.putString("reward", "0");
        rewardPrefsEditor.commit();

        adPreferences = getSharedPreferences("ADPREFRES", MODE_PRIVATE);
        adPrefsEditor = adPreferences.edit();

        mBottomSheetDialog = new BottomSheetDialog(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewPager = findViewById(R.id.view_pager);
        fragmentPagerAdapter = new MainFragmentPagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        floatingActionButton = findViewById(R.id.floating_action_button);
        pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(
                        "android:switcher:" + viewPager.getId() + ":" + fragmentPagerAdapter.getItemId(position)
                );
                if (fragment instanceof FloatingActionButtonFragment) {
                    ((FloatingActionButtonFragment) fragment).setupFloatingActionButton(floatingActionButton);
                    floatingActionButton.show();
                } else {
                    floatingActionButton.hide();
                }
                showInterstitialAd();
            }
        };
        viewPager.addOnPageChangeListener(pageChangeListener);

        RuleDatabaseUpdateJobService.scheduleOrCancel(this, config);
    }

    private void updateStatus(int status) {
        if (viewPager == null) return;

        int currentItem = viewPager.getCurrentItem();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + viewPager.getId() + ":" + currentItem
        );

        if (fragment instanceof StartFragment) {
            ((StartFragment) fragment).updateStatus(status); // call instance method
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pageChangeListener.onPageSelected(viewPager.getCurrentItem());
        updateStatus(AdVpnService.vpnStatus);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                vpnServiceBroadcastReceiver,
                new IntentFilter(AdVpnService.VPN_UPDATE_STATUS_INTENT)
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(vpnServiceBroadcastReceiver);
    }

    // === Interstitial Ad Loading ===
    public void loadAdMobInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, getResources().getString(R.string.inter), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                mInterstitialAd = null;
                                loadAdMobInterstitialAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                mInterstitialAd = null;
                                loadAdMobInterstitialAd();
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                    }
                });
    }

    public void showInterstitialAd() {
        final int random = new Random().nextInt(9) + 1;
        if (random < 5 && mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle("Exit")
                .setMessage("Are you sure you want to close this activity?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    public void editItem(int stateChoices, Configuration.Item item, ItemChangedListener listener) {
        Intent editIntent = new Intent(this, ItemActivity.class);

        this.itemChangedListener = listener;
        if (item != null) {
            editIntent.putExtra("ITEM_TITLE", item.title);
            editIntent.putExtra("ITEM_LOCATION", item.location);
            editIntent.putExtra("ITEM_STATE", item.state);
        }
        editIntent.putExtra("STATE_CHOICES", stateChoices);
        startActivityForResult(editIntent, REQUEST_ITEM_EDIT);
    }
}
