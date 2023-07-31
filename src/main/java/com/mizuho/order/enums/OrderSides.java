package com.mizuho.order.enums;

public enum OrderSides {
    BID('B'), OFFER('O');

    private final char orderSide;

    OrderSides(char orderSide) {
        this.orderSide = orderSide;
    }


    public char getOrderSide() {
        return orderSide;
    }
}
