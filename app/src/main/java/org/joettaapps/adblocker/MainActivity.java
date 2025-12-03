/* Copyright (C) 2016-2019 Julian Andres Klode <jak@jak-linux.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.text.Html;
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

    private BottomSheetBehavior behaviorsheet;
    private BottomSheetDialog mBottomSheetDialog;
    private FrameLayout bottom_sheet;


    private final BroadcastReceiver vpnServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int str_id = intent.getIntExtra(AdVpnService.VPN_UPDATE_STATUS_EXTRA, R.string.notification_stopped);
            updateStatus(str_id);
        }
    };

    private ItemChangedListener itemChangedListener = null;
    private MainFragmentPagerAdapter fragmentPagerAdapter;
    private FloatingActionButton floatingActionButton;
    private ViewPager.SimpleOnPageChangeListener pageChangeListener;
    private BottomSheetDialog BottomSheetDialog;


    ViewPager viewPagerr;
    TabLayout indicator;

    List<Integer> color;
    List<String> colorName;

    String rewardnumber;

    private BillingClient billingClient;
    private InterstitialAd mInterstitialAd;
    private AdView mAdView;

    private NativeAd nativeAd;
    private NativeAdView adView;


    RelativeLayout pager;
    InstallReferrerClient referrerClient;
    SharedPreferences preferences , rewardeprefrences ;
    SharedPreferences.Editor prefsEditor , rewardprefsEditors ;

    SharedPreferences adprefrences;
    SharedPreferences.Editor adprefseditor ;

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

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_main);

        loadAdMobInterstitialAd();

        Sentry.captureMessage("testing SDK setup");


        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });


        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(getResources().getString(R.string.banner));

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        rewardeprefrences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        rewardprefsEditors= rewardeprefrences.edit();
        rewardprefsEditors.putString("reward" , "0");
//        rewardnumber = rewardeprefrences.("reward", "0");
        rewardprefsEditors.commit();

        adprefrences = getSharedPreferences("ADPREFRES", MODE_PRIVATE);
        adprefseditor  = adprefrences.edit();

        mBottomSheetDialog = new BottomSheetDialog(this);

        viewPagerr=(ViewPager)findViewById(R.id.viewPager);
        indicator=(TabLayout)findViewById(R.id.indicator);

        pager = (RelativeLayout) findViewById(R.id.pager);

        color = new ArrayList<>();
        color.add(Color.RED);
        color.add(Color.GREEN);
        color.add(Color.BLUE);

        colorName = new ArrayList<>();
        colorName.add("RED");
        colorName.add("GREEN");
        colorName.add("BLUE");

        SliderAdapter sliderAdapter = new SliderAdapter(MainActivity.this, color, colorName);
        viewPagerr.setAdapter (sliderAdapter);
        indicator.setupWithViewPager(viewPagerr, true);

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new SliderTimer(), 4000, 6000);

        SharedPreferences prefs = getSharedPreferences("RATE_US_PREF_BG_CHANGER", MODE_PRIVATE);
        final Boolean hasRated = prefs.getBoolean("rated", false);

        Boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);

        if (isFirstRun) {
            //show start activity

            startActivity(new Intent(MainActivity.this, PrivacyPolicy.class));
//            Toast.makeText(MainActivity.this, "First Run", Toast.LENGTH_LONG)
//                    .show();
        }




        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewPager = (ViewPager) findViewById(R.id.view_pager);

        fragmentPagerAdapter = new MainFragmentPagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        // Add a page change listener that sets the floating action button per tab.
        floatingActionButton = (FloatingActionButton) findViewById(R.id.floating_action_button);
        pageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":" + fragmentPagerAdapter.getItemId(position));
                if (fragment instanceof FloatingActionButtonFragment) {
                    ((FloatingActionButtonFragment) fragment).setupFloatingActionButton(floatingActionButton);
                    floatingActionButton.show();
                    showInterstitialAd();
                } else {
                    floatingActionButton.hide();
                    showInterstitialAd();
                }
            }
        };
        viewPager.addOnPageChangeListener(pageChangeListener);

        RuleDatabaseUpdateJobService.scheduleOrCancel(this, config);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    MainActivity.this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.setting_show_notification).setChecked(config.showNotification);
        menu.findItem(R.id.setting_night_mode).setChecked(config.nightMode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            menu.findItem(R.id.setting_night_mode).setVisible(false);
        }
        // On Android O, require users to configure notifications via notification channels.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menu.findItem(R.id.setting_show_notification).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_refresh:

//                if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
                    refresh();
//                }else{
//                    Intent intent = new Intent(MainActivity.this, UpgradeActivityTwo.class);
//                    startActivity(intent);
//
//                    Toast.makeText(MainActivity.this, "To use this feature get pro version", Toast.LENGTH_SHORT).show();
//                }
                break;
            case R.id.action_load_defaults:
//
//                if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
//                    config = FileHelper.loadDefaultSettings(this);
                showInterstitialAd();
                FileHelper.writeSettings(this, MainActivity.config);
                    recreate();
//                }else{
//                    Intent intent = new Intent(MainActivity.this, UpgradeActivityTwo.class);
//                    startActivity(intent);
//
//                    Toast.makeText(MainActivity.this, "To use this feature get pro version", Toast.LENGTH_SHORT).show();
//                }
                break;
            case R.id.action_import:
//                if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
                showInterstitialAd();
                    Intent intent1 = new Intent()
                            .setType("*/*")
                            .setAction(Intent.ACTION_OPEN_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE);

                    startActivityForResult(intent1, REQUEST_FILE_OPEN);

//                }else{
//
//                    Intent intent = new Intent(MainActivity.this, UpgradeActivityTwo.class);
//                    startActivity(intent);
//                    Toast.makeText(MainActivity.this, "To use this feature get pro version", Toast.LENGTH_SHORT).show();
//                }
                break;
            case R.id.action_export:
//                if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
                showInterstitialAd();
                Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("*/*")
                            .putExtra(Intent.EXTRA_TITLE, "dns66.json");

                    startActivityForResult(exportIntent, REQUEST_FILE_STORE);
//                }else{
//
//                    Intent intent = new Intent(MainActivity.this, UpgradeActivityTwo.class);
//                    startActivity(intent);
//                    Toast.makeText(MainActivity.this, "To use this feature get pro version", Toast.LENGTH_SHORT).show();
//                }
                break;
            case R.id.setting_night_mode:
//                if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
                showInterstitialAd();

                item.setChecked(!item.isChecked());
                    MainActivity.config.nightMode = item.isChecked();
                    FileHelper.writeSettings(MainActivity.this, MainActivity.config);
                    recreate();
//                }else{
//                    Intent intent = new Intent(MainActivity.this, UpgradeActivityTwo.class);
//                    startActivity(intent);
//                    Toast.makeText(MainActivity.this, "To use this feature get pro version", Toast.LENGTH_SHORT).show();
//                }
                break;
            case R.id.setting_show_notification:
                // If we are enabling notifications, we do not need to show a dialog.

//                if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
                    if (!item.isChecked()) {
                        item.setChecked(!item.isChecked());
                        MainActivity.config.showNotification = item.isChecked();
                        FileHelper.writeSettings(MainActivity.this, MainActivity.config);
                        break;
                    }
                    new AlertDialog.Builder(this)
                            .setIcon(R.drawable.ic_warning)
                            .setTitle(R.string.disable_notification_title)
                            .setMessage(R.string.disable_notification_message)
                            .setPositiveButton(R.string.disable_notification_ack, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    item.setChecked(!item.isChecked());
                                    MainActivity.config.showNotification = item.isChecked();
                                    FileHelper.writeSettings(MainActivity.this, MainActivity.config);
                                }
                            })
                            .setNegativeButton(R.string.disable_notification_nak, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            }).show();
//                }else{
//                    Intent intent = new Intent(MainActivity.this, UpgradeActivityTwo.class);
//                    startActivity(intent);
//                    Toast.makeText(MainActivity.this, "To use this feature get pro version", Toast.LENGTH_SHORT).show();
//                }

                break;
            case R.id.action_about:
//                Intent infoIntent = new Intent(this, InfoActivity.class);
//                startActivity(infoIntent);
                break;
            case R.id.action_logcat:

//                if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
                showInterstitialAd();
                sendLogcat();
//                }else{
//                    Intent intent = new Intent(MainActivity.this, UpgradeActivityTwo.class);
//                    startActivity(intent);
//                    Toast.makeText(MainActivity.this, "To use this feature get pro version", Toast.LENGTH_SHORT).show();
//                }
                break;

            case R.id.rateus:
                try{
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+getPackageName())));
                }
                catch (ActivityNotFoundException e){
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+getPackageName())));
                }
                break;

            case R.id.ad:


//                if (adMobInterstitialAd.isLoaded()) {
//                    adMobInterstitialAd.show();}
                AdDialog cdd=new AdDialog(MainActivity.this);
                cdd.show();
                cdd.setCancelable(false);

                break;

            case R.id.share:

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "Here is the share content body";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));

                break;

            case R.id.upgrade:

//                Intent mainIntent = new Intent(MainActivity.this,UpgradeActivityTwo.class);
//                this.startActivity(mainIntent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendLogcat() {
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec("logcat -d");
            InputStream is = proc.getInputStream();
            BufferedReader bis = new BufferedReader(new InputStreamReader(is));
            StringBuilder logcat = new StringBuilder();
            String line;
            while ((line = bis.readLine()) != null) {
                logcat.append(line);
                logcat.append('\n');
            }

            Intent eMailIntent = new Intent(Intent.ACTION_SEND);
            eMailIntent.setType("text/plain");
            eMailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"jak@jak-linux.org"});
            eMailIntent.putExtra(Intent.EXTRA_SUBJECT, "DNS66 Logcat");
            eMailIntent.putExtra(Intent.EXTRA_TEXT, logcat.toString());
            eMailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(eMailIntent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Not supported: " + e, Toast.LENGTH_LONG).show();
        } finally {
            if (proc != null)
                proc.destroy();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("MainActivity", "onNewIntent: Wee");

        if (intent.getBooleanExtra("UPDATE", false)) {
            refresh();
        }

        List<String> errors = RuleDatabaseUpdateTask.lastErrors.getAndSet(null);
        if (errors != null && !errors.isEmpty()) {
            Log.d("MainActivity", "onNewIntent: It's an error");
            errors.add(0, getString(R.string.update_incomplete_description));
            new AlertDialog.Builder(this)
                    .setAdapter(newAdapter(errors), null)
                    .setTitle(R.string.update_incomplete)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
        super.onNewIntent(intent);

    }

    @NonNull
    private ArrayAdapter<String> newAdapter(final List<String> errors) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, errors) {
            @NonNull
            @Override
            @SuppressWarnings("deprecation")
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                //noinspection deprecation
                text1.setText(Html.fromHtml(errors.get(position)));
                return view;
            }
        };
    }


    private void refresh() {
        final RuleDatabaseUpdateTask task = new RuleDatabaseUpdateTask(getApplicationContext(), config, true);


        task.execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity", "onActivityResult: Received result=" + resultCode + " for request=" + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FILE_OPEN && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file

            try {
                config = Configuration.read(new InputStreamReader(getContentResolver().openInputStream(selectedfile)));
            } catch (Exception e) {
                Toast.makeText(this, "Cannot read file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            recreate();
            FileHelper.writeSettings(this, MainActivity.config);
        }
        if (requestCode == REQUEST_FILE_STORE && resultCode == RESULT_OK) {
            Uri selectedfile = data.getData(); //The uri with the location of the file
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(getContentResolver().openOutputStream(selectedfile));
                config.write(writer);
                writer.close();
            } catch (Exception e) {
                Toast.makeText(this, "Cannot write file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    writer.close();
                } catch (Exception ignored) {

                }
            }
            recreate();
        }
        if (requestCode == REQUEST_ITEM_EDIT && resultCode == RESULT_OK) {
            Configuration.Item item = new Configuration.Item();
            Log.d("FOOOO", "onActivityResult: item title = " + data.getStringExtra("ITEM_TITLE"));
            if (data.hasExtra("DELETE")) {
                this.itemChangedListener.onItemChanged(null);
                return;
            }
            item.title = data.getStringExtra("ITEM_TITLE");
            item.location = data.getStringExtra("ITEM_LOCATION");
            item.state = data.getIntExtra("ITEM_STATE", 0);
            this.itemChangedListener.onItemChanged(item);
        }
    }

    private void updateStatus(int status) {
        if (viewPager.getChildAt(0) == null)
            return;

        StartFragment.updateStatus(viewPager.getChildAt(0).getRootView(), status);
    }

    @Override
    protected void onPause() {
        super.onPause();
        loadAdMobInterstitialAd();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(vpnServiceBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<String> errors = RuleDatabaseUpdateTask.lastErrors.getAndSet(null);
        if (errors != null && !errors.isEmpty()) {
            Log.d("MainActivity", "onNewIntent: It's an error");
            errors.add(0, getString(R.string.update_incomplete_description));
            new AlertDialog.Builder(this)
                    .setAdapter(newAdapter(errors), null)
                    .setTitle(R.string.update_incomplete)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
        pageChangeListener.onPageSelected(viewPager.getCurrentItem());
        updateStatus(AdVpnService.vpnStatus);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(vpnServiceBroadcastReceiver, new IntentFilter(AdVpnService.VPN_UPDATE_STATUS_INTENT));
    }

    /**
     * Start the item editor activity
     *
     * @param item     an item to edit, may be null
     * @param listener A listener that will be called once the editor returns
     */
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

    public void loadAdMobInterstitialAd() {

        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,getResources().getString(R.string.inter), adRequest,
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
                mInterstitialAd.show(MainActivity.this);
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.");
                loadAdMobInterstitialAd();
            }
        }
    }

    private class SliderTimer extends TimerTask {

        @Override
        public void run() {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (viewPagerr.getCurrentItem() < color.size() - 1) {
                        viewPagerr.setCurrentItem(viewPagerr.getCurrentItem() + 1);
                    } else {
                        viewPagerr.setCurrentItem(0);
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
//        onBackPressed();
//        showBottomSheetDialog();
//        startActivity(new Intent(MainActivity.this, ExitActivity.class));

        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle("Exit")
                .setMessage("Are you sure you want to close this activity?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showBottomSheetDialog() {
        Log.d("TAG", "showBottomSheetDialog: ");
        if (behaviorsheet.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            behaviorsheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
            Log.d("TAG", "showBottomSheetDialog: collasped");
        }

        View view = getLayoutInflater().inflate(R.layout.six_d_bottom_sheet_layout, null);
        FrameLayout frameLayout = view.findViewById(R.id.ad_frame);
        if (adView != null) {
            if (adView.getParent() != null) {
                ((ViewGroup) adView.getParent()).removeView(adView);
//                frameLayout.addView(adView);
            } else {
                frameLayout.addView(adView);
            }
        } else {
            loadNativeExit();
        }
        (view.findViewById(R.id.txt_exit)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                BottomSheetDialog.dismiss();
                finishAffinity();
            }
        });

        BottomSheetDialog = new BottomSheetDialog(this);
        BottomSheetDialog.setContentView(view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBottomSheetDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        BottomSheetDialog.show();
        BottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                BottomSheetDialog = null;
            }
        });
    }

    private void loadNativeExit() {
        AdLoader.Builder builder = new AdLoader.Builder(this, getString(R.string.native_ad));
        builder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
            @Override
            public void onNativeAdLoaded(NativeAd unifiedNativeAd) {

                if (nativeAd != null) {
                    nativeAd.destroy();
                }
                nativeAd = unifiedNativeAd;

                adView = (NativeAdView) getLayoutInflater().inflate(R.layout.six_d_native_ad_splash, null);
                populateUnifiedNativeAdExitView(unifiedNativeAd, adView);
            }
        });
        builder.build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder().build();
        builder.withNativeAdOptions(adOptions);
        AdLoader adLoader = builder.withAdListener(new AdListener() {
            public void onAdFailedToLoad(int i) {


            }
        }).build();
        adLoader.loadAd(new AdRequest.Builder().build());

    }

    private void populateUnifiedNativeAdExitView(NativeAd nativeAd, NativeAdView adView) {
        // Set the media view.
        adView.setMediaView(adView.findViewById(R.id.ad_media));

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());


        Log.d("getMediaView", "populateNativeAdView:        " + nativeAd.getHeadline());
        // These assets aren't guaranteed to be in every NativeAd, so it's important to
        // check before trying to display them.
//        if (nativeAd.getBody() == null) {
//            adView.getBodyView().setVisibility(View.INVISIBLE);
//        } else {
//            adView.getBodyView().setVisibility(View.VISIBLE);
        if (nativeAd.getBody() != null) {
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }
//        }

//        if (nativeAd.getCallToAction() != null) {
        if (nativeAd.getCallToAction() != null) {
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }
//        }


        if (nativeAd.getIcon() != null) {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
        }

//        if (nativeAd.getPrice() == null) {
//            adView.getPriceView().setVisibility(View.INVISIBLE);
//        } else {
//            adView.getPriceView().setVisibility(View.VISIBLE);
        if (nativeAd.getPrice() != null) {
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }


//        }

//        if (nativeAd.getStore() == null) {
//            adView.getStoreView().setVisibility(View.INVISIBLE);
//        } else {
//            adView.getStoreView().setVisibility(View.VISIBLE);
        if (nativeAd.getStore() != null) {
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }


//        }

//        if (nativeAd.getStarRating() == null) {
//            adView.getStarRatingView().setVisibility(View.INVISIBLE);
//        } else {
        if (nativeAd.getStarRating() != null) {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
        }


//            adView.getStarRatingView().setVisibility(View.VISIBLE);
//        }

//        if (nativeAd.getAdvertiser() == null) {
//            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
//        } else {
        if (nativeAd.getAdvertiser() != null) {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
        }
//            adView.getAdvertiserView().setVisibility(View.VISIBLE);
//        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd);

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        VideoController vc = nativeAd.getMediaContent().getVideoController();

        // Updates the UI to say whether or not this ad has a video asset.
        if (vc.hasVideoContent()) {


            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {

                    super.onVideoEnd();
                }
            });
        } else {

        }

    }
}
