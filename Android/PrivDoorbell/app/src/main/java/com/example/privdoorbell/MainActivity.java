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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import com.example.privdoorbell.FirebaseMessageService;
import com.example.privdoorbell.FirebaseMessageService.PostCall;

import com.google.firebase.messaging.FirebaseMessagingService;

import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";
    public static final String SERVICE_TYPE = "_services._dns-sd._udp";

    public final static String HARDCODE_RPI_ADDRESS = "http://192.168.0.4:8080/register";


    public final static String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    public final static int MY_PERMISSIONS_REQUEST_LOCATION = 255;

    /* Initialization for NSD */
    NSDDiscover nsdHelper;
    JmDNSService JmDNSDiscover;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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


    public void switchToStreaming(View view) {
        Intent intent = new Intent(this, StreamingActivity.class);
        startActivity(intent);
    }

    /**
     * The OnClick function for the "register" button.
     * This function only asks for permissions, and the actual work
     * is handled in onRequestPermissionsResult().
     * @param view This activity.
     */
    public void registerToServer(View view) {
        // OnClick function for the register function
        // Ask for ACCESS_FINE_LOCATION if not already granted
        if (ContextCompat.checkSelfPermission(this,
                LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{LOCATION_PERMISSION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }
        else {
            getIdAndSendToServer();
        }
    }

    /**
     * The handler function for permission request.
     * This function calls corresponding functions by given requestCode:
     * getIdAndSendToServer() - 255
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getIdAndSendToServer();
                }
                else {
                    // Toast, and disable the button
                    toastHelper("Registration failed due to lack of permissions.");
                    Button registrationButton = (Button) findViewById(R.id.registrationButton);
                    registrationButton.setEnabled(false);
                }
            }
        }
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
            String token = params[1];
            String nickname = params[2];
            OutputStream out = null;


            JSONObject jsonParam = new JSONObject();
            try {
                jsonParam.put(token, nickname);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.i(LOG_TAG, jsonParam.toString());
            Log.v(LOG_TAG, "Start sending POST request to " + urlString + "...");

            String ret = null;

            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("POST");
                out = new BufferedOutputStream(urlConnection.getOutputStream());

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.write(jsonParam.toString());
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
            List<String> res_list = Utils.splitResponseToSeedAndHostname(result);

            writeToInternalFile("seed.conf", res_list.get(0));
            writeToInternalFile("hostname.conf", res_list.get(1));
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

    public void getIdAndSendToServer() {
        getIdAndSendToServer(getNickname());
    }

    public void getIdAndSendToServer(String nickname) {
        // Do the actual registration work
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
                        sendRegistrationToServer(token, "Nickname");
                        String msg = getString(R.string.msg_token_fmt, token);
                        // Log.d(LOG_TAG, "Token: " + token);
                        // Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

                    }
                });
        Log.i(LOG_TAG, "Register button pushed.");
        // Notice: the lines above, for some reason, are automatically executed.
        //FirebaseInstanceId.getInstance().getInstanceId();
    }


    /**
     * Send given token to the registered local server.
     * The function obtains the address from nsdHelper, and fails
     * if the nsd service is not ready.
     * @param token
     * @return 0 if the registration is completed (i.e. the POST
     * request is sent whether successfully or not.
     * -1 if no address is obtained from nsd service.
     */
    public int sendRegistrationToServer(String token, String nickname){
        Log.i(LOG_TAG, "sendRegistrationToServer(): sending " + token + "; " + nickname);
        if (nsdHelper == null) {
            Log.e(LOG_TAG, "sendRegistrationToServer(): nsdHelper has shut down.");
            toastHelper("mDNS service is not running!");
            return -1;
        }
        String targetURL = nsdHelper.getResolvedHostname();

        if (targetURL == null) {
            Log.e(LOG_TAG, "sendRegistrationToServer(): failed to get hostname. ");
            toastHelper("Doorbell device not found; please try later.");
            return -1;
        }

        targetURL = Utils.constructRegisterAddress(targetURL);


        Log.i(LOG_TAG, "Registering to server...");
        // String targetURL = "http://priviot.cs-georgetown.net:8080/register";
        // targetURL = HARDCODE_RPI_ADDRESS;


        new PostCall().execute(targetURL, token, nickname);
        // Log.i(LOG_TAG, "Seed: " + seed);

        String seed = readStringFromInternalFile("seed.conf");
        Log.i(LOG_TAG, "Seed: " + seed);
        toastHelper("Registration completed. Seed: " + seed);
        return 0;
    }


    public void testFunc(View view) {
        JmDNSDiscover = new JmDNSService();
        JmDNSDiscover.doInBackground(this);
    }



    /**
     * A helper function for toasting messages.
     * Equals to Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
     */
    public void toastHelper(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    protected String getNickname() {
        TextInputLayout nicknameField = (TextInputLayout) findViewById(R.id.nicknameField);
        return nicknameField.getEditText().getText().toString();
    }
}
