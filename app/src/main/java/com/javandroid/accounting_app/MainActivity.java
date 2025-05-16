package com.javandroid.accounting_app;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.javandroid.accounting_app.data.model.CustomerEntity;
import com.javandroid.accounting_app.data.model.UserEntity;
import com.javandroid.accounting_app.ui.adapter.CustomerDrawerAdapter;
import com.javandroid.accounting_app.ui.adapter.UserDrawerAdapter;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;
import com.javandroid.accounting_app.ui.viewmodel.UserViewModel;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        CustomerDrawerAdapter.OnCustomerClickListener,
        UserDrawerAdapter.OnUserClickListener {

    private DrawerLayout drawerLayout;
    private NavigationView navViewLeft;
    private NavigationView navViewRight;
    private UserViewModel userViewModel;
    private CustomerViewModel customerViewModel;
    private CustomerDrawerAdapter customerAdapter;
    private UserDrawerAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModels
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        customerViewModel = new ViewModelProvider(this).get(CustomerViewModel.class);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navViewLeft = findViewById(R.id.nav_view_left);
        navViewRight = findViewById(R.id.nav_view_right);

        // Setup ActionBarDrawerToggle for left drawer (Customer drawer)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Set navigation item click listeners
        navViewLeft.setNavigationItemSelectedListener(this);
        navViewRight.setNavigationItemSelectedListener(this);

        // Add customer RecyclerView to the left drawer
        setupCustomerDrawer();

        // Add user RecyclerView to the right drawer
        setupUserDrawer();

        // Configure NavController with Toolbar
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(toolbar, navController);
    }

    private void setupCustomerDrawer() {
        // Create header view for the drawer
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        customerAdapter = new CustomerDrawerAdapter(this);
        recyclerView.setAdapter(customerAdapter);

        // Add the RecyclerView to the drawer header
        navViewLeft.addHeaderView(recyclerView);

        // Observe customer list from ViewModel
        customerViewModel.getAllCustomers().observe(this, customers -> {
            if (customers != null) {
                customerAdapter.setCustomers(customers);
            }
        });
    }

    private void setupUserDrawer() {
        // Create header view for the drawer
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserDrawerAdapter(this);
        recyclerView.setAdapter(userAdapter);

        // Add the RecyclerView to the drawer header
        navViewRight.addHeaderView(recyclerView);

        // Observe user list from ViewModel
        userViewModel.getAllUsers().observe(this, users -> {
            if (users != null) {
                userAdapter.setUsers(users);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Handle Customer drawer menu items
        if (id == R.id.nav_all_customers) {
            // Show customer list (already shown in drawer)
        } else if (id == R.id.nav_add_customer) {
            // Navigate to add customer screen
            Navigation.findNavController(this, R.id.nav_host_fragment)
                    .navigate(R.id.action_global_addCustomerFragment);
        } else if (id == R.id.nav_delete_customer) {
            Toast.makeText(this, "Select a customer to delete", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_edit_customer) {
            Toast.makeText(this, "Select a customer to edit", Toast.LENGTH_SHORT).show();
        }
        // Handle User drawer menu items
        else if (id == R.id.nav_all_users) {
            // Show user list (already shown in drawer)
        } else if (id == R.id.nav_add_user) {
            // Navigate to add user screen
            Navigation.findNavController(this, R.id.nav_host_fragment)
                    .navigate(R.id.action_global_addUserFragment);
        } else if (id == R.id.nav_delete_user) {
            Toast.makeText(this, "Select a user to delete", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_edit_user) {
            Toast.makeText(this, "Select a user to edit", Toast.LENGTH_SHORT).show();
        }

        // Close the drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // Handle customer selection from drawer
    @Override
    public void onCustomerClick(CustomerEntity customer) {
        // Set selected customer in ViewModel to be used in orders
        customerViewModel.setSelectedCustomer(customer);
        Toast.makeText(this, "Selected customer: " + customer.getName(), Toast.LENGTH_SHORT).show();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    // Handle user selection from drawer
    @Override
    public void onUserClick(UserEntity user) {
        // Set selected user in ViewModel to be used in orders
        userViewModel.setCurrentUser(user);
        Toast.makeText(this, "Selected user: " + user.getUsername(), Toast.LENGTH_SHORT).show();
        drawerLayout.closeDrawer(GravityCompat.END);
    }

    // Open right drawer when user clicks on user info button in toolbar
    public void openUserDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    // Open left drawer when user clicks on customer info button in toolbar
    public void openCustomerDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle toolbar menu items
        if (id == R.id.action_user_info) {
            openUserDrawer();
            return true;
        } else if (id == R.id.action_customer_info) {
            openCustomerDrawer();
            return true;
        } else if (id == R.id.action_backup_data) {
            runManualBackup();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Run a manual backup of the database
     */
    private void runManualBackup() {
        // Show a toast message
        Toast.makeText(this, "Starting manual backup...", Toast.LENGTH_SHORT).show();

        // Run the backup operation immediately
        com.javandroid.accounting_app.data.backup.BackupScheduler.runBackupNow(this);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
