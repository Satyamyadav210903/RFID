package com.ecartes.rfid_demo.model;

public class Product {
    private int productId;
    private String tagId;
    private String productName;
    private String category;
    private String size;
    private String color;

    public Product(int productId, String tagId, String productName, String category, String size, String color) {
        this.productId = productId;
        this.tagId = tagId;
        this.productName = productName;
        this.category = category;
        this.size = size;
        this.color = color;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", tagId='" + tagId + '\'' +
                ", productName='" + productName + '\'' +
                ", category='" + category + '\'' +
                ", size='" + size + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}