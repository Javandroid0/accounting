package com.javandroid.accounting_app;

public class SearchResult {
    private final String data;
    private final int row;
    private final int column;

    public SearchResult(String data, int row, int column) {
        this.data = data;
        this.row = row;
        this.column = column;
    }

    public String getData() {
        return data;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}
