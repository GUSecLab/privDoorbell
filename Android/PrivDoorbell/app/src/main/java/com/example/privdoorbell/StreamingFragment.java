package com.example.privdoorbell;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StreamingFragment extends ListFragment {

    public final String LOG_TAG = "StreamingFragment";
    private Map<String, String> registration;
    List<Map<String, String>> hashmapdata;
    List<Map<String, String>> hashmapdata_dup;

    /**
     * Constructor of this fragment. It is necessary to pass a context
     * as it tries to call a few context-based functions.
     */
    public StreamingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_streaming, container, false);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "Started");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        registration = Utils.readRegistration(getActivity());

        if (registration == null) {

        }
        else {
            // This is for passing to StreamingActivity
            hashmapdata = new ArrayList<Map<String, String>>();
            // This is for passing to ListView
            hashmapdata_dup = new ArrayList<Map<String, String>>();

            for (Iterator<Map.Entry<String, String>> entries = registration.entrySet().iterator(); entries.hasNext();) {
                Map.Entry<String, String> entry = entries.next();
                Map<String, String> tmp_map = new HashMap<String, String>();
                tmp_map.put("Seed", entry.getKey());
                tmp_map.put("Hostname", entry.getValue());

                Map<String, String> tmp_map_dup = new HashMap<String, String>();
                tmp_map_dup.put("Seed", "Seed: " + entry.getKey());
                tmp_map_dup.put("Hostname", "Hostname: " + entry.getValue());

                Log.i(LOG_TAG, "Hashmapdata: " + entry.getKey() + entry.getValue());
                hashmapdata.add(tmp_map);
                hashmapdata_dup.add(tmp_map_dup);
            }


            String fromArray[] = {"Seed", "Hostname"};
            int toArray[] = {R.id.text_seed, R.id.text_host};

            SimpleAdapter adapter = new SimpleAdapter(getActivity(), hashmapdata_dup, R.layout.listview_text, fromArray, toArray);

            setListAdapter(adapter);
        }
        super.onResume();
    }


    @Override
    public void onListItemClick(ListView l, View w, int pos, long id) {
        if (hashmapdata == null) {
            Log.i(LOG_TAG, "No data available; aborting.");
            return;
        }

        Intent intent = new Intent(getActivity(), StreamingActivity.class);
        Bundle b = new Bundle();
        b.putString("Hostname", hashmapdata.get(pos).get("Hostname"));
        b.putString("Seed", hashmapdata.get(pos).get("Seed"));
        intent.putExtras(b);
        startActivity(intent);
    }
}
