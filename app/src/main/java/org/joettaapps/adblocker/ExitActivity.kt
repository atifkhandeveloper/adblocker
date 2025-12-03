package org.joettaapps.adblocker

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class ExitActivity : AppCompatActivity() {

    var nativeAdLoad: NativeUtils? = null
    lateinit var close_application: Button
    lateinit var rateUS: LinearLayout
    lateinit var shareNow: LinearLayout
    lateinit var policy: LinearLayout

    override fun onDestroy() {
        super.onDestroy()
        nativeAdLoad?.destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.six_d_activity_exit)
        val nativeView = findViewById<FrameLayout>(R.id.nativeExit)
        close_application = findViewById(R.id.close_application)
        rateUS = findViewById(R.id.ll_rate_us)
        policy = findViewById(R.id.ll_policy)
        shareNow = findViewById(R.id.ll_share_now)

        nativeAdLoad = NativeUtils()
        nativeAdLoad!!.refreshAd(
            this,
            R.layout.six_d_native_ad_splash,
            nativeView,
            resources.getString(R.string.native_ad)
        )


        close_application.setOnClickListener {
            finishAffinity()
        }

        rateUS.setOnClickListener {
            try {
                val url = "https://play.google.com/store/apps/details?id=$packageName"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        shareNow.setOnClickListener {
            try {
                val i = Intent(Intent.ACTION_SEND)
                i.type = "text/plain"
                i.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.app_name))
                var sAux =
                    "\nLet me recommend you this application\n\n" + resources.getString(
                        R.string.app_name
                    ) + " \n\n"
                sAux =
                    sAux + "https://play.google.com/store/apps/details?id=" + packageName + " \n\n"
                i.putExtra(
                    Intent.EXTRA_TEXT, sAux
                )
                startActivity(Intent.createChooser(i, "Share app"))
            } catch (e: java.lang.Exception) { //e.toString();
            }

        }

//        policy.setOnClickListener {
//            try {
//                val browser = Intent(
//                    Intent.ACTION_VIEW, Uri.parse(
//                        resources.getString(R.string.policy_link)
//                    )
//                )
//                startActivity(browser)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//
//        }


    }
}