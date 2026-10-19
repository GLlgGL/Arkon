package com.fms.service;

/** A rule the user broke while editing an order; the message is shown on the page. */
public class OrderException extends RuntimeException {
    public OrderException(String message) {
        super(message);
    }
}
