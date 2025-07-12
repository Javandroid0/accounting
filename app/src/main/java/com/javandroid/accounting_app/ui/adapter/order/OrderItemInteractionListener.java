package com.javandroid.accounting_app.ui.adapter.order; // Or a more central package like 'com.javandroid.accounting_app.ui.listener'

import com.javandroid.accounting_app.data.model.OrderItemEntity;

public interface OrderItemInteractionListener {
    void onQuantityChanged(OrderItemEntity item, double newQuantity);

    void onPriceChanged(OrderItemEntity item, double newPrice); // Keep if price editing is a feature in either context

    void onDelete(OrderItemEntity item);
}