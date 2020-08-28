package com.example.privdoorbell;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import info.guardianproject.netcipher.proxy.OrbotHelper;

// WARNING: This fragment is very badly programmed; all the onClick functions are
// not handled in this class but MainActivity (for saving time migrating functions
// from MainActivity to MainFragment).

// This works because the fragment is only inflated in MainActivity. It cannot be
// reused.
public class MainFragment extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }


}
