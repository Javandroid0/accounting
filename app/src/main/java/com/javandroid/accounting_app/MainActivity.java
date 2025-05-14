package com.javandroid.accounting_app;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.javandroid.accounting_app.data.model.Customer;
import com.javandroid.accounting_app.ui.viewmodel.CustomerViewModel;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        // Setup drawer toggle icon
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup Navigation Component
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout);
        }

        // Drawer item click handling
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_customer1) {
                Toast.makeText(this, "Customer 1 selected", Toast.LENGTH_SHORT).show();
                // Open order for Customer 1
            } else if (id == R.id.nav_customer2) {
                Toast.makeText(this, "Customer 2 selected", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


        CustomerViewModel customerViewModel = new ViewModelProvider(this).get(CustomerViewModel.class);

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_customer1) {
                customerViewModel.getCustomers().observe(this, customers -> {
                    if (customers != null && customers.size() > 0) {
                        Customer customer = customers.get(0);
                        customerViewModel.selectCustomer(customer);
                        Toast.makeText(this, "Selected: " + customer.name, Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (item.getItemId() == R.id.nav_customer2) {
                customerViewModel.getCustomers().observe(this, customers -> {
                    if (customers != null && customers.size() > 1) {
                        Customer customer = customers.get(1);
                        customerViewModel.selectCustomer(customer);
                        Toast.makeText(this, "Selected: " + customer.name, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }

    // Inflate the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        if (item.getItemId() == R.id.action_open_order_editor) {
            navController.navigate(R.id.menuFragment);
            return true;
        } else if (item.getItemId() == R.id.action_open_product_editor) {
            navController.navigate(R.id.menuFragment1);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


}
