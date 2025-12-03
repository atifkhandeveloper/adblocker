package org.joettaapps.adblocker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicy extends AppCompatActivity {


    Button btn;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.privacy_policy);
        btn = (Button) findViewById(R.id.btn);
        tv = (TextView) findViewById(R.id.tv3);


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("isFirstRun", false).commit();
                Intent myIntent = new Intent(PrivacyPolicy.this, MainActivity.class);
                startActivity(myIntent);
                finish();
            }
        });

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(PrivacyPolicy.this, PrivacyPolicyWebView.class);
                startActivity(myIntent);
            }
        });

    }
}
