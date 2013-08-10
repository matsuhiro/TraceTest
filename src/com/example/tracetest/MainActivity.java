
package com.example.tracetest;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends Activity {

    private String mUrl;
    private String mUserAgent;
    private AppHandler mHandler;

    private void initialize(AppHandler handler) {
        Message msg = new Message();
        msg.what = AppHandler.EVENT_INIT;
        handler.sendMessage(msg);
    }

    private void loadUrl(AppHandler handler) {
        Message msg = new Message();
        msg.what = AppHandler.EVENT_LOAD_URL;
        handler.sendMessage(msg);
    }

    private void loadImage(AppHandler handler, String url, String userAgent) {
        Message msg = new Message();
        msg.what = AppHandler.EVENT_LOAD_IMAGE;
        ImageParams params = new ImageParams(url, userAgent);
        msg.obj = params;
        handler.sendMessage(msg);
    }

    private static String loadUrlFormSdcard() {
        File sdcardDir = Environment.getExternalStorageDirectory();
        File filePath = new File(sdcardDir, "tracetesturl.txt");
        BufferedReader in = null;
        String url = null;
        try {
            FileInputStream file = new FileInputStream(filePath);
            in = new BufferedReader(new InputStreamReader(file));
            url = in.readLine();
            in.close();
            return url;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static private Bitmap getBitmapFromUrl(String in_url, String userAgent) {
        try {
            URL url = new URL(in_url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("User-Agent", userAgent);
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getDefaultUserAgentString(Context context) {
        if (Build.VERSION.SDK_INT >= 17) {
            String userAgent = NewApiWrapper.getDefaultUserAgent(context);
            return userAgent;
        }

        if (Build.VERSION.SDK_INT >= 16) {
            String userAgent = NewApiWrapper.getUserAgent(context);
            return userAgent;
        }

        try {
            Constructor<WebSettings> constructor = WebSettings.class.getDeclaredConstructor(
                    Context.class, WebView.class);
            constructor.setAccessible(true);
            try {
                WebSettings settings = constructor.newInstance(context, null);
                String userAgent = settings.getUserAgentString();
                return userAgent;
            } finally {
                constructor.setAccessible(false);
            }
        } catch (Exception e) {
            String userAgent = new WebView(context).getSettings().getUserAgentString();
            return userAgent;
        }
    }

    static class NewApiWrapper {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        static String getDefaultUserAgent(Context context) {
            return WebSettings.getDefaultUserAgent(context);
        }

        static String getUserAgent(Context context) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends WebSettings> clz = (Class<? extends WebSettings>) Class
                        .forName("android.webkit.WebSettingsClassic");
                Class<?> webViewClassicClz = (Class<?>) Class
                        .forName("android.webkit.WebViewClassic");
                Constructor<? extends WebSettings> constructor = clz.getDeclaredConstructor(
                        Context.class, webViewClassicClz);
                constructor.setAccessible(true);
                try {
                    WebSettings settings = constructor.newInstance(context, null);
                    String userAgent = settings.getUserAgentString();
                    return userAgent;
                } finally {
                    constructor.setAccessible(false);
                }
            } catch (Exception e) {
                String userAgent = new WebView(context).getSettings().getUserAgentString();
                return userAgent;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.os.Debug.startMethodTracing("1.4.0");

        HandlerThread handlerThread = new HandlerThread("App");
        handlerThread.start();

        mHandler = new AppHandler(handlerThread.getLooper(), this,
                new AppCallback() {
                    @Override
                    public void OnCallback(int event, Object arg) {
                        switch (event) {
                            case AppHandler.EVENT_INIT:
                                mUserAgent = (String)arg;
                                loadUrl(mHandler);
                                break;
                            case AppHandler.EVENT_LOAD_URL:
                                mUrl = (String)arg;
                                loadImage(mHandler, mUrl, mUserAgent);
                                break;
                            case AppHandler.EVENT_LOAD_IMAGE:
                                ImageView imageView = (ImageView) findViewById(R.id.image);
                                Bitmap bitmap = (Bitmap)arg;
                                imageView.setImageBitmap(bitmap);
                                break;
                        }
                    }
        });

        initialize(mHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();

        android.os.Debug.stopMethodTracing();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    static class ImageParams {
        public ImageParams(String url, String userAgent) {
            this.url = url;
            this.userAgent = userAgent;
        }
        public String url;
        public String userAgent;
    }

    interface AppCallback {
        void OnCallback(int event, Object arg);
    }

    static class AppHandler extends Handler {
        public static final int EVENT_INIT = 0;
        public static final int EVENT_LOAD_URL = 1;
        public static final int EVENT_LOAD_IMAGE = 2;
        private WeakReference<Activity> mActivity;
        private WeakReference<AppCallback> mAppCallback;

        public AppHandler(Looper looper, Activity activity) {
            super(looper);
            mActivity = new WeakReference<Activity>(activity);
        }

        public AppHandler(Looper looper, Activity activity, AppCallback callback) {
            this(looper, activity);
            mAppCallback = new WeakReference<AppCallback>(callback);
        }

        @Override
        public void handleMessage(Message msg) {
            Activity activity = mActivity.get();
            if (activity == null) {
                return;
            }
            Object data = null;
            int event = msg.what;
            switch (event) {
                case EVENT_INIT:
                    String userAgent = getDefaultUserAgentString(activity);
                    data = (Object)userAgent;
                    break;
                case EVENT_LOAD_URL:
                    String url = loadUrlFormSdcard();
                    data = (Object)url;
                    break;
                case EVENT_LOAD_IMAGE:
                    ImageParams params = (ImageParams)msg.obj;
                    Bitmap bitmap = getBitmapFromUrl(params.url, params.userAgent);
                    data = (Object)bitmap;
                    break;
            }
            final AppCallback callback = mAppCallback.get();
            if (callback == null) {
                return;
            }
            final int outEvent = event;
            final Object outData = data;
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    callback.OnCallback(outEvent, outData);
                }
            });
        }
    }

}
