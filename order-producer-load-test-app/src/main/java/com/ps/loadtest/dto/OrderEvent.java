package com.ps.loadtest.dto;

/**
 * Data Transfer Object representing an Order Event.
 * Same structure as used by OrderService-producer and NotificationService-consumer.
 */
public class OrderEvent {

    private String orderId;       // Unique identifier for the order
    private String productName;   // Name of the product being ordered
    private int quantity;         // Number of units ordered
    private double price;         // Price per unit of the product
    private String status;        // Current status of the order (e.g., CREATED)

    public OrderEvent() {
    }

    public OrderEvent(String orderId, String productName, int quantity, double price, String status) {
        this.orderId = orderId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "OrderEvent{orderId='" + orderId + "', productName='" + productName +
                "', quantity=" + quantity + ", price=" + price + ", status='" + status + "'}";
    }
}
