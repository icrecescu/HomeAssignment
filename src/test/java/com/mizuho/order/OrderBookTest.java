package com.mizuho.order;

import com.mizuho.order.dto.Order;
import com.mizuho.order.enums.OrderSides;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    public void setUp() {
        orderBook = new OrderBook();
    }

    @Test
    void testAddOrder_WhenNullOrder_NullPointerExceptionThrown() {
        // Arrange && Act
        NullPointerException npe = assertThrows(NullPointerException.class, () -> orderBook.addOrder(null));

        // Assert
        assertEquals("Order can't be null", npe.getMessage());
    }

    @Test
    void testAddOrder_WhenPriceNegative_IllegalArgumentExceptionThrown() {
        // Arrange
        Order order = new Order(1L, -5.74, OrderSides.OFFER.getOrderSide(), 23);

        // Act
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> orderBook.addOrder(order));

        // Assert
        assertEquals(String.format("Price of the order %s can't be negative", order.id()), iae.getMessage());
    }

    @Test
    void testAddOrder_WhenInvalidSide_IllegalArgumentExceptionThrown() {
        // Arrange
        Order order = new Order(1L, 25.0, 'X', 31);

        // Act
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> orderBook.addOrder(order));

        // Assert
        assertEquals(String.format("Unsupported order side %s", order.side()), iae.getMessage());
    }

    @Test
    void testAddOrder_WhenInvalidSize_IllegalArgumentExceptionThrown() {
        // Arrange
        Order order = new Order(1L, 25.0, OrderSides.OFFER.getOrderSide(), -31);

        // Act
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> orderBook.addOrder(order));

        // Assert
        assertEquals(String.format("Size of and order can't have negative values %s", order.size()), iae.getMessage());
    }

    @Test
    void testAddOrder_WhenInvalidId_IllegalArgumentExceptionThrown() {
        // Arrange
        Order order = new Order(-1L, 25.0, OrderSides.BID.getOrderSide(), 31);

        // Act
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> orderBook.addOrder(order));

        // Assert
        assertEquals(String.format("Id of and order can't have negative values %s", order.id()), iae.getMessage());
    }

    @Test
    void testAddOrder_WhenOrderAlreadyExists_IllegalArgumentExceptionThrown() {
        // Arrange
        Order order = new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        orderBook.addOrder(order);

        // Act
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> orderBook.addOrder(order));

        // Assert
        assertEquals(String.format("This order already exists %s", order.id()), iae.getMessage());
    }

    @Test
    void testAddOrder_WhenOrdersAdded_OrderContainedWithinBookAndLookups() {
        // Arrange
        Order order1 = new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        Order order2 = new Order(2L, 21.5, OrderSides.OFFER.getOrderSide(), 30);
        Order order3 = new Order(3L, 78.3, OrderSides.BID.getOrderSide(), 30);

        // Act
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);

        // Assert
        assertEquals(3, orderBook.getOrderBook().size(), "Invalid number of orders");
        assertEquals(1, orderBook.getOfferLevels().size(), "Invalid number of offers");
        assertEquals(2, orderBook.getBidLevels().size(), "Invalid number of bids");
    }

    @Test
    void testRemoveOderById_WhenOrderDoesntExists_IllegalArgumentExceptionThrown() {
        // Arrange
        long inexistentOrderId = 1;

        // Act
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> orderBook.removeOrderById(inexistentOrderId));

        // Assert
        assertEquals(String.format("Order %s doesn't exist", inexistentOrderId), iae.getMessage());
    }

    @Test
    void testRemoveOrderById_WhenOrderRemoved_OrderRemovedFromBookAndLookupsAndLevelRemoved() {
        // Arrange
        Order order1 = new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        Order order2 = new Order(2L, 20.3, OrderSides.OFFER.getOrderSide(), 5);
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        assertEquals(2, orderBook.getOrderBook().size());
        assertEquals(1, orderBook.getBidLevels().size());
        assertEquals(1, orderBook.getOfferLevels().size());

        // Act
        orderBook.removeOrderById(order1.id());
        orderBook.removeOrderById(order2.id());

        // Assert
        assertTrue(orderBook.getOrderBook().isEmpty(), "Order book should be empty");
        assertTrue(orderBook.getBidLevels().isEmpty(), "Bids lookup should be empty");
        assertTrue(orderBook.getOfferLevels().isEmpty(), "Offers lookup should be empty");
    }

    @Test
    void testRemoveOrderById_WhenOrderRemoved_NonRemovedOrdersAreStillPersisted() {
        // Arrange
        Order order1 = new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        Order order2 = new Order(2L, 20.3, OrderSides.OFFER.getOrderSide(), 5);
        Order order3 = new Order(3L, 17.5, OrderSides.BID.getOrderSide(), 5);
        Order order4 = new Order(4L, 10.9, OrderSides.OFFER.getOrderSide(), 5);
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);
        orderBook.addOrder(order4);
        assertEquals(4, orderBook.getOrderBook().size());
        assertEquals(2, orderBook.getBidLevels().size());
        assertEquals(2, orderBook.getOfferLevels().size());

        // Act
        orderBook.removeOrderById(order1.id());
        orderBook.removeOrderById(order2.id());

        // Assert
        assertEquals(2, orderBook.getOrderBook().size(), "Order book should have exactly 2 entries");
        assertEquals(1, orderBook.getBidLevels().size(), "Bids lookup should have exactly 1 entry");
        assertEquals(1, orderBook.getOfferLevels().size(), "Offers lookup should have exactly 1 entry");

        assertTrue(orderBook.getOrderBook().containsKey(order3.id()));
        assertTrue(orderBook.getOrderBook().containsKey(order4.id()));
        assertTrue(orderBook.getBidLevels().containsKey(order3.price()));
        assertTrue(orderBook.getOfferLevels().containsKey(order4.price()));
    }

    @Test
    void testUpdateOrderSizeById_WhenOrderDoesntExists_IllegalArgumentExceptionThrown() {
        // Arrange
        long inexistentOrderId = 1;
        long size = 3;

        // Act
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> orderBook.updateOrderSizeById(inexistentOrderId, size));

        // Assert
        assertEquals(String.format("Order %s doesn't exist", inexistentOrderId), iae.getMessage());
    }

    @Test
    void testUpdateOrderSizeById_WhenInvalidSize_IllegalArgumentExceptionThrown() {
        // Arrange
        Order order = new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        orderBook.addOrder(order);
        long invalidSize = -30;

        // Act
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> orderBook.updateOrderSizeById(order.id(), invalidSize));

        // Assert
        assertEquals(String.format("Size of and order can't have negative values %s", invalidSize), iae.getMessage());
    }

    @Test
    void testUpdateOrderSizeById_WhenOrderSizeUpdated_SizePersistentInBookAndLookup() {
        // Arrange
        Order order = new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        orderBook.addOrder(order);
        long newSize = 40;

        // Act
        orderBook.updateOrderSizeById(order.id(), newSize);

        // Assert
        Order orderStoredInBook = orderBook.getOrderBook().get(order.id());
        assertEquals(newSize, orderStoredInBook.size());
        assertEquals(1L, orderBook.getBidLevels().get(order.price()).iterator().next());
    }

    @Test
    void testGetPriceOfLevelBySide_WhenInvalidLevel_IllegalArgumentExceptionThrown() {
        // Arrange
        int invalidNegativeLevel = -1;
        int invalidPositiveLevel = 10;

        // Act
        IllegalArgumentException iaen = assertThrows(IllegalArgumentException.class,
                () -> orderBook.getPriceOfLevelBySide(invalidNegativeLevel, OrderSides.BID.getOrderSide()));
        IllegalArgumentException iaep = assertThrows(IllegalArgumentException.class,
                () -> orderBook.getPriceOfLevelBySide(invalidPositiveLevel, OrderSides.OFFER.getOrderSide()));

        // Assert
        assertEquals(String.format("Level can't be negative %s", invalidNegativeLevel), iaen.getMessage());
        assertEquals(String.format("Level %s is out of range", invalidPositiveLevel), iaep.getMessage());
    }

    @Test
    void testGetPriceOfLevelBySide_WhenInvalidSide_IllegalArgumentExceptionThrown() {
        // Arrange
        char invalidSide = 'X';

        // Act
        IllegalArgumentException iaen = assertThrows(IllegalArgumentException.class,
                () -> orderBook.getPriceOfLevelBySide(1, invalidSide));

        // Assert
        assertEquals(String.format("Unsupported order side %s", invalidSide), iaen.getMessage());
    }

    @Test
    void testGetPriceOfLevelBySide_WhenThereAreMoreThanOneLevel_TheRightPriceIsReturnedForEachLevel() {
        // Arrange
        // lvl 2 bid
        Order order1= new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        // lvl 3 bid
        Order order2 = new Order(2L, 12.5, OrderSides.BID.getOrderSide(), 4);
        // lvl 1 bid
        Order order3 = new Order(3L, 27.8, OrderSides.BID.getOrderSide(), 10);
        Order order4 = new Order(4L, 27.8, OrderSides.BID.getOrderSide(), 10);
        Order order5 = new Order(5L, 27.8, OrderSides.BID.getOrderSide(), 10);
        // lvl 1 offer
        Order order6 = new Order(6L, 17.1, OrderSides.OFFER.getOrderSide(), 11);
        // lvl 2 offer
        Order order7 = new Order(7L, 97.5, OrderSides.OFFER.getOrderSide(), 17);
        
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);
        orderBook.addOrder(order4);
        orderBook.addOrder(order5);
        orderBook.addOrder(order6);
        orderBook.addOrder(order7);

        // Act && Assert
        assertEquals(27.8, orderBook.getPriceOfLevelBySide(1, OrderSides.BID.getOrderSide()));
        assertEquals(22.3, orderBook.getPriceOfLevelBySide(2, OrderSides.BID.getOrderSide()));
        assertEquals(12.5, orderBook.getPriceOfLevelBySide(3, OrderSides.BID.getOrderSide()));
        assertEquals(17.1, orderBook.getPriceOfLevelBySide(1, OrderSides.OFFER.getOrderSide()));
        assertEquals(97.5, orderBook.getPriceOfLevelBySide(2, OrderSides.OFFER.getOrderSide()));
    }

    @Test
    void testGetTotalSizeByLevelAndSide_WhenInvalidLevel_IllegalArgumentExceptionThrown() {
        // Arrange
        int invalidNegativeLevel = -1;

        // Act
        IllegalArgumentException iaen = assertThrows(IllegalArgumentException.class,
                () -> orderBook.getTotalSizeByLevelAndSide(invalidNegativeLevel, OrderSides.BID.getOrderSide()));

        // Assert
        assertEquals(String.format("Level can't be negative %s", invalidNegativeLevel), iaen.getMessage());
    }

    @Test
    void testGetTotalSizeByLevelAndSide_WhenInvalidSide_IllegalArgumentExceptionThrown() {
        // Arrange
        char invalidSide = 'X';

        // Act
        IllegalArgumentException iaen = assertThrows(IllegalArgumentException.class,
                () -> orderBook.getTotalSizeByLevelAndSide(1, invalidSide));

        // Assert
        assertEquals(String.format("Unsupported order side %s", invalidSide), iaen.getMessage());
    }

    @Test
    void testGetTotalSizeByLevelAndSide_WhenThereAreMoreThanOneLevel_TheRightPriceIsReturnedForEachLevel() {
        // Arrange
        // lvl 2 bid
        Order order1= new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        // lvl 3 bid
        Order order2 = new Order(2L, 12.5, OrderSides.BID.getOrderSide(), 4);
        // lvl 1 bid
        Order order3 = new Order(3L, 27.8, OrderSides.BID.getOrderSide(), 10);
        Order order4 = new Order(4L, 27.8, OrderSides.BID.getOrderSide(), 10);
        Order order5 = new Order(5L, 27.8, OrderSides.BID.getOrderSide(), 10);
        // lvl 1 offer
        Order order6 = new Order(6L, 17.1, OrderSides.OFFER.getOrderSide(), 11);
        // lvl 2 offer
        Order order7 = new Order(7L, 97.5, OrderSides.OFFER.getOrderSide(), 17);

        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);
        orderBook.addOrder(order4);
        orderBook.addOrder(order5);
        orderBook.addOrder(order6);
        orderBook.addOrder(order7);

        // Act && Assert
        long lvl1Offer = order6.size();
        long lvl2Offer = order7.size();
        long lvl1Bid = order3.size() + order4.size() + order5.size();
        long lvl2Bid = order1.size();
        long lvl3Bid = order2.size();
        assertEquals(lvl1Bid, orderBook.getTotalSizeByLevelAndSide(1, OrderSides.BID.getOrderSide()));
        assertEquals(lvl2Bid, orderBook.getTotalSizeByLevelAndSide(2, OrderSides.BID.getOrderSide()));
        assertEquals(lvl3Bid, orderBook.getTotalSizeByLevelAndSide(3, OrderSides.BID.getOrderSide()));
        assertEquals(lvl1Offer, orderBook.getTotalSizeByLevelAndSide(1, OrderSides.OFFER.getOrderSide()));
        assertEquals(lvl2Offer, orderBook.getTotalSizeByLevelAndSide(2, OrderSides.OFFER.getOrderSide()));
    }

    @Test
    void testGetTotalSizeByLevelAndSide_WhenThereAreMoreThanOneLevelWithRemovals_TheRightPriceIsReturnedForEachLevel() {
        // Arrange
        // lvl 2 bid
        Order order1= new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        // lvl 3 bid
        Order order2 = new Order(2L, 12.5, OrderSides.BID.getOrderSide(), 4);
        // lvl 1 bid
        Order order3 = new Order(3L, 27.8, OrderSides.BID.getOrderSide(), 10);
        Order order4 = new Order(4L, 27.8, OrderSides.BID.getOrderSide(), 10);
        Order order5 = new Order(5L, 27.8, OrderSides.BID.getOrderSide(), 10);
        // lvl 1 offer
        Order order6 = new Order(6L, 17.1, OrderSides.OFFER.getOrderSide(), 11);
        // lvl 2 offer
        Order order7 = new Order(7L, 97.5, OrderSides.OFFER.getOrderSide(), 17);

        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);
        orderBook.addOrder(order4);
        orderBook.addOrder(order5);
        orderBook.addOrder(order6);
        orderBook.addOrder(order7);
        orderBook.removeOrderById(order7.id());
        orderBook.removeOrderById(order3.id());

        // Act && Assert
        long lvl1Offer = order6.size();
        long lvl1Bid = order4.size() + order5.size();
        long lvl2Bid = order1.size();
        long lvl3Bid = order2.size();
        assertEquals(lvl1Bid, orderBook.getTotalSizeByLevelAndSide(1, OrderSides.BID.getOrderSide()));
        assertEquals(lvl2Bid, orderBook.getTotalSizeByLevelAndSide(2, OrderSides.BID.getOrderSide()));
        assertEquals(lvl3Bid, orderBook.getTotalSizeByLevelAndSide(3, OrderSides.BID.getOrderSide()));
        assertEquals(lvl1Offer, orderBook.getTotalSizeByLevelAndSide(1, OrderSides.OFFER.getOrderSide()));
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> orderBook.getTotalSizeByLevelAndSide(2, OrderSides.OFFER.getOrderSide()));
        assertEquals(String.format("Level %s is out of range", 2), iae.getMessage());
    }

    @Test
    void testGetAllOrdersBySide_WhenInvalidSide_IllegalArgumentExceptionThrown() {
        // Arrange
        char invalidSide = 'X';

        // Act
        IllegalArgumentException iaen = assertThrows(IllegalArgumentException.class,
                () -> orderBook.getAllOrdersBySide(invalidSide));

        // Assert
        assertEquals(String.format("Unsupported order side %s", invalidSide), iaen.getMessage());
    }

    @Test
    void testGetAllOrdersBySide_WhenThereAreMoreThanOneLevel_TheResultIsReturnedInTimeOrder() {
        // Arrange
        // Arrange
        // lvl 2 bid
        Order order1= new Order(1L, 22.3, OrderSides.BID.getOrderSide(), 30);
        // lvl 3 bid
        Order order2 = new Order(2L, 12.5, OrderSides.BID.getOrderSide(), 4);
        // lvl 1 bid
        Order order3 = new Order(3L, 27.8, OrderSides.BID.getOrderSide(), 10);
        Order order4 = new Order(4L, 27.8, OrderSides.BID.getOrderSide(), 10);
        Order order5 = new Order(5L, 27.8, OrderSides.BID.getOrderSide(), 10);
        // lvl 1 offer
        Order order6 = new Order(6L, 17.1, OrderSides.OFFER.getOrderSide(), 11);
        // lvl 2 offer
        Order order7 = new Order(7L, 97.5, OrderSides.OFFER.getOrderSide(), 17);

        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);
        orderBook.addOrder(order4);
        orderBook.addOrder(order5);
        orderBook.addOrder(order6);
        orderBook.addOrder(order7);
        
        // Act
        List<Order> bidOrdersBySide = orderBook.getAllOrdersBySide(OrderSides.BID.getOrderSide());
        List<Order> offerOrdersBySide = orderBook.getAllOrdersBySide(OrderSides.OFFER.getOrderSide());

        // Assert
        assertEquals(Arrays.asList(order3, order4, order5, order1, order2), bidOrdersBySide);
        assertEquals(Arrays.asList(order6, order7), offerOrdersBySide);
    }
}