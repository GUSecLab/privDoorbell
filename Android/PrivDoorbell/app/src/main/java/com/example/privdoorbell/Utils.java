package com.example.privdoorbell;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    public static final String LOG_TAG = "Utils";

    /**
     * Split response into a list.
     * @param res Response in format "Seed---Hostname---authcookie".
     * @return List<String> {Seed, Hostname, authcookie}
     */
    public static List<String> splitResponseToSeedAndHostname(String res) {
        if (res == null) {
            throw new NullPointerException();
        }
        List<String> list = new ArrayList<String>();
        if (! res.contains("---")) {
            Log.e(LOG_TAG, "The res string is incorrect.");
            return null;
        }
        else {
            String[] parts = res.split("---");
            try {
                list.add(parts[0]);
                list.add(parts[1]);
                list.add(parts[2]);
            } catch (IndexOutOfBoundsException e) {
                Log.e(LOG_TAG, "The res string is incorrect.");
                return null;
            }


            return list;
        }
    }

    /**
     * Convert the hostname string to useful registration URL.
     * @param hostname Hostname string. Format is "/x.x.x.x".
     *                 Either get it from serviceInfo.getHost()
     *                 or pass in a string that fits the format.
     *                 The function itself doesn't check the format.
     * @return "http://x.x.x.x:8080/register"
     *
     */
    public static String constructRegisterAddress(String hostname) {
        Log.i(LOG_TAG, "registeraddress: " + "http:/" + hostname + ":8080/register");
        return "http:/" + hostname + ":8080/register";
    }

    public static String constructStreamingAddress(String onion_hostname, String pwd, String device_token) {
        return "http://" + onion_hostname + ":8000/live?port=1935&app=live&stream=mystream" + "&psk=" + pwd + "&wmt=" + device_token;
    }

    public static String constructPlayAudioAddress(String onion_hostname, String pwd, String device_token, String audioFileID) {
        return "http://" + onion_hostname + ":8081/playAudio" + "?psk=" + pwd + "&wmt=" + device_token + "&audio=" + audioFileID;
    }

    public static List<String> getConfFileNames() {
        List<String> ret = new ArrayList<String>();
        ret.add("hostname.conf");
        ret.add("seed.conf");
        return ret;
    }

    public static Map<String, String> readRegistration(Context context) {
        Map<String, String> map = new HashMap<String, String>();
        String host = readStringFromInternalFile(context, "hostname.conf");
        String seed = readStringFromInternalFile(context, "seed.conf");
        if (host != null && seed != null) {
            map.put(seed, host);
            return map;
        }
        else {
            return null;
        }

    }

    static String readStringFromInternalFile(Context context, String filename) {
        File path = context.getFilesDir();
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
            return null;
        }

        return contents;
    }

    static void writeToInternalFile(Context context, String filename, String data) {
        File path = context.getFilesDir();
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

    static String devicetokenToPwd(String seed, String device_token) {
        HMAC HMACMachine_dup = new HMAC(seed, device_token);
        byte[] aes_key = HMACMachine_dup.calcHmacSha256();
        String pwd = CryptoHelper.bytesToBase64(aes_key);
        pwd = pwd.replaceAll("[^A-Za-z0-9]", "");
        return pwd;
    }


    static String torrcConfig(String hostname, String auth_cookie) {
        return "HidServAuth " + hostname + " " + auth_cookie;
    }

    /**
     * A wrapper for org.json.
     */
    public class SettingsJSONMachine {

        private JSONObject jsonObject;

        public SettingsJSONMachine(String settingsJSON) {
            try {
                jsonObject = new JSONObject(settingsJSON);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String toString(){
            return jsonObject.toString();
        }

        public String getField(String field) {
            try {
                String ret = jsonObject.getString(field);
                return ret;
            } catch (Exception e) {
                Log.w(LOG_TAG, e.getMessage());
                return null;
            }
        }

        public int removeValue(String field) {
            try {
                jsonObject.put(field, null);
                return 0;
            } catch (Exception e){
                return -1;
            }
        }

        public int remove(String field) {
            try {
                jsonObject.remove(field);
                return 0;
            } catch (Exception e){
                return -1;
            }
        }


    }
}
