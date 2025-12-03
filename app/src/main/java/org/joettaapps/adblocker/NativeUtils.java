package org.joettaapps.adblocker;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;


public class NativeUtils {
    private NativeAd mNativeAd;
    public void refreshAd(final Activity activity, @LayoutRes final int resource,
                          FrameLayout frameLayout, String admob_nativeId) {
        AdLoader.Builder builder = new AdLoader.Builder(activity, admob_nativeId);
        builder.forNativeAd(
                new NativeAd.OnNativeAdLoadedListener() {
                    // OnLoadedListener implementation.
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        // If activity callback occurs after the activity is destroyed, you must call
                        // destroy and return or you may get a memory leak.
                        boolean isDestroyed = false;
                        Log.d("nonAdFailedToLoad", "onAdFailedToLoad:nativeAdnativeAd        ");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            isDestroyed = activity.isDestroyed();
                        }
                        if (isDestroyed || activity.isFinishing() || activity.isChangingConfigurations()) {
                            nativeAd.destroy();
                            return;
                        }

                        mNativeAd = nativeAd;

                        NativeAdView adView =
                                (NativeAdView) activity.getLayoutInflater().inflate(resource, null);
                        if(activity.getLocalClassName().contains("Settings")){
                            populateNativeAdViewBanner(mNativeAd,adView);
                        }else {
                            populateNativeAdView(mNativeAd, adView);
                        }
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);
                    }
                });

        AdLoader adLoader =
                builder.withAdListener(
                        new AdListener() {
                            @Override
                            public void onAdFailedToLoad(LoadAdError loadAdError) {
                                Log.d("nonAdFailedToLoad", "onAdFailedToLoad:        " + loadAdError);
                            }
                        })
                        .build();

        adLoader.loadAd(new AdRequest.Builder().build());


    }

    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
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
//        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());


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
//        if (nativeAd.getStarRating() != null) {
//            ((RatingBar) adView.getStarRatingView())
//                    .setRating(nativeAd.getStarRating().floatValue());
//        }


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

    private void populateNativeAdViewBanner(NativeAd nativeAd, NativeAdView adView) {
        // Set the media view.
//        adView.setMediaView(adView.findViewById(R.id.ad_media));

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
//        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
//        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());


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
//        if (nativeAd.getStarRating() != null) {
//            ((RatingBar) adView.getStarRatingView())
//                    .setRating(nativeAd.getStarRating().floatValue());
//        }


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


    public void destroy() {
        if (mNativeAd != null) {
            mNativeAd.destroy();
        }
    }


}
