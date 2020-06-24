package com.example.privdoorbell;

import android.app.Service;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;



public class JmDNSService{

    public static final String LOG_TAG = "JmDNSActivity";
    // public static final String SERVICE_TYPE = "_http._tcp.local";
    public static final String SERVICE_TYPE = "_services._dns-sd._udp";
    public static final String HOST_NAME = "JmDNS";

    private String SERVICE_INFO_PROPERTY_IP_VERSION = "ipv4";

    private WifiManager.MulticastLock multicastLock;
    private JmDNS jmdns;
    InetAddress deviceIPAddress = null;


    public JmDNSService() {
    }

    private InetAddress getDeviceIPAddress(WifiManager wifi) {
        InetAddress result = null;
        try {
            result = InetAddress.getByName("10.0.0.2");

            WifiInfo wifiinfo = wifi.getConnectionInfo();
            int intaddr = wifiinfo.getIpAddress();
            byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
                    (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
            result = InetAddress.getByAddress(byteaddr);

        } catch (UnknownHostException e) {
            Log.w(LOG_TAG, e.getMessage());
        }
        Log.i(LOG_TAG, "getDeviceIPAddress: " + result);
        return result;
    }

    public String doInBackground(Context... contexts){

        Context context = contexts[0];

        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        deviceIPAddress = getDeviceIPAddress(wifi);
        multicastLock = wifi.createMulticastLock(getClass().getName());
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        Log.i(LOG_TAG, "Acquired multicastLock");
        try {
            jmdns = JmDNS.create(deviceIPAddress, HOST_NAME);
            jmdns.addServiceListener(SERVICE_TYPE, new ServiceListener() {
                public void serviceResolved(ServiceEvent ev) {
                    Log.i(LOG_TAG, "serviceResolved():" + ev.getInfo());
                }

                public void serviceRemoved(ServiceEvent ev) {
                }

                public void serviceAdded(ServiceEvent ev) {
                    Log.i(LOG_TAG, "serviceAdded():" + ev.getInfo());
                    jmdns.requestServiceInfo(ev.getType(), ev.getName(), 1);
                }
            });
            Log.i(LOG_TAG, "Sucessfully set up jmdns");
            ServiceInfo[] serviceInfos = jmdns.list(SERVICE_TYPE);
            Log.i(LOG_TAG, "List is empty? = " + isEmpty(serviceInfos));
            for (ServiceInfo serviceInfo: serviceInfos) {
                Log.i(LOG_TAG, "Entered loop");
                String ipv4_address = serviceInfo.getPropertyString(SERVICE_INFO_PROPERTY_IP_VERSION);
                Log.i(LOG_TAG, "Discovered: " + ipv4_address);
            }

            return "0";
        } catch (IOException e) {
            Log.e(LOG_TAG, "discoverDevices(): " + e.getMessage());
            return "-1";
        }
    }

    public void onPostExecute(String result) {
        // Stop the JmDNS service and release the lock
        try {
            if (jmdns != null) {
                jmdns.unregisterAllServices();
                jmdns.close();
                jmdns = null;
            }
            if (multicastLock != null) {
                multicastLock.release();
                multicastLock = null;
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        Log.i(LOG_TAG, "Sucessfully completed task." + result);

    }


    public boolean isEmpty(ServiceInfo[] serviceInfos) {
        if (serviceInfos == null || serviceInfos.length == 0) {
            return true;
        }
        else {
            return false;
        }
    }
}
