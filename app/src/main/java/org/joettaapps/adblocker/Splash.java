package org.joettaapps.adblocker;

import static android.content.ContentValues.TAG;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;


public class Splash extends AppCompatActivity {

    private InterstitialAd mInterstitialAd;


    private ProgressBar progressBar;
    private int progressStatus = 0;
    private TextView textView;
    private Handler handler = new Handler();
    private boolean running=true;
    public static FrameLayout native_ad_panel_two;

    private static final String LOG_TAG = "SplashActivity";

    private static final long COUNTER_TIME = 5;

    private long secondsRemaining;

    NativeUtils nativeUtils;
    FrameLayout frameLayout;

//for threading

    Thread t;
    int threadNameCounter = 0; // i use this variable to make sure that old thread is deleted
    // when i pause, you can see it in DDMS

    String message = "";
    boolean isPost = false;
    Runnable work;




    @Override
    protected void onPause() {
        super.onPause();
        message = "Main Activity is going to pause";
        isPost = false;
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getSupportActionBar().hide();

        createTimer(COUNTER_TIME);

//        loadad();

        MobileAds.initialize(this);
        frameLayout = findViewById(R.id.splash_native);
        nativeUtils = new NativeUtils();
        nativeUtils.refreshAd(this, R.layout.six_d_native_ad_splash, frameLayout, getResources().getString(R.string.native_ad));

//        AdLoader adLoader = new AdLoader.Builder(this, getResources().getString(R.string.native_ad))
//                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
//                    @Override
//                    public void onNativeAdLoaded(NativeAd nativeAd) {
//                        NativeTemplateStyle styles = new
//                                NativeTemplateStyle.Builder().build();
//                        TemplateView template = findViewById(R.id.my_template);
//                        template.setStyles(styles);
//                        template.setNativeAd(nativeAd);
//                    }
//                })
//                .build();

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textView = (TextView) findViewById(R.id.textView);


        //////////////
        work = new Runnable() {

            @Override
            public void run() {

                while (true) {
                    if(!isPost){
                        //TODO
                        while (progressStatus < 10 && running && message.equals("Main Activity is going to resume")) {
                            progressStatus += 1;
                            // Update the progress bar and display the
                            //current value in the text view
                            handler.post(new Runnable() {
                                public void run() {
                                    progressBar.setProgress(progressStatus);
                                    textView.setText("Loading Assets " + progressStatus+"/"+progressBar.getMax());
                                }
                            });
                            try {
                                // Sleep for 200 milliseconds.
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                        if( message.equals("Main Activity is going to resume")) {

                            Intent mainIntent = new Intent(Splash.this, MainActivity.class);
                            Splash.this.startActivity(mainIntent);
                            Splash.this.finish();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    displayInterstitial();
//                                            show interestetial();
                                }
                            });
                        }

                        //////
                        isPost = true;
                        if( message.equals("Main Activity is going to pause")){
                            t.interrupt();
                        }

                    }
                    if(Thread.currentThread().isInterrupted()){
                        break;
                    }
                }
            }
        };
//

        message = "Main Activity is going to resume";
        isPost = false;
        threadNameCounter++;
        t = new Thread(work,"My Name is " + String.valueOf(threadNameCounter));
        t.start();



    }
//
//public void loadad(){
//        AdRequest adRequest = new AdRequest.Builder().build();
//
//        InterstitialAd.load(this,getResources().getString(R.string.inter), adRequest,
//                new InterstitialAdLoadCallback() {
//                    @Override
//                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
//                        // The mInterstitialAd reference will be null until
//                        // an ad is loaded.
//                        mInterstitialAd = interstitialAd;
//                        Log.i(TAG, "onAdLoaded");
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        // Handle the error
//                        Log.i(TAG, loadAdError.getMessage());
//                        mInterstitialAd = null;
//                    }
//                });
//    }
//
//    public void displayInterstitial(){
//
//        if (mInterstitialAd != null) {
//            mInterstitialAd.show(Splash.this);
//        } else {
//            Log.d("TAG", "The interstitial ad wasn't ready yet.");
//            loadad();
//        }
//    }

    private void createTimer(long seconds) {
//        final TextView counterTextView = findViewById(R.id.timer);

        CountDownTimer countDownTimer =
                new CountDownTimer(seconds * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        secondsRemaining = ((millisUntilFinished / 1000) + 1);
//                        counterTextView.setText("App is done loading in: " + secondsRemaining);
                    }

                    @Override
                    public void onFinish() {
                        secondsRemaining = 0;
//                        counterTextView.setText("Done.");

                        Application application = getApplication();



                        // If the application is not an instance of MyApplication, log an error message and
                        // start the MainActivity without showing the app open ad.
                        if (!(application instanceof MyApplication)) {
                            Log.e(LOG_TAG, "Failed to cast application to MyApplication.");
                            startMainActivity();
                            return;
                        }

                        // Show the app open ad.
                        ((MyApplication) application)
                                .showAdIfAvailable(
                                        Splash.this,
                                        new MyApplication.OnShowAdCompleteListener() {
                                            @Override
                                            public void onShowAdComplete() {
                                                startMainActivity();
                                            }
                                        });


                    }
                };
        countDownTimer.start();
    }

    public void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
        finish();
    }
}

