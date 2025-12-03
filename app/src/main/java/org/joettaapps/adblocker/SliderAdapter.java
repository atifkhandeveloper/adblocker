package org.joettaapps.adblocker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;

import java.util.List;

public class SliderAdapter extends PagerAdapter {

    private Context context;
    private List<Integer> color;
    private List<String> colorName;
    FrameLayout  native_ad;

    SharedPreferences adprefrences;
    SharedPreferences.Editor adprefseditor ;
    NativeUtils nativeUtils;
    FrameLayout frameLayout;



    public SliderAdapter(Context context, List<Integer> color, List<String> colorName) {
        this.context = context;
        this.color = color;
        this.colorName = colorName;

    }

    @Override
    public int getCount() {
        return color.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_slider, null);



        MobileAds.initialize(context);
        frameLayout = view.findViewById(R.id.splash_native);
        nativeUtils = new NativeUtils();
        nativeUtils.refreshAd((Activity) context, R.layout.native_banner_ad_layout, frameLayout, context.getResources().getString(R.string.native_ad));

//        AdLoader adLoader = new AdLoader.Builder(context, context.getResources().getString(R.string.native_ad))
//                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
//                    @Override
//                    public void onNativeAdLoaded(NativeAd nativeAd) {
//                        NativeTemplateStyle styles = new
//                                NativeTemplateStyle.Builder().build();
//                        TemplateView template = view.findViewById(R.id.my_template);
//                        template.setStyles(styles);
//                        template.setNativeAd(nativeAd);
//                    }
//                })
//                .build();



//        ads.loadNativeAd3(view.findViewById(R.id.native_ad_panel));

        TextView textView = (TextView) view.findViewById(R.id.textView);
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.linearLayout);


//        textView.setText(colorName.get(position));
//        linearLayout.setBackgroundColor(color.get(position));

        ViewPager viewPager = (ViewPager) container;
        viewPager.addView(view, 0);

        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ViewPager viewPager = (ViewPager) container;
        View view = (View) object;
        viewPager.removeView(view);
    }


}