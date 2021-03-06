package com.artemkopan.baseproject.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.TypedArray;
import android.location.Address;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.artemkopan.baseproject.R;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@SuppressWarnings("WeakerAccess")
public final class ExtraUtils {

    /**
     * Check current thread; If was not main then throw illegalState exception;
     */
    public static void checkUiThread() {
        if (!isMainLooper()) {
            throw new IllegalStateException(
                    "Must be called from the main thread. Was: " + Thread.currentThread());
        }
    }

    /**
     * Check current thread; If it is main thread then throw illegalState exception;
     */
    public static void checkBackgroundThread() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalStateException(
                    "Must be called from the background thread. Was: " + Thread.currentThread());
        }
    }

    /**
     * @return Check current thread. If thread is main then return true;
     */
    public static boolean isMainLooper() {
        return Looper.getMainLooper() == Looper.myLooper();
    }

    /**
     * Hide keyboard if view !=null
     *
     * @param view current focused view
     */
    public static void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext()
                                                              .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null) {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    /**
     * Open keyboard
     *
     * @param view View for request focus; if view == null - nothing happens
     */
    public static void openKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext()
                                                                         .getSystemService(Context.INPUT_METHOD_SERVICE);

        inputMethodManager
                .toggleSoftInputFromWindow(view.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED,
                                           0);
        view.requestFocus();
    }

    /**
     * Check current internet connection
     *
     * @param context {@link Application#getApplicationContext()}
     * @return if internet (WIFI or MOBILE) is connected return true;
     */
    public static boolean checkInternetConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Get full address from {@link Address}
     */
    public static String getCompleteAddressLine(@Nullable Address address) {
        StringBuilder strReturnedAddress = new StringBuilder("");
        if (address != null) {
            int count = address.getMaxAddressLineIndex();
            if (count == 0) {
                strReturnedAddress.append(address.getAddressLine(0));
                return strReturnedAddress.toString();
            }
            for (int i = 0; i < count; i++) {
                strReturnedAddress.append(address.getAddressLine(i));
                if (i < count - 1) strReturnedAddress.append(", ");
            }
        }
        return strReturnedAddress.toString();
    }

    /**
     * Get current width of display
     *
     * @param context {@link Application#getApplicationContext()}
     * @return current width in pixels
     */
    public static int getWindowWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * Get current height of display
     *
     * @param context {@link Application#getApplicationContext()}
     * @return current height in pixels
     */
    public static int getWindowHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int getDialogWidth(Context context) {
        float percent = context.getResources().getFraction(R.fraction.dialog_width, 1, 1);
        return (int) (getWindowWidth(context) * percent);
    }

    /**
     * Get toolbar height size
     */
    public static int getToolbarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme()
                                                   .obtainStyledAttributes(new int[]{R.attr.actionBarSize});
        int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        return toolbarHeight;
    }

    public static int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    /**
     * Keep the CPU On! If you want keep the Screen On you must add flag   getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
     * or  android:keepScreenOn="true" in  layout
     *
     * @param context for get power manager service
     * @return wake lock for {@link ExtraUtils#wakeUnlock(PowerManager.WakeLock)}
     */
    public static PowerManager.WakeLock wakeLock(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getClass().getName());
        wakeLock.acquire();
        return wakeLock;
    }

    /**
     * unlock cpu
     *
     * @param wakeLock acuired wakelock
     */
    public static void wakeUnlock(PowerManager.WakeLock wakeLock) {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    /**
     * Allow all ssl certificates
     *
     * @return {@link SSLSocketFactory}
     */
    public static SSLContext getUnsafeSSL() throws NoSuchAlgorithmException, KeyManagementException {
        return getUnsafeSSL(getTrustAllCerts());
    }

    /**
     * Allow all ssl certificates
     *
     * @return {@link SSLSocketFactory}
     */
    public static SSLContext getUnsafeSSL(TrustManager[] trustAllCerts)
            throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        // Create an ssl socket factory with our all-trusting manager
        return sslContext;
    }

    /**
     * Get all trusted certificates manager;
     */
    @SuppressLint("TrustAllX509TrustManager")
    public static TrustManager[] getTrustAllCerts() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
    }

    /**
     * @return if true then RTL;
     */
    public static boolean isRTL() {
        return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
               == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    public static boolean postLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Calculate some value from start to end;
     *
     * @param percentage Current percent from 0 to 1;
     */
    public static float calculateFactorValue(@FloatRange(from = 0, to = 1) float percentage, float startValue, float endValue) {
        return ((startValue - endValue) * (1 - percentage) + endValue);
    }

}
