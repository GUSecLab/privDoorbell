package com.example.privdoorbell;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class ClearRegistrationDialogFragment extends DialogFragment {

    private final String DIALOG_MSG = "Deleting all registration information from this device. This operation cannot be reverted. Continue?";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(DIALOG_MSG)
                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        List<String> filenames = Utils.getConfFileNames();
                        for (String filename: filenames) {
                            try {
                                getActivity().deleteFile(filename);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("ClearRegistrationDialogFragment", "Conf file deleted.");
                        Snackbar.make(getActivity().findViewById(R.id.activity_preference),"DONE.", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
