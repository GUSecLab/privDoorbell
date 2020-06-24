package com.example.privdoorbell;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.util.Log;

import com.example.privdoorbell.FirebaseMessageService;
import com.example.privdoorbell.FirebaseMessageService.PostCall;

import com.google.firebase.messaging.FirebaseMessagingService;

import org.slf4j.spi.LocationAwareLogger;

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
import java.net.InetAddress;
import java.net.URL;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";
    public static final String SERVICE_TYPE = "_services._dns-sd._udp";

    public final static String HARDCODE_RPI_ADDRESS = "http://192.168.0.4:8080/register";

    /* Initialization for NSD */
    NSDDiscover nsdHelper;
    JmDNSService JmDNSDiscover;

    /* Permissions */
    private final int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Check permissions */
        if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSION_ALL);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        nsdHelper = new NSDDiscover(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nsdHelper != null) {
            nsdHelper.teardown();
        }
        if (JmDNSDiscover != null) {
            JmDNSDiscover.onPostExecute("0");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nsdHelper != null) {
            nsdHelper.teardown();
        }
        if (JmDNSDiscover != null) {
            JmDNSDiscover.onPostExecute("0");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        /* Placeholder; the app is going to crash if not granted permissions */
        assert true;
    }

    public void switchToStreaming(View view) {
        Intent intent = new Intent(this, StreamingActivity.class);
        startActivity(intent);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        /* Helper function, checking permissions listed in PERMISSIONS */
        if (context != null && permissions != null){
            for (String permission: permissions) {
                if (ActivityCompat.checkSelfPermission(context,
                        permission) != PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    public void registerToServer(View view) {
        // OnClick function for the register function
        Log.i(LOG_TAG, "Start checking token...");
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    // Retrieve the current registration token
                    // Source: https://firebase.google.com/docs/cloud-messaging/android/client
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(LOG_TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Log and toast
                        // WARNING: The sendRegistrationToServer() function includes AsyncTask for
                        // POST request. No way to know if the task is over!
                        sendRegistrationToServer(token);
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(LOG_TAG, "Token: " + token);
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

                    }
                });
        Log.i(LOG_TAG, "Register button pushed.");
        // Notice: the lines above, for some reason, are automatically executed.
        //FirebaseInstanceId.getInstance().getInstanceId();
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
            if (result == null) {
                Toast.makeText(MainActivity.this, "Registration failed! Please retry.", Toast.LENGTH_SHORT).show();
            }
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

    public void sendRegistrationToServer(String token){
        if (nsdHelper == null) {
            Log.e(LOG_TAG, "sendRegistrationToServer(): nsdHelper has shut down.");
            toastHelper("mDNS service is not running!");
            return;
        }
        String targetURL = nsdHelper.getResolvedHostname();

        if (targetURL == null) {
            Log.e(LOG_TAG, "sendRegistrationToServer(): failed to get hostname. ");
            toastHelper("Doorbell device not found; please try later.");
            return;
        }

        targetURL = constructRegisterAddress(targetURL);


        Log.i(LOG_TAG, "Registering to server...");
        // String targetURL = "http://priviot.cs-georgetown.net:8080/register";
        // targetURL = HARDCODE_RPI_ADDRESS;


        new PostCall().execute(targetURL, token);
        // Log.i(LOG_TAG, "Seed: " + seed);

        String seed = readStringFromInternalFile("seed.conf");
        Log.i(LOG_TAG, "Seed: " + seed);
    }

    public void testFunc3(View view) {
        String key = "281742777473543518207051811201009247628";
        String data = "Crimson Humble God";
        HMAC HMACMachine = new HMAC(key, data);
        byte[] hmacSha256 = HMACMachine.calcHmacSha256();


        String BASE64_CIPHERTEXT = "C9evRfhy+z8ZrOxNwrARFYLw";
        String BASE64_TAG = "bmyfBjgD1ffDDdNb5aYXxg==";
        String BASE64_IV = "H2f3wpVHjyZW25At";

        byte[] BYTE_CIPHERTEXT = CryptoHelper.base64ToBytes(BASE64_CIPHERTEXT);
        byte[] BYTE_TAG = CryptoHelper.base64ToBytes(BASE64_TAG);
        byte[] BYTE_IV = CryptoHelper.base64ToBytes(BASE64_IV);


        AESCipher AESEncrypter = new AESCipher(hmacSha256, BYTE_IV);
        byte[] nativeCipherText = null;
        try {
            byte[] ciphertext = AESEncrypter.encrypt(data);
            byte[] rawCipherText = AESCipher.getRawCipherText(ciphertext);
            byte[] tag = AESCipher.getTagFromCipherText(ciphertext);

            nativeCipherText = ciphertext;
            Log.i(LOG_TAG, "CombinedCipherText: " + CryptoHelper.bytesToBase64(ciphertext));
            Log.i(LOG_TAG, "SplittedCipherText: " + CryptoHelper.bytesToBase64(CryptoHelper.combineByteArrays(rawCipherText, tag)));
            Log.i(LOG_TAG, "Equals? " + Arrays.equals(ciphertext, CryptoHelper.combineByteArrays(rawCipherText, tag)));

            Log.i(LOG_TAG, "Key: " + CryptoHelper.bytesToBase64(hmacSha256));
            Log.i(LOG_TAG, "Encrypted: " + CryptoHelper.bytesToBase64(rawCipherText) + Arrays.equals(rawCipherText, BYTE_CIPHERTEXT));
            Log.i(LOG_TAG, "BYTE_CIPHERTEXT: " + CryptoHelper.bytesToBase64(BYTE_CIPHERTEXT));
            Log.i(LOG_TAG, "Tag: " + CryptoHelper.bytesToBase64(tag) + Arrays.equals(tag, BYTE_TAG));
            Log.i(LOG_TAG, "IV: " + CryptoHelper.bytesToBase64(AESEncrypter.IV));
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }




        AESCipher AESMachine = new AESCipher(hmacSha256);
        try{
            String plaintext = AESMachine.decrypt(CryptoHelper.combineByteArrays(BYTE_CIPHERTEXT, BYTE_TAG), BYTE_IV);
            //String plaintext = AESMachine.decrypt(nativeCipherText, BYTE_IV);
            Log.i(LOG_TAG, "Decrypted: " + plaintext);
        }  catch (Exception e) {
            Log.e(LOG_TAG, "Error in decryption: " + e.getMessage());
        }
    }

    public void testFunc(View view) {
        JmDNSDiscover = new JmDNSService();
        JmDNSDiscover.doInBackground(this);
    }

    /**
     * Convert the hostname string to useful registration URL.
     * @param hostname Hostname string. Format is "/x.x.x.x".
     *                 Either get it from serviceInfo.getHost()
     *                 or pass in a string that fits the format.
     *                 The function itself doesn't check the format.
     * @return "http://x.x.x.x/register"
     *
     */
    public String constructRegisterAddress(String hostname) {
        Log.i(LOG_TAG, "registeraddress: " + "http://" + hostname + "/register");
        return "http:/" + hostname + "/register";
    }

    /**
     * A helper function for toasting messages.
     * Equals to Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
     */
    public void toastHelper(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

}
