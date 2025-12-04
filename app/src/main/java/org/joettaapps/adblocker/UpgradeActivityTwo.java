package org.joettaapps.adblocker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class UpgradeActivityTwo extends AppCompatActivity{

    Button btn;
    ImageView close;

    SharedPreferences adprefrences;
    SharedPreferences.Editor adprefseditor ;


    //for inapp purchase
    private boolean readyToPurchase = false;
//    private BillingProcessor bp;
    private final String PURCHASE_ID = "remove_ads";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade_two);


        getSupportActionBar().hide();

        //initilize in app purchase
//
//        bp = BillingProcessor.newBillingProcessor(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi9xmBpamVFlCM5lm3/dhxabKAallShhMTbRSu7sg7F+IHnHEuWgiIaW12XO+enjXz6S/0LkoqkpJBOUTQ0DTkiUxJZSNUy7l1I7Z8iU4l8Xy0QyBkJ7yeKCYO7AwUQmeaQm2Nf39XAxPexhOdAzNGSzVnhOKw33Ez/m1CyvUilS2CuQZo3lBdlXBbkqYzpgNxQovA2zaqtKMuETqXO4+GxBg7xgEZo8ub3v870hO36kNLUaKPmtD9P/VT0WYrid0ZUA2is4Ly8d2RzFCErRHRcdzsrAFb3e7TeQE35wgCNmEqmZFVXjq+vVfXb9kdsjg3XQw/EW8lwMSE6NAcdgO2QIDAQAB", this); // doesn't bind
//        bp.initialize();

        adprefrences = getSharedPreferences("ADPREFRES", MODE_PRIVATE);
        adprefseditor  = adprefrences.edit();

        btn = (Button) findViewById(R.id.btn);
        close = (ImageView) findViewById(R.id.close);

        if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
            btn.setVisibility(View.GONE);
        }else{
            btn.setVisibility(View.VISIBLE);
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (readyToPurchase) {
//                    bp.purchase(UpgradeActivityTwo.this, PURCHASE_ID);

                } else {
                    Toast.makeText(getApplicationContext(), "Unable to initiate purchase", Toast.LENGTH_SHORT).show();
                }
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


//    @Override
//    public void onProductPurchased(@NonNull String productId, @Nullable PurchaseInfo details) {
//
//        Toast.makeText(this, "Thanks for your Purchased!", Toast.LENGTH_SHORT).show();
//
//
//
//        if(adprefrences.getString("ispurchased", "0").toString().equals("0")) {
//            adprefseditor.putString("ispurchased", "1");
//            adprefseditor.commit();
//        }
//
//        if(adprefrences.getString("ispurchased", "0").toString().equals("1")){
//            btn.setVisibility(View.GONE);
//        }else{
//            btn.setVisibility(View.VISIBLE);
//        }
//    }
//
//    @Override
//    public void onPurchaseHistoryRestored() {
//
//    }
//
//    @Override
//    public void onBillingError(int errorCode, Throwable error) {
//
//        Toast.makeText(this, "Unable to process billing", Toast.LENGTH_SHORT).show();
//        if(adprefrences.getString("ispurchased", "0").toString().equals("1")) {
//            adprefseditor.putString("ispurchased", "0");
//            adprefseditor.commit();
//        }
//
//    }
//
//    @Override
//    public void onBillingInitialized() {
//
//        readyToPurchase = true;
//        btn.setEnabled(true);
//
//    }
//    @Override
//    public void onDestroy() {
//        if (bp != null)
//            bp.release();
//
//        super.onDestroy();
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
////        if (!bp.handleActivityResult(requestCode, resultCode, data))
////            super.onActivityResult(requestCode, resultCode, data);
//        super.onActivityResult(requestCode, resultCode, data);
//    }
//


}

