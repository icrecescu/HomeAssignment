### About:
This project simulates a limit order book stores customer orders on a price time priority basis. The highest bid and lowest offer
are considered "best" with all other orders stacked in price levels behind. In this test, the best order is considered to be at level 1.

### Minimum requirements:
    - Java 17

### To run all the tests execute the following command:
    ./gradlew test

### Note:
This solution assumes that eventual consistency it's allowed. In a multithreaded environment there is a tiny time window
in which the reading threads won't see the latest updates of the writing threads. For a stronger consistency policy
additional synchronization mechanisms are required

## Part B
As an improvement I would suggest having many instances of the same application behind a load balancer. Every instance 
processes the orders in parallel and store the results in a distributed caching system like Redis,Memcached, Hazelcast, Ehcache etc...

A further improvement if a lot of data would be involved will be to use a map-reduce algorithm system like Apache Hadoop  