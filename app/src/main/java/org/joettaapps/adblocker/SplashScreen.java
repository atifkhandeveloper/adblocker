package org.joettaapps.adblocker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {


    SharedPreferences adprefrences;
    SharedPreferences.Editor adprefseditor ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        getSupportActionBar().hide();

        adprefrences = getSharedPreferences("ADPREFRES", MODE_PRIVATE);
        adprefseditor = adprefrences.edit();


        if (adprefrences == null && adprefrences.getString("ispurchased", "0").toString().equals("1")) {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    /* Create an Intent that will start the Menu-Activity. */
                    Intent mainIntent = new Intent(SplashScreen.this, MainActivity.class);
                    SplashScreen.this.startActivity(mainIntent);
                    SplashScreen.this.finish();
                }
            }, 1000);

        } else {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    /* Create an Intent that will start the Menu-Activity. */
                    Intent mainIntent = new Intent(SplashScreen.this, Splash.class);
                    SplashScreen.this.startActivity(mainIntent);
                    SplashScreen.this.finish();
                }
            }, 1000);
        }
    }
}
