package com.mizuho.order.dto;

public record Order(long id, double price, char side, long size) {
}
