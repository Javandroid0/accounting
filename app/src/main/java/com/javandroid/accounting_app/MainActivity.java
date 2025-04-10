package com.javandroid.accounting_app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.javandroid.accounting_app.ui.fragment.ScannerFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure activity_main.xml exists

        // Load the ScannerFragment by default
        if (savedInstanceState == null) {
            loadFragment(new ScannerFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment); // Ensure this ID exists in activity_main.xml
        transaction.commit();
    }
}
