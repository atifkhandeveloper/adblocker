package org.joettaapps.adblocker;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class AdDialog extends Dialog implements
        View.OnClickListener {

    public Activity c;
    public Dialog d;
    public Button no;
    public TextView yes;

    public AdDialog(Activity a) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ad_dialogue);
        yes = (TextView) findViewById(R.id.later);
        no = (Button) findViewById(R.id.adsss);
        yes.setOnClickListener(this);
        no.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.later:
                dismiss();
                break;
            case R.id.adsss:
                Intent mainIntent = new Intent(c,UpgradeActivity.class);
                c.startActivity(mainIntent);
                break;
            default:
                break;
        }
        dismiss();
    }
}