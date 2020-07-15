package com.example.privdoorbell;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    public static final String LOG_TAG = "Utils";


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
            list.add(parts[0]);
            list.add(parts[1]);

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

    public static String constructStreamingAddress(String onion_hostname) {
        return "http://" + onion_hostname + ":8000/live?port=1935&app=live&stream=mystream";
    }

    public static String constructStreamingAddress(String onion_hostname, String pwd) {
        return "http://" + onion_hostname + ":8000/live?port=1935&app=live&stream=mystream&psk=" + pwd;
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

    protected static String readStringFromInternalFile(Context context, String filename) {
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
