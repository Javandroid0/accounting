package com.javandroid.accounting_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.view.activity.ProductActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonManageProducts = findViewById(R.id.btn_manage_products);
        buttonManageProducts.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, )
        });
        Button buttonViewProducts = findViewById(R.id.btn_view_products);
        buttonViewProducts.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProductActivity.class);
            startActivity(intent);
        });

    }
}
