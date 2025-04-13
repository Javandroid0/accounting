package com.javandroid.accounting_app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.javandroid.accounting_app.R;

public class ScanFragment extends Fragment {

    public ScanFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button scanButton = view.findViewById(R.id.btn_scan_product);
        Button addProductButton = view.findViewById(R.id.btn_add_product);
        Button confirmOrderButton = view.findViewById(R.id.btn_confirm_order);

//        scanButton.setOnClickListener(v ->
//                Navigation.findNavController(v).navigate(R.id.action_scanFragment_to_barcodeScannerFragment));
//
//        addProductButton.setOnClickListener(v ->
//                Navigation.findNavController(v).navigate(R.id.action_scanFragment_to_addProductFragment));
//
//        confirmOrderButton.setOnClickListener(v ->
//                Navigation.findNavController(v).navigate(R.id.action_scanFragment_to_confirmOrderFragment));
    }
}
