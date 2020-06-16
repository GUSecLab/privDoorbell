package com.example.privdoorbell;

import android.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;


import com.example.privdoorbell.HMAC;

public class AESCipher {
    public static final String LOG_TAG = "AESCipher";

    public static final int AES_KEY_SIZE = 256;
    public static final int GCM_IV_LENGTH = 12;
    public static final int GCM_TAG_LENGTH = 16;


    public static final BigInteger KEY_SEED = new BigInteger("281742777473543518207051811201009247628");
    KeyGenerator keyGenerator;
    Cipher AES;
    byte[] IV;
    SecretKey existingKey;
    public AESCipher(byte[] key, byte[] existing_iv){
        try {
            existingKey = new SecretKeySpec(key, "AES");
            if (existing_iv != null) {
                IV = existing_iv;
            }
            else{
                IV = new byte[GCM_IV_LENGTH];
                SecureRandom random = new SecureRandom();
                random.nextBytes(IV);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public AESCipher(byte[] key) {
        existingKey = new SecretKeySpec(key, "AES");
        IV = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(IV);
    }

    public byte[] getIV() {
        return IV;
    }

    public byte[] encrypt(String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        Log.i(LOG_TAG, "Original text: " + data);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);

        cipher.init(Cipher.ENCRYPT_MODE, existingKey, gcmParameterSpec);
        byte[] cipherText = cipher.doFinal(data.getBytes("UTF-8"));

        return cipherText;
    }

    public String decrypt(byte[] cipherText,  byte[] IV) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);

        cipher.init(Cipher.DECRYPT_MODE, existingKey, gcmParameterSpec);

        byte[] plainText = cipher.doFinal(cipherText);

        return new String(plainText);
    }

    // Untested functions
    public static byte[] getTagFromCipherText(byte[] ciphertext) {
        return Arrays.copyOfRange(ciphertext, ciphertext.length - (GCM_TAG_LENGTH), ciphertext.length);
    }

    public static byte[] getRawCipherText(byte[] ciphertext) {
        return Arrays.copyOfRange(ciphertext, 0, ciphertext.length - (GCM_TAG_LENGTH));
    }




    public static void main(String[] args){

        String key = "281742777473543518207051811201009247628";
        String data = "1";
        HMAC HMACMachine = new HMAC(key, data);
        byte[] hmacSha256 = HMACMachine.calcHmacSha256();

        AESCipher AESMachine = new AESCipher(hmacSha256, null);
        try{
            byte[] ciphertext = AESMachine.encrypt(data);

            Log.i(LOG_TAG, "base64:" + CryptoHelper.bytesToBase64(ciphertext));
        }  catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }
}
