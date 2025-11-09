package com.example.prm392.models;

public class SearchResultItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    private int type;
    private String title;
    private Object data;

    public SearchResultItem(int type, String title, Object data) {
        this.type = type;
        this.title = title;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public Object getData() {
        return data;
    }
}
