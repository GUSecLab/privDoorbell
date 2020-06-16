package com.example.privdoorbell;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class CryptoHelper {
    public static final String LOG_TAG = "CryptoHelper";

    public static String bytesToBase64(byte[] byteString) {

        byte[] encoded = Base64.getEncoder().encode(byteString);
        //byte[] decoded = Base64.getDecoder().decode(encoded);

        return new String(encoded);
    }

    public static byte[] base64ToBytes(String base64String) {
        byte[] decoded = null;
        try {
            decoded = Base64.getDecoder().decode(base64String.getBytes("UTF-8"));
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return decoded;
    }

    public static byte[] combineByteArrays(byte[] one, byte[] two) {
        byte[] combined = new byte[one.length + two.length];

        for (int i = 0; i < combined.length; ++i) {
            combined[i] = i < one.length ? one[i] : two[i - one.length];
        }

        return combined;
    }
}
