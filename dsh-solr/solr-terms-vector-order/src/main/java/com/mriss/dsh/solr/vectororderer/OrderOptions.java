package com.mriss.dsh.solr.vectororderer;

/**
 * Class holding the order options. Mainly the {@link Order} instance and the
 * field used to order.
 * 
 * @author riss
 *
 */
public class OrderOptions {

    /**
     * The order type (asc or desc)
     */
    private Order order;

    /**
     * The field/terms vector param to be used to order.
     */
    private String termVectorParam;

    /**
     * Default constructor.
     * 
     * @param order      the order (asc or desc)
     * @param orderField the field to be used when ordering.
     */
    public OrderOptions(Order order, String orderField) {
        super();
        this.order = order;
        this.termVectorParam = orderField;
    }

    /**
     * Gets the order.
     * 
     * @return the order.
     */
    public Order getOrder() {
        return order;
    }

    /**
     * Gets the param/field.
     * 
     * @return the param/field.
     */
    public String getTermVectorParam() {
        return termVectorParam;
    }

    @Override
    public String toString() {
        return "OrderOptions [order=" + order + ", orderField=" + termVectorParam + "]";
    }

}
