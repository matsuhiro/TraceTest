
package com.example.tracetest;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.webkit.WebView;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends Activity {

    private String mUrlString;
    private String mUserAgent;

    private void initialize() {
        mUserAgent = new WebView(this).getSettings().getUserAgentString();
        mUrlString = loadUrlFormSdcard();
    }

    private String loadUrlFormSdcard() {
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
    private Bitmap getBitmapFromUrl(String in_url, String userAgent) {
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

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView mImageView;
        String mUrl;
        String mUserAgent;

        DownloadImageTask(ImageView imageView, String url, String userAgent) {
            mImageView = imageView;
            mUrl = url;
            mUserAgent = userAgent;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = getBitmapFromUrl(mUrl, mUserAgent);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mImageView.setImageBitmap(bitmap);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.os.Debug.startMethodTracing("1.1.0");

        initialize();
        ImageView imageView = (ImageView) findViewById(R.id.image);
        new DownloadImageTask(imageView, mUrlString, mUserAgent).execute("");
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

}
