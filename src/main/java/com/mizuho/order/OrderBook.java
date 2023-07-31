package com.mizuho.order;

import com.mizuho.order.dto.Order;
import com.mizuho.order.enums.OrderSides;

import java.util.Collections;
import java.util.Comparator;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public class OrderBook {

    private final Map<Long, Order> orderBook = new ConcurrentHashMap<>();
    private final Map<Double, Set<Long>> bidsLookup = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private final Map<Double, Set<Long>> offersLookup = new ConcurrentSkipListMap<>();

    /**
     * Adds an order to the order book
     *
     * @param order to be added
     */
    public void addOrder(Order order) {
        validateOrder(order);

        Set<Long> ordersLevel = getOrderLevel(order);
        orderBook.put(order.id(), order);
        ordersLevel.add(order.id());
    }

    private void validateOrder(Order order) {
        Objects.requireNonNull(order, "Order can't be null");

        if (order.id() <= 0) {
            throw new IllegalArgumentException(String.format("Id of and order can't have negative values %s", order.id()));
        }

        if (order.side() != OrderSides.BID.getOrderSide() && order.side() != OrderSides.OFFER.getOrderSide()) {
            throw new IllegalArgumentException(String.format("Unsupported order side %s", order.side()));
        }

        if (order.price() < 0) {
            throw new IllegalArgumentException(String.format("Price of the order %s can't be negative", order.id()));
        }

        if (order.size() <= 0) {
            throw new IllegalArgumentException(String.format("Size of and order can't have negative values %s", order.size()));
        }

        if (orderBook.containsKey(order.id())) {
            throw new IllegalArgumentException(String.format("This order already exists %s", order.id()));
        }
    }

    /**
     * Removes and order from the order book
     *
     * @param id of the order to be removed
     */
    public void removeOrderById(long id) {
        validateOrderId(id);

        Order order = orderBook.remove(id);
        Set<Long> ordersLevel = getOrderLevel(order);
        ordersLevel.remove(order.id());
        //remove level from lookup in case there are no more orders on that level
        if (ordersLevel.isEmpty()) {
            removeLevel(order);
        }
    }

    private void validateOrderId(long id) {
        if (!orderBook.containsKey(id)) {
            throw new IllegalArgumentException(String.format("Order %s doesn't exist", id));
        }
    }

    private void removeLevel(Order order) {
        Map<Double, Set<Long>> orders = order.side() == OrderSides.BID.getOrderSide() ? bidsLookup : offersLookup;
        orders.remove(order.price());
    }

    private Set<Long> getOrderLevel(Order order) {
        Map<Double, Set<Long>> orders = order.side() == OrderSides.BID.getOrderSide() ? bidsLookup : offersLookup;
        return orders.computeIfAbsent(order.price(), k -> new LinkedHashSet<>());
    }

    /**
     * Updates an order's size by the order's id. Doesn't affect time priority
     *
     * @param id   of the order to be updated
     * @param size updated value
     */
    public void updateOrderSizeById(long id, long size) {
        validateOrderIdAndSize(id, size);

        Order order = orderBook.get(id);
        Order updatedOrder = new Order(order.id(), order.price(), order.side(), size);
        orderBook.put(updatedOrder.id(), updatedOrder);
    }

    private void validateOrderIdAndSize(long id, long size) {
        if (!orderBook.containsKey(id)) {
            throw new IllegalArgumentException(String.format("Order %s doesn't exist", id));
        }

        if (size <= 0) {
            throw new IllegalArgumentException(String.format("Size of and order can't have negative values %s", size));
        }
    }

    /**
     * Returns the price of a particular level type
     *
     * @param level to be searched for
     * @param side  of the level
     * @return level's price
     */
    public double getPriceOfLevelBySide(int level, char side) {
        validateLevelAndSide(level, side);

        Map<Double, Set<Long>> levels = side == OrderSides.BID.getOrderSide() ? bidsLookup : offersLookup;
        return levels.keySet().stream()
                .skip(level - 1)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Level %s is out of range", level)));
    }

    private void validateLevelAndSide(int level, char side) {
        if (level <= 0) {
            throw new IllegalArgumentException(String.format("Level can't be negative %s", level));
        }
        if (OrderSides.OFFER.getOrderSide() != side && OrderSides.BID.getOrderSide() != side) {
            throw new IllegalArgumentException(String.format("Unsupported order side %s", side));
        }
    }

    /**
     * Returns the total size available for a level of a particular side
     *
     * @param level to be searched for
     * @param side  pf the level
     * @return total size of a level
     */
    public long getTotalSizeByLevelAndSide(int level, char side) {
        validateLevelAndSide(level, side);

        Map<Double, Set<Long>> levels = side == OrderSides.BID.getOrderSide() ? bidsLookup : offersLookup;
        return levels.values().stream()
                .skip(level - 1)
                .findFirst()
                .stream()
                .flatMap(Set::stream)
                .mapToLong(id -> orderBook.get(id).size())
                .sum();
    }

    public List<Order> getAllOrdersBySide(char side) {
        validateSide(side);

        Map<Double, Set<Long>> orders = OrderSides.BID.getOrderSide() == side ? bidsLookup : offersLookup;
        return orders.values()
                .stream()
                .flatMap(Collection::stream)
                .map(orderBook::get)
                .collect(Collectors.toList());
    }

    private void validateSide(char side) {
        if (OrderSides.OFFER.getOrderSide() != side && OrderSides.BID.getOrderSide() != side) {
            throw new IllegalArgumentException(String.format("Unsupported order side %s", side));
        }
    }

    /**
     * A live read-only view of the order book. It's safe to publish it since the collection contains immutable
     * objects, and it's wrapped in an unmodifiable map
     *
     * @return live view of an order book
     */
    public Map<Long, Order> getOrderBook() {
        return Collections.unmodifiableMap(orderBook);
    }

    /**
     * A live read-only view of the bid's lookup. It's safe to publish it since the collection contains immutable
     * objects, and it's wrapped in an unmodifiable map
     *
     * @return live view of a bid's lookup
     */
    public Map<Double, Set<Long>> getBidsLookup() {
        return Collections.unmodifiableMap(bidsLookup);
    }

    /**
     * A live read-only view of the offer's lookup. It's safe to publish it since the collection contains immutable
     * objects, and it's wrapped in an unmodifiable map
     *
     * @return live view of an order book
     */
    public Map<Double, Set<Long>> getOffersLookup() {
        return Collections.unmodifiableMap(offersLookup);
    }
}
