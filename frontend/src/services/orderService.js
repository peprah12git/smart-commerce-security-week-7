import api from './api';

const OrderService = {
  // Get all orders (admin)
  getAllOrders: async () => {
    const response = await api.get('/orders');
    return response.data;
  },

  // Get order by ID
  getOrderById: async (orderId) => {
    const response = await api.get(`/orders/${orderId}`);
    return response.data;
  },

  // Get orders by user
  getOrdersByUser: async (userId) => {
    const response = await api.get(`/orders/user/${userId}`);
    return response.data;
  },

  // Create new order
  createOrder: async (orderData) => {
    const response = await api.post('/orders', orderData);
    return response.data;
  },

  // Update order status
  updateOrderStatus: async (orderId, status) => {
    const response = await api.patch(`/orders/${orderId}/status`, { status });
    return response.data;
  },

  // Cancel order (use DELETE on cancellation resource)
  cancelOrder: async (orderId) => {
    const response = await api.delete(`/orders/${orderId}/cancellation`);
    return response.data;
  },

  // Delete order
  deleteOrder: async (orderId) => {
    await api.delete(`/orders/${orderId}`);
  },

  // Get order items
  getOrderItems: async (orderId) => {
    const response = await api.get(`/orders/${orderId}/items`);
    return response.data;
  },

  // Checkout from cart (create order from cart)
  checkoutFromCart: async (userId) => {
    const response = await api.post(`/orders/from-cart`);
    return response.data;
  },
};

export default OrderService;
