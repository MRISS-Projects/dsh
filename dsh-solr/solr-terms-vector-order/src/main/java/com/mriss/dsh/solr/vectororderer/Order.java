package com.mriss.dsh.solr.vectororderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration defining ascending or descending order. Constructor should
 * receive the enumeration description which will the the string to be used
 * during query requests. Query request format:
 * <code>order=[tv_field];asc|desc</code>. Example:
 * <code>order=tv.tf;desc</code>.
 * 
 * @author riss
 *
 */
public enum Order {

    /**
     * Ascending order.
     */
    ASC("asc"),

    /**
     * Descending order.
     */
    DESC("desc");

    /**
     * Order name (asc or desc).
     */
    private String orderName;

    /**
     * Holder inner class to store the enumerations. It also allows to get by order
     * name.
     *
     * @author riss
     *
     */
    private static class OrderNamesHolder {

        /**
         * Stores orders per order name.
         */
        private static Map<String, Order> orderNames = new HashMap<String, Order>();

        /**
         * Puts an order using its order name as a key.
         * 
         * @param orderName the order name (asc or desc).
         * @param order     the order instance.
         */
        public static void put(String orderName, Order order) {
            orderNames.put(orderName, order);
        }

        /**
         * Gets an order by its order name.
         * 
         * @param orderName the order name to search for.
         * @return the order.
         */
        public static Order get(String orderName) {
            return orderNames.get(orderName);
        }

    }

    /**
     * Constructor with the order name. It also stores the order at
     * {@link OrderNamesHolder}.
     * 
     * @param orderName the order name (asc or desc)
     */
    private Order(String orderName) {
        this.orderName = orderName;
        OrderNamesHolder.put(this.orderName, this);
    }

    /**
     * Gets an order by its order name.
     * 
     * @param orderName the order name
     * @return the order
     */
    public static Order getOrderByName(String orderName) {
        return OrderNamesHolder.get(orderName);
    }

    /**
     * Compare two integer values according with order ascending or descending. This
     * method is exclusive to operate with sorted maps or sets. It will never return
     * zero, since zero means two objects are equal and then, in a map, the objects
     * will be stored just once per key. Two overcome this, this compare method will
     * return 1 in case values are equal, allowing it to keep being stored in a map
     * or set.
     * 
     * @param paramValueO1 the first value.
     * @param paramValueO2 the second value.
     * @return in case of ASC: -1 if <code>paramValueO1 &lt; paramValueO2</code>, 1
     *         otherwise (<code>&gt;=</code>). In case of DESC: -1 if
     *         <code>paramValueO1 &gt; paramValueO2</code>, 1 otherwise
     *         (<code>&lt;=</code>).
     */
    public int compare(int paramValueO1, int paramValueO2) {
        if (this == ASC) {
            if (paramValueO1 < paramValueO2) {
                return -1;
            } else if (paramValueO1 >= paramValueO2) {
                return 1;
            } else {
                return 1;
            }
        } else if (this == DESC) {
            if (paramValueO1 > paramValueO2) {
                return -1;
            } else if (paramValueO1 <= paramValueO2) {
                return 1;
            } else {
                return 1;
            }
        } else {
            return 0;
        }
    }

}
