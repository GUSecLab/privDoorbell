package com.example.privdoorbell;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;


public class StreamingActivity extends AppCompatActivity {
    public final static String LOG_TAG = "StreamingActivity";
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;

    String socks_host = "127.0.0.1";
    String socks_port = "9050";



    // private static String streaming_url = "rtmp://priviot.cs-georgetown.net:1935/vod/test.flv";
    private static String streaming_url = "http://s2py4nxpsb2il2gg2y5uvibvbue57wi2hqo4ncbxmka7zyl3ab5tkdid.onion:8000/live?port=1935&app=live&stream=mystream";

    private VLCVideoLayout mVideoLayout = null;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "Streaming Activity created.");



        setContentView(R.layout.activity_media);

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        // The proxy settings in VLC don't seem working
        args.add("--socks=127.0.0.1:9050");
        //args.add("--http-proxy=http://127.0.0.1:8118/");
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
            System.setProperty("socksProxyHost", socks_host);
            System.setProperty("socksProxyPort", socks_port);
            Log.i(LOG_TAG, "Proxy set.");
            final Media media = new Media(mLibVLC, Uri.parse(streaming_url));
            Log.i(LOG_TAG, "Got media link.");
            mMediaPlayer.setMedia(media);
            Log.i(LOG_TAG, "Set media.");
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

        mMediaPlayer.stop();
        mMediaPlayer.detachViews();
    }
}
