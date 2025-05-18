package com.javandroid.accounting_app.data.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProductEntityTest {

    @Test
    public void testProductCreation() {
        ProductEntity product = new ProductEntity("Test Product", "123456");
        product.setProductId(1L);
        product.setSellPrice(10.0);
        product.setBuyPrice(7.0);

        assertEquals(1L, product.getProductId());
        assertEquals("Test Product", product.getName());
        assertEquals("123456", product.getBarcode());
        assertEquals(10.0, product.getSellPrice(), 0.001);
        assertEquals(7.0, product.getBuyPrice(), 0.001);
    }

    @Test
    public void testProductSettersAndGetters() {
        ProductEntity product = new ProductEntity("", "");
        product.setProductId(1L);
        product.setName("Updated Product");
        product.setBarcode("654321");
        product.setSellPrice(15.0);
        product.setBuyPrice(9.0);
        product.setStock(100);

        assertEquals(1L, product.getProductId());
        assertEquals("Updated Product", product.getName());
        assertEquals("654321", product.getBarcode());
        assertEquals(15.0, product.getSellPrice(), 0.001);
        assertEquals(9.0, product.getBuyPrice(), 0.001);
        assertEquals(100.0, product.getStock(), 0.001);
    }
}