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

  // Get current user's orders (non-paginated - legacy)
  getOrdersByUser: async () => {
    const response = await api.get('/orders/me');
    return response.data;
  },

  // Get current user's orders with pagination (RECOMMENDED)
  getOrdersByUserPaged: async (page = 0, size = 10, status = null) => {
    const params = new URLSearchParams({
      page,
      size,
    });
    if (status) {
      params.append('status', status);
    }
    const response = await api.get(`/orders/me/paged?${params}`);
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

  // Cancel order
  cancelOrder: async (orderId) => {
    const response = await api.post(`/orders/${orderId}/cancellations`);
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

  // Create order from cart
  checkoutFromCart: async (userId) => {
    const response = await api.post(`/orders/checkout`);
    return response.data;
  },
};

export default OrderService;
