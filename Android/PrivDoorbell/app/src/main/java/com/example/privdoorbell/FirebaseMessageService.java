package com.example.privdoorbell;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FirebaseMessageService extends FirebaseMessagingService{
    private static final String LOG_TAG = "FirebaseMessagingService";
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
            Log.w(LOG_TAG, "onMessageReceived(): Got unexpected message.");
            return;
        }

        HMAC HMACMachine = new HMAC(seed, HARDCODE_FIRST_KEY_STR);

        Log.d(LOG_TAG, "Seed: " + seed + " From: " + remoteMessage.getFrom());

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
            return;
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

        sendNotification("The messaging token has expired; please register again.");

        writeToInternalFile("token.txt", token);
    }


    private void sendNotification(String messageBody){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean notification_enabled = sharedPreferences.getBoolean("notification", true);

        if (!notification_enabled) {
            Log.v(LOG_TAG, "Notification disabled by user.");
            return;
        }

        // Send the notification to the user

        // Redirect the user to the MainActivity if notification is clicked
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
                    "Privdoorbell Notification",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
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