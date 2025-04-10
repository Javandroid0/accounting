package com.javandroid.accounting_app.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.client.android.Intents;
import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.ui.adaptor.OrderAdapter;
import com.javandroid.accounting_app.ui.viewmodel.OrderViewModel;
import com.journeyapps.barcodescanner.CaptureActivity;

public class ScannerFragment extends Fragment {
    private static final int REQUEST_CODE_SCAN = 1;
    private OrderViewModel orderViewModel;
    private OrderAdapter orderAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_scanner, container, false);
        Button scanButton = view.findViewById(R.id.scan_button);
        RecyclerView recyclerView = view.findViewById(R.id.orders_recyclerview);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new OrderAdapter();
        recyclerView.setAdapter(orderAdapter);

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        orderViewModel.getOrders().observe(getViewLifecycleOwner(), orders -> {
            orderAdapter.submitList(orders);
        });

        scanButton.setOnClickListener(v -> startBarcodeScanner());

        return view;
    }

    private void startBarcodeScanner() {
        Intent intent = new Intent(getActivity(), CaptureActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SCAN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK && data != null) {
            String barcode = data.getStringExtra(Intents.Scan.RESULT);
            orderViewModel.scanBarcode(barcode);
        }
    }
}


