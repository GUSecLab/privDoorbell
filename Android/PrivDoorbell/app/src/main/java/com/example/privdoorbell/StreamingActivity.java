package com.example.privdoorbell;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.helpers.Util;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import info.guardianproject.netcipher.NetCipher;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongConnectionBuilder;


public class StreamingActivity extends AppCompatActivity implements
        StrongBuilder.Callback<HttpURLConnection> {
    public final static String LOG_TAG = "StreamingActivity";
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;
    private static final String HARDCODE_VIDEO_KEY_STR = "2";

    String socks_host = "127.0.0.1";
    String socks_port = "9050";
    String socks_string = "--socks=" + socks_host + ":" + socks_port;

    private String hostname;
    private String seed;
    private String device_token;
    private String pwd;

    private static String streaming_url;

    private VLCVideoLayout mVideoLayout = null;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        Log.i(LOG_TAG, "Streaming Activity created.");


        // Set up toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_media);
        setSupportActionBar(myToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Assign text for buttons
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Button button2 = (Button) findViewById(R.id.button2);
        button2.setText(sharedPreferences.getString("audio2", "Hello!"));
        Button button3 = (Button) findViewById(R.id.button3);
        button3.setText(sharedPreferences.getString("audio3", "What are you doing?"));
        Button button4 = (Button) findViewById(R.id.button4);
        button4.setText(sharedPreferences.getString("audio4", "Bye!"));

        File device_tokenfile = new File(getFilesDir(), "device_token.conf");
        if (!device_tokenfile.exists()) {
            Log.e(LOG_TAG, "Device token file not found; aborting.");
            return;
        }

        // Extract streaming URL (when multiple client function is enabled)
        Bundle b = getIntent().getExtras();
        if (b == null) {
            // Try never call this; this is not reliable.
            // If no argument is passed, try directly reading from configuration file.
            try {
                streaming_url = Utils.readStringFromInternalFile(this, "hostname.conf");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            streaming_url = b.getString("Hostname");
        }

        if (streaming_url == null) {
            Log.e(LOG_TAG, "Streaming URL not available; aborting.");
        }

        // Calculate the Base64 key
        seed = b.getString("Seed");

        device_token = Utils.readStringFromInternalFile(this, "device_token.conf");
        pwd = Utils.devicetokenToPwd(seed, device_token);

        hostname = streaming_url;
        streaming_url = Utils.constructStreamingAddress(streaming_url, pwd, device_token);
        Log.v(LOG_TAG, "Full streaming address: " + streaming_url);


        // Init netcipher
        OrbotHelper.get(this).init();
        if (!OrbotHelper.isOrbotRunning(this)) {
            Log.w(LOG_TAG, "OrbotHelper: Orbot is not running!");
        }


        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        args.add(socks_string);
        args.add("--drop-late-frames");
        args.add("--skip-frames");
        args.add("--network-caching=1000");
        args.add("--clock-jitter=0");
        args.add("--clock-synchro=0");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoLayout = findViewById(R.id.video_layout);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mLibVLC.release();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(LOG_TAG, "Streaming Activity started.");

        mMediaPlayer.attachViews(mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);


        try {
            //System.setProperty("socksProxyHost", socks_host);
            //System.setProperty("socksProxyPort", socks_port);
            final Media media = new Media(mLibVLC, Uri.parse(streaming_url));
            media.setHWDecoderEnabled(true, false);
            //media.addOption(":network-caching=300");
            //media.addOption(":clock-jitter=0");
            //media.addOption(":clock-synchro=0");
            mMediaPlayer.setMedia(media);
            media.release();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            throw new RuntimeException("I/O error");
        }
        mMediaPlayer.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMediaPlayer.detachViews();
        mMediaPlayer.stop();

    }

    /**
     *  Inflates the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     *  Listener for the menu (toolbar).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                //showSettings();
                return true;

            case R.id.action_about:
                //showAbout();
                return true;

            case R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Callback function for clicking the return button.
     * Why is this even required? Ask Google.
     * @return True
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void startRecord(View view) {
        Button button2 = (Button) findViewById(R.id.button2);
        button2.setVisibility(View.VISIBLE);
        Button button3 = (Button) findViewById(R.id.button3);
        button3.setVisibility(View.VISIBLE);
        Button button4 = (Button) findViewById(R.id.button4);
        button4.setVisibility(View.VISIBLE);
        //new GetCall().execute(hostname, device_token, "1");
        //String audioFileID = "1";
        //String target_url = Utils.constructPlayAudioAddress(hostname, Utils.devicetokenToPwd(seed, device_token), device_token, audioFileID);
        /*try {
            Log.v(LOG_TAG, "Started GET.");
            StrongConnectionBuilder
                    .forMaxSecurity(this)
                    .withTorValidation()
                    .connectTo(target_url)
                    .build(this);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }*/
    }

    public void button2Click(View view) {
        new GetCall().execute(hostname, device_token, "1");
        Button button2 = (Button) findViewById(R.id.button2);
        button2.setVisibility(View.INVISIBLE);
        Button button3 = (Button) findViewById(R.id.button3);
        button3.setVisibility(View.INVISIBLE);
        Button button4 = (Button) findViewById(R.id.button4);
        button4.setVisibility(View.INVISIBLE);
    }

    public void button3Click(View view) {
        new GetCall().execute(hostname, device_token, "2");
        Button button2 = (Button) findViewById(R.id.button2);
        button2.setVisibility(View.INVISIBLE);
        Button button3 = (Button) findViewById(R.id.button3);
        button3.setVisibility(View.INVISIBLE);
        Button button4 = (Button) findViewById(R.id.button4);
        button4.setVisibility(View.INVISIBLE);
    }

    public void button4Click(View view) {
        new GetCall().execute(hostname, device_token, "3");
        Button button2 = (Button) findViewById(R.id.button2);
        button2.setVisibility(View.INVISIBLE);
        Button button3 = (Button) findViewById(R.id.button3);
        button3.setVisibility(View.INVISIBLE);
        Button button4 = (Button) findViewById(R.id.button4);
        button4.setVisibility(View.INVISIBLE);
    }

    public class GetCall extends AsyncTask<String, String, String> {
        public GetCall() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(LOG_TAG, "POST Started.");
        }

        @Override
        protected String doInBackground(String ... params) {
            String hostname = params[0];
            String device_token = params[1];
            String audioFileID = params[2];
            OutputStream out = null;


            hostname = Utils.constructPlayAudioAddress(hostname, Utils.devicetokenToPwd(seed, device_token), device_token, audioFileID);

            Log.v(LOG_TAG, "Start sending GET request to " + hostname + "...");

            String ret = null;

            try {
                URL url = new URL(hostname);
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socks_host, 9050));
                NetCipher.setProxy(proxy);
                //HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection(proxy);
                HttpURLConnection urlConnection = NetCipher.getHttpURLConnection(url);
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Connection", "close");

                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                ret = String.valueOf(responseCode);
                Log.i(LOG_TAG, "Res code: " + responseCode);
            }
            catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }

            return ret;
        }

        protected void onPostExecute(String result) {
            Log.v(LOG_TAG, "Received: " + result);
        }

    }

    // Implements StrongBuilder.Callback<HttpURLConnection>

    @Override
    public void onConnected(final HttpURLConnection conn) {
        Log.v(LOG_TAG, "Start sending GET request " + "...");
        new Thread() {
            @Override
            public void run() {
                try {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(in));
                    reader.close();
                    Log.v(LOG_TAG, String.valueOf(conn.getResponseCode()));
                } catch (IOException e) {
                    onConnectionException(e);
                } finally {
                    conn.disconnect();
                }
            }
        }.start();
    }

    @Override
    public void onConnectionException(Exception e) {
        Log.e(LOG_TAG, "onConnectionException(): " +  e.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.e(LOG_TAG, "Time out.");
    }

    @Override
    public void onInvalid() {
        Log.e(LOG_TAG, "Invalid.");
    }

}



