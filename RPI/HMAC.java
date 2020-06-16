



import java.io.UnsupportedEncodingException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class HMAC {
    public static final String LOG_TAG = "HMAC";

    byte[] secretkey;
    byte[] message;
    byte[] hmacSha256 = null;
    public HMAC(String key, String data) {
        try{
            secretkey = key.getBytes("UTF-8");
            message = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e){
            //Log.e(LOG_TAG, e.getMessage());
        }
    }

    public byte[] calcHmacSha256() {
        try{
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec generatedKey = new SecretKeySpec(secretkey, "AES");
            mac.init(generatedKey);
            hmacSha256 = mac.doFinal(message);
        } catch (Exception e) {
            //Log.e(LOG_TAG, e.getMessage());
        }
        return hmacSha256;
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static void main(String[] args){

        String key = "281742777473543518207051811201009247628";
        String data = "1";
        HMAC HMACMachine = new HMAC(key, data);
        byte[] hmacSha256 = HMACMachine.calcHmacSha256();

        String hexHmacSha256 = HMAC.byteArrayToHex(hmacSha256);
        System.out.println(hexHmacSha256);
    }
}
