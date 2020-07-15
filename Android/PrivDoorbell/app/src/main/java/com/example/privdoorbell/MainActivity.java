package com.example.privdoorbell;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.List;

import info.guardianproject.netcipher.proxy.OrbotHelper;


public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";
    // public static final String SERVICE_TYPE = "_services._dns-sd._udp";

    // public final static String HARDCODE_RPI_ADDRESS = "http://192.168.0.4:8080/register";


    public final static String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    public final static int MY_PERMISSIONS_REQUEST_LOCATION = 255;

    private FragmentManager fragmentManager;

    /* Initialization for NSD */
    NSDDiscover nsdHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(myToolbar);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_register:
                        switchToFragmentRegister();
                        break;
                    case R.id.action_streaming:
                        switchToFragmentStreaming();
                        break;
                    case R.id.action_wifi:
                        switchToFragmentWifi();
                        break;
                }
                return true;
            }
        });


        fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        MainFragment fragment1 = new MainFragment();
        fragmentTransaction.add(R.id.fragment_main, fragment1);
        fragmentTransaction.commit();

        // The NSDDiscover service runs automatically after initialization!
        nsdHelper = new NSDDiscover(this);


    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (nsdHelper != null) {
            nsdHelper.start();
        }
        else {
            Log.w(LOG_TAG, "onResume(): nsdHelper object not found!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nsdHelper != null) {
            nsdHelper.teardown();
        }
        /*
        if (JmDNSDiscover != null) {
            JmDNSDiscover.onPostExecute("0");
        }
        */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nsdHelper != null) {
            nsdHelper.teardown();
        }
        /*
        if (JmDNSDiscover != null) {
            JmDNSDiscover.onPostExecute("0");
        }
        */
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
                showSettings();
                return true;

            case R.id.action_about:
                showAbout();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showAbout(){
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    public void showSettings(){
        Intent intent = new Intent(this, PreferenceActivity.class);
        startActivity(intent);
    }

    public void switchToFragmentRegister() {
        if (fragmentManager != null) {
            fragmentManager.beginTransaction().replace(R.id.fragment_main, new MainFragment()).commit();
        }
    }

    public void switchToFragmentStreaming() {
        if (fragmentManager != null) {
            fragmentManager.beginTransaction().replace(R.id.fragment_main, new StreamingFragment()).commit();
        }
    }

    public void switchToFragmentWifi() {
        final String urlString = nsdHelper.getResolvedHostname();
        if (urlString == null) {
            toastHelper("mDNS isn't running.");
            return;
        } else {
            if (fragmentManager != null) {
                fragmentManager.beginTransaction().replace(R.id.fragment_main, new WebFragment(urlString)).commit();
            }
        }
    }



    /*
    The logic for these registration functions is:
    registerToServer(): check if LOCATION permission is already granted.
    -> YES: getIdAndSendToServer()
    -> NO: requestPremissions()

    requestPermissions(): ask for permissions, if granted:
    -> YES: getIdAndSendToServer()
    -> NO: disable the register button
    */

    /**
     * The OnClick function for the "register" button.
     * This function only asks for permissions, and the actual work
     * is handled in onRequestPermissionsResult().
     * @param view This activity.
     */
    public void registerToServer(View view) {
        ProgressBar pb = (ProgressBar) findViewById(R.id.registration_progressbar);
        pb.setVisibility(View.VISIBLE);
        // OnClick function for the register function
        // Ask for ACCESS_FINE_LOCATION if not already granted
        if (ContextCompat.checkSelfPermission(this,
                LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            // The function also calls getIdAndSendToServer() if the permissions are granted
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
                urlConnection.setRequestProperty("Connection", "close");
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
                Toast.makeText(MainActivity.this, "Registration failed: illegal HTML response", Toast.LENGTH_SHORT).show();
                ProgressBar pb = (ProgressBar) findViewById(R.id.registration_progressbar);
                pb.setVisibility(View.GONE);
                return;
            }
            List<String> res_list = Utils.splitResponseToSeedAndHostname(result);

            // If the string is not correct
            if (res_list == null) {
                toastHelper("Registration failed: invalid response");
                ProgressBar pb = (ProgressBar) findViewById(R.id.registration_progressbar);
                pb.setVisibility(View.GONE);
                return;
            }
            writeToInternalFile("seed.conf", res_list.get(0));
            writeToInternalFile("hostname.conf", res_list.get(1));
            Log.v(LOG_TAG, "ProgressBar is dead.");
            String seed = res_list.get(0);
            Log.i(LOG_TAG, "Seed: " + seed);
            toastHelper("Registration completed. Seed: " + seed);
            ProgressBar pb = (ProgressBar) findViewById(R.id.registration_progressbar);
            pb.setVisibility(View.GONE);
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


    /**
     * Get the token, send it to the server (which is retrieved from nsdService)
     * with nickname in the inputField.
     *
     * Logic: check if the tokenfile exists (which means the token has been updated)
     * YES ->   Instead of calling Firebase method again (which doesn't seem working),
     *          read token from the file and send. Delete the file after calling.
     * NO ->    Simply call getInstanceId() and send.
     *
     *
     * *** This is not the best way of handling tokens ***
     */
    public void getIdAndSendToServer() {
        getIdAndSendToServer(getNickname());
    }

    public void getIdAndSendToServer(final String nickname) {
        // Do the actual registration work
        Log.i(LOG_TAG, "Register button pushed.");

        File tokenfile = new File(getFilesDir(), "token.txt");
        if (tokenfile.exists()) {
            Log.i(LOG_TAG, "getIdAndSendToServer(): Reading existing token");
            String token = readStringFromInternalFile("token.txt");
            sendRegistrationToServer(token, nickname);
            deleteFile("token.txt");
        }
        else{
            Log.i(LOG_TAG, "getIdAndSendToServer(): Getting new token");

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
                            sendRegistrationToServer(token, nickname);
                        }
                    });
        }
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
            ProgressBar pb = (ProgressBar) findViewById(R.id.registration_progressbar);
            pb.setVisibility(View.GONE);
            return -1;
        }
        String targetURL = nsdHelper.getResolvedHostname();

        if (targetURL == null) {
            Log.e(LOG_TAG, "sendRegistrationToServer(): failed to get hostname. ");
            toastHelper("Doorbell device not found; please try later.");
            ProgressBar pb = (ProgressBar) findViewById(R.id.registration_progressbar);
            pb.setVisibility(View.GONE);
            return -1;
        }

        targetURL = Utils.constructRegisterAddress(targetURL);


        Log.i(LOG_TAG, "Registering to server...");
        // String targetURL = "http://priviot.cs-georgetown.net:8080/register";
        // targetURL = HARDCODE_RPI_ADDRESS;

        new PostCall().execute(targetURL, token, nickname);
        // Log.i(LOG_TAG, "Seed: " + seed);

        // Deprecated. This will possibly read the old seed.
        /*
        String seed = readStringFromInternalFile("seed.conf");
        Log.i(LOG_TAG, "Seed: " + seed);
        toastHelper("Registration completed. Seed: " + seed);
        */
        return 0;
    }



    /**
     * A helper function for toasting messages.
     * Equals to Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
     */
    public void toastHelper(String message) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean switcher = sharedPreferences.getBoolean("toast", true);
        if (switcher == false) {
            Log.i(LOG_TAG, "Toast disabled by user.");
            return;
        }
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Get the value of R.id.nicknameField.
     * @return value of R.id.nicknameField.
     */
    protected String getNickname() {
        TextInputLayout nicknameField = (TextInputLayout) findViewById(R.id.nicknameField);
        Log.i(LOG_TAG, "nickname: " + nicknameField.getEditText().getText().toString());
        return nicknameField.getEditText().getText().toString();
    }

    public void startOrbot(View view) {
        Boolean requested =  OrbotHelper.requestShowOrbotStart(this);
        if (!requested) {
            toastHelper("Failed to start orbot.");
        }

    }
}
