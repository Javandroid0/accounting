package com.javandroid.accounting_app.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.viewmodel.ViewProductsActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonOpenProductActivity = findViewById(R.id.btn_manage_products);
        buttonOpenProductActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ProductActivity.class));
            }
        });
        Button buttonViewProducts = findViewById(R.id.btn_view_products);

        buttonViewProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ViewProductsActivity.class));
            }
        });
    }
}
