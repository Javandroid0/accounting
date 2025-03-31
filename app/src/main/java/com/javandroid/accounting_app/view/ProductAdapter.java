package com.javandroid.accounting_app.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.BaseAdapter;

import com.javandroid.accounting_app.R;
import com.javandroid.accounting_app.model.Product;

import java.util.List;

public class ProductAdapter extends BaseAdapter {
    private Context context;
    private List<Product> productList;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
            holder = new ViewHolder();
            holder.productName = convertView.findViewById(R.id.productName);
            holder.productBarcode = convertView.findViewById(R.id.productBarcode);
            holder.productSellPrice = convertView.findViewById(R.id.productSellPrice);
            holder.productBuyPrice = convertView.findViewById(R.id.productBuyPrice);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product product = productList.get(position);
        holder.productName.setText(product.getName());
        holder.productBarcode.setText("Barcode: " + product.getBarcode());
        holder.productSellPrice.setText("Sell: $" + product.getSellPrice());
        holder.productBuyPrice.setText("Buy: $" + product.getBuyPrice());

        return convertView;
    }

    static class ViewHolder {
        TextView productName;
        TextView productBarcode;
        TextView productSellPrice;
        TextView productBuyPrice;
    }
}
