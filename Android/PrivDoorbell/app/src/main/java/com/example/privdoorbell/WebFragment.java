package com.example.privdoorbell;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.Fragment;

public class WebFragment extends Fragment {

    public final static String LOG_TAG = "WebFragment";

    public WebView mWebView;
    private String urlString;

    public WebFragment(String url) {
        if (url == null) {
            Log.e(LOG_TAG, "url not existing!");
        }
        urlString = "http:/" + url + ":8080";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_webview, container, false);

        Log.v(LOG_TAG, "Started.");
        Log.v(LOG_TAG, "target URL: " + urlString);

        mWebView = (WebView) rootView.findViewById(R.id.webview);

        if (null != urlString) {
            mWebView.loadUrl(urlString);

            //WebSettings webSettings = mWebView.getSettings();
            //webSettings.setJavaScriptEnabled(true);
            // Force links and redirects to open in the WebView instead of in a browser
            mWebView.setWebViewClient(new WebViewClient());
        }


        return rootView;
    }
}
