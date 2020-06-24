package com.example.privdoorbell;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.net.Uri;
import android.os.Build;
import android.media.RingtoneManager;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.example.privdoorbell.AESCipher;
import com.example.privdoorbell.HMAC;
import com.example.privdoorbell.CryptoHelper;

public class FirebaseMessageService extends FirebaseMessagingService{
    private static final String LOG_TAG = "FirebaseMessagingService";
    protected String key = "";
    public final static String HARDCODE_RPI_ADDRESS = "http://192.168.0.4:8080/register";
    public final static String HARDCODE_FIRST_KEY_STR = "1";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage){
        // Receives the message, source: Github - firebase - quickstart-android
        // Diff between Notification & Data msg: https://firebase.google.com/docs/cloud-messaging/concept-options

        byte[] ciphertext_bytes = null;
        byte[] iv_bytes = null;
        byte[] tag_bytes = null;
        String type_plaintext = null;
        String seed = readStringFromInternalFile("seed.conf");

        if (seed == null) {
            return;
        }

        HMAC HMACMachine = new HMAC(seed, HARDCODE_FIRST_KEY_STR);

        Log.d(LOG_TAG, "From: " + remoteMessage.getFrom());

        // Check message type
        if (remoteMessage.getData().size() > 0) {
            Log.d(LOG_TAG, "Message data payload: " + remoteMessage.getData());
            ciphertext_bytes = CryptoHelper.base64ToBytes(remoteMessage.getData().get("type"));
            tag_bytes = CryptoHelper.base64ToBytes(remoteMessage.getData().get("tag"));
            iv_bytes = CryptoHelper.base64ToBytes(remoteMessage.getData().get("iv"));
        }

        byte[] aes_key = HMACMachine.calcHmacSha256();
        Log.i(LOG_TAG, "AES key: " + CryptoHelper.bytesToBase64(aes_key));
        AESCipher AESDecrypter = new AESCipher(aes_key, iv_bytes);
        try {
            type_plaintext = AESDecrypter.decryptWithTag(ciphertext_bytes, tag_bytes, iv_bytes);
            Log.i(LOG_TAG, "Received type: " + type_plaintext);
        } catch (Exception e) {
            Log.e(LOG_TAG, "onMessageReceived(): Decryption failed.");
        }


        //
        if (remoteMessage.getNotification() != null){
            Log.d(LOG_TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
        //

        if (type_plaintext.equals("face")) {
            sendNotification("Someone is at your door!");
        }
        else if (type_plaintext.equals("bell")) {
            sendNotification("Someone is pressing your bell!");
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(LOG_TAG, "Refreshed token: " + token);

        sendRegistrationToServer(token);
    }

    public class PostCall extends AsyncTask<String, String, String> {
        public PostCall() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(LOG_TAG, "POST Started.");
        }

        @Override
        protected String doInBackground(String ... params) {
            String urlString = params[0];
            String data = params[1];
            OutputStream out = null;

            Log.v(LOG_TAG, "Start sending POST request to " + urlString + "...");

            String ret = null;

            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("POST");
                out = new BufferedOutputStream(urlConnection.getOutputStream());

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.write(data);
                writer.flush();
                writer.close();

                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                Log.i(LOG_TAG, "Res code: " + responseCode);

                InputStream inS;
                if (200 <= urlConnection.getResponseCode() && urlConnection.getResponseCode() <= 299) {
                    inS = urlConnection.getInputStream();
                }
                else{
                    inS = urlConnection.getErrorStream();
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(inS));

                StringBuilder responseBody = new StringBuilder();
                String curLine;
                while ((curLine = br.readLine()) != null) {
                    responseBody.append(curLine);
                }

                br.close();
                ret = responseBody.toString();
                Log.i(LOG_TAG, "Res body: " + responseBody);
            }
            catch (Exception e) {
                Log.d(LOG_TAG, e.getMessage());
            }

            return ret;
        }

        protected void onPostExecute(String result) {
            Log.v(LOG_TAG, "Received: " + result);
            writeToInternalFile("seed.conf", result);
        }

        protected void writeToInternalFile(String filename, String data) {
            File path = getFilesDir();
            File file = new File(path, filename);
            Log.i(LOG_TAG, "Starting writing " + data + "to " + filename);

            try{
                // TODO: Handle the actual exceptions
                FileOutputStream stream = new FileOutputStream(file);
                stream.write(data.getBytes());
                stream.close();
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }


    private void sendNotification(String messageBody){
        // Send the notification to the user
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        String channelID = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelID)
                        .setSmallIcon(R.drawable.ic_stat_ic_notification)
                        .setContentTitle(getString(R.string.fcm_message))
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // If SDK > 26 then channel is needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }



    public void sendRegistrationToServer(String token){
        Log.i(LOG_TAG, "Registering to server...");
        //String targetURL = "http://priviot.cs-georgetown.net:8080/register";
        String targetURL = HARDCODE_RPI_ADDRESS;


        new PostCall().execute(targetURL, token);
        // Log.i(LOG_TAG, "Seed: " + seed);
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
