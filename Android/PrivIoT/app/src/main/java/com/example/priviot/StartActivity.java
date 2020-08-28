package com.example.priviot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class StartActivity extends Activity {
    private static final String LOG_TAG ="StartActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
    }

    public void startStreaming(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
