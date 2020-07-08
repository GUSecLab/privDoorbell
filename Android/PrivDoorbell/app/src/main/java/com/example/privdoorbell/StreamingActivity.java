package com.example.privdoorbell;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.helpers.Util;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import info.guardianproject.netcipher.proxy.OrbotHelper;


public class StreamingActivity extends AppCompatActivity {
    public final static String LOG_TAG = "StreamingActivity";
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;

    private MediaController controller;

    private boolean recordbutton_recording = false;

    String socks_host = "127.0.0.1";
    String socks_port = "9050";
    String socks_string = "--socks=" + socks_host + ":" + socks_port;



    private static String streaming_url;
    // private static String streaming_url = "http://s2py4nxpsb2il2gg2y5uvibvbue57wi2hqo4ncbxmka7zyl3ab5tkdid.onion:8000/live?port=1935&app=live&stream=mystream";

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


        // Extract streaming URL (when multiple client function is enabled)
        Bundle b = getIntent().getExtras();
        if (b == null) {
            // Try never call this; this is not reliable.
            // If no argument is passed, try directly reading from configuration file.
            try {
                streaming_url = readStringFromInternalFile("hostname.conf");
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

        streaming_url = Utils.constructStreamingAddress(streaming_url);


        if (!OrbotHelper.isOrbotRunning(this)) {
            Log.w(LOG_TAG, "OrbotHelper: Orbot is not running!");
        }


        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        args.add(socks_string);
        args.add("--drop-late-frames");
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
            media.addOption(":network-caching=300");
            media.addOption(":clock-jitter=0");
            media.addOption(":clock-synchro=0");
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
        finish();
        /*
        // Change the icon on click
        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.fab);
        Log.i(LOG_TAG, "startRecord(): Pushed");
        if (!recordbutton_recording) {
            recordbutton_recording = true;
            button.setImageResource(R.drawable.ic_stop_record);
        }
        else {
            recordbutton_recording = false;
            button.setImageResource(R.drawable.ic_start_record);
        }*/
    }

    protected String readStringFromInternalFile(String filename) {
        File path = getFilesDir();
        File file = new File(path, filename);

        int length = (int) file.length();
        byte[] bytes = new byte[length];
        String contents = null;

        try {
            FileInputStream inS = new FileInputStream(file);
            inS.read(bytes);
            inS.close();

            contents = new String(bytes);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return contents;
    }

}
