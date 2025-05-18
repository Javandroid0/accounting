package com.javandroid.accounting_app.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.ui.fragment.CustomerSelectionDialogFragment;
import com.javandroid.accounting_app.ui.fragment.HomeFragment;
import com.javandroid.accounting_app.ui.fragment.UserSelectionDialogFragment;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navViewLeft; // Customer drawer
    private NavigationView navViewRight; // User drawer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Find the drawer layout
        drawerLayout = findViewById(R.id.drawer_layout);

        // Create drawer toggle
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Set up the navigation views
        navViewLeft = findViewById(R.id.nav_view_left);
        navViewRight = findViewById(R.id.nav_view_right);

        // Setup menu handlers
        setupDrawerContent(navViewLeft);
        setupDrawerContent(navViewRight);

        // Load default fragment if needed
        if (savedInstanceState == null) {
            loadHomeFragment();
        }
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(item -> {
            selectDrawerItem(item);
            return true;
        });
    }

    private void selectDrawerItem(MenuItem item) {
        int id = item.getItemId();

        // Handle menu item clicks
        if (id == R.id.menu_delete_customer) {
            Toast.makeText(this, "Select a customer to delete", Toast.LENGTH_SHORT).show();
            showCustomerSelectionDialog(CustomerSelectionDialogFragment.MODE_DELETE);
        } else if (id == R.id.menu_edit_customer) {
            Toast.makeText(this, "Select a customer to edit", Toast.LENGTH_SHORT).show();
            showCustomerSelectionDialog(CustomerSelectionDialogFragment.MODE_EDIT);
        } else if (id == R.id.menu_delete_user) {
            Toast.makeText(this, "Select a user to delete", Toast.LENGTH_SHORT).show();
            showUserSelectionDialog(UserSelectionDialogFragment.MODE_DELETE);
        } else if (id == R.id.menu_edit_user) {
            Toast.makeText(this, "Select a user to edit", Toast.LENGTH_SHORT).show();
            showUserSelectionDialog(UserSelectionDialogFragment.MODE_EDIT);
        }

        // Close the drawer
        drawerLayout.closeDrawers();
    }

    private void showCustomerSelectionDialog(String mode) {
        CustomerSelectionDialogFragment.newInstance(mode)
                .show(getSupportFragmentManager(), "CustomerDialog");
    }

    private void showUserSelectionDialog(String mode) {
        UserSelectionDialogFragment.newInstance(mode)
                .show(getSupportFragmentManager(), "UserDialog");
    }

    private void loadHomeFragment() {
        // Replace with your actual home fragment
        Fragment homeFragment = new HomeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, homeFragment)
                .commit();
    }

    public void openUserDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(android.view.Gravity.END);
        }
    }

    public void openCustomerDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(android.view.Gravity.START);
        }
    }
}