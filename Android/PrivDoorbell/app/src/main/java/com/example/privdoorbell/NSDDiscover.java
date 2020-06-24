package com.example.privdoorbell;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;

public class NSDDiscover {
    private final static String LOG_TAG = "NSDDiscover";
    public static final String SERVICE_TYPE = "_workstation._tcp.";

    String resolvedHostname;


    NsdManager.DiscoveryListener discoveryListener;
    //NsdManager.ResolveListener resolveListener;
    String serviceName = SERVICE_TYPE;
    NsdManager nsdManager;
    WifiManager.MulticastLock multicastLock;


    public NSDDiscover(Context context) {
        /* Start NSD */
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("multicastLock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        initializeDiscoveryListener();

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        discoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(LOG_TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(LOG_TAG, "Service discovery success" + service + service.getServiceName());
                if (service.getServiceName().contains("raspberrypi")) {
                    nsdManager.resolveService(service, initializeResolveListener());
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(LOG_TAG, "service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(LOG_TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(LOG_TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(LOG_TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    /**
     * Generate a new NsdManager.ResolveListener object for the discoverListener.
     * It is required that a separate resolveListener is used for every host.
     * @return Newly generated NsdManager.ResolveListener object
     */
    public NsdManager.ResolveListener initializeResolveListener() {
        NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(LOG_TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(LOG_TAG, "Resolve Succeeded. " + serviceInfo);
                /*
                if (serviceInfo.getServiceName().equals(serviceName)) {
                    Log.d(LOG_TAG, "Same IP.");
                    return;
                }*/
                NsdServiceInfo mService = serviceInfo;
                int port = mService.getPort();
                InetAddress host = mService.getHost();
                Log.i(LOG_TAG, "Resolved host:port = " + host.toString() + ":" + port);
                resolvedHostname = host.toString();
            }
        };
        return resolveListener;
    }

    public void teardown() {
        //nsdManager.unregisterService(registrationListener);
        nsdManager.stopServiceDiscovery(discoveryListener);
        multicastLock.release();
    }

    public String getResolvedHostname() {
        return resolvedHostname;
    }
}
