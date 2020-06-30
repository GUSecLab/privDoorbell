package com.example.privdoorbell;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.nio.ShortBuffer;

public class FFmpegAudioRecorder<AudioRecordRunnable> {
    private final static String LOG_TAG = "FFmpegAudioRecorder";
    private String ffmpeg_link;
    long startTime = 0;
    boolean recording = false;

    private FFmpegFrameRecorder recorder;

    private boolean isPreviewOn = false;

    /*Filter information, change boolean to true if adding a filter*/
    private boolean addFilter = true;
    private String filterString = "";
    FFmpegFrameFilter filter;

    private int sampleAudioRateInHz = 44100;

    /* The number of seconds in the continuous record loop (or 0 to disable loop). */
    final int RECORD_LENGTH = 0;
    Frame[] images;
    long[] timestamps;
    ShortBuffer[] samples;
    int imagesIndex, samplesIndex;

    /* audio data getting thread */
    private AudioRecord audioRecord;
    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;
    volatile boolean runAudioThread = true;

    public FFmpegAudioRecorder(String output_link) {

        ffmpeg_link = output_link;
    }

    private void initRecorder() {

        Log.w(LOG_TAG, "init recorder");

        Log.i(LOG_TAG, "ffmpeg_url: " + ffmpeg_link);
        recorder = new FFmpegFrameRecorder(ffmpeg_link, 1);
        recorder.setFormat("flv");
        recorder.setSampleRate(sampleAudioRateInHz);
        // Set in the surface changed method

        // The filterString  is any ffmpeg filter.
        // Here is the link for a list: https://ffmpeg.org/ffmpeg-filters.html
        // filterString = "transpose=2,crop=w=200:h=200:x=0:y=0";
        // Try another transpose value
        // filterString = "transpose=0";
        // filter = new FFmpegFrameFilter(filterString, imageWidth, imageHeight);

        //default format on android
        // filter.setPixelFormat(avutil.AV_PIX_FMT_NV21);


        class AudioRecordRunnable implements Runnable {

            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

                // Audio
                int bufferSize;
                ShortBuffer audioData;
                int bufferReadResult;

                bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                if (RECORD_LENGTH > 0) {
                    samplesIndex = 0;
                    samples = new ShortBuffer[RECORD_LENGTH * sampleAudioRateInHz * 2 / bufferSize + 1];
                    for (int i = 0; i < samples.length; i++) {
                        samples[i] = ShortBuffer.allocate(bufferSize);
                    }
                } else {
                    audioData = ShortBuffer.allocate(bufferSize);
                }

                Log.d(LOG_TAG, "audioRecord.startRecording()");
                audioRecord.startRecording();

                /* ffmpeg_audio encoding loop */
                while (runAudioThread) {
                    if (RECORD_LENGTH > 0) {
                        audioData = samples[samplesIndex++ % samples.length];
                        audioData.position(0).limit(0);
                    }
                    //Log.v(LOG_TAG,"recording? " + recording);
                    bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
                    audioData.limit(bufferReadResult);
                    if (bufferReadResult > 0) {
                        Log.v(LOG_TAG, "bufferReadResult: " + bufferReadResult);
                        // If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
                        // Why?  Good question...
                        if (recording) {
                            if (RECORD_LENGTH <= 0) try {
                                recorder.recordSamples(audioData);
                                //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                            } catch (FFmpegFrameRecorder.Exception e) {
                                Log.v(LOG_TAG, e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }
                Log.v(LOG_TAG, "AudioThread Finished, release audioRecord");

                /* encoding finish, release recorder */
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                    Log.v(LOG_TAG, "audioRecord released");
                }
            }
        }
    }
}
