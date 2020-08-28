package com.example.privdoorbell;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_preference, rootKey);

        Preference delete_all = (Preference) findPreference("clear_registration");
        delete_all.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ClearRegistrationDialogFragment dialogFragment = new ClearRegistrationDialogFragment();
                dialogFragment.show(getActivity().getSupportFragmentManager(), "ClearRegistration");
                return true;
            }
        });
    }
}
