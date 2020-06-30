package com.example.privdoorbell;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
        Log.i(LOG_TAG, "streamaddress: ");
        return "http://" + onion_hostname + ":8000/live?port=1935&app=live&stream=mystream";
    }
}
