import api from './api';

const CartService = {
  // Get user's full cart with items and totals
  getCart: async (userId) => {
    const response = await api.get(`/carts/user/${userId}`);
    return response.data;
  },

  // Get cart items for a user
  getCartItems: async (userId) => {
    const response = await api.get(`/carts/user/${userId}/items`);
    return response.data;
  },

  // Get specific cart item
  getCartItem: async (userId, productId) => {
    const response = await api.get(`/carts/user/${userId}/items/${productId}`);
    return response.data;
  },

  // Add item to cart
  addToCart: async (userId, productId, quantity) => {
    const response = await api.post('/carts/items', {
      productId,
      quantity,
    });
    return response.data;
  },

  // Update cart item quantity
  updateQuantity: async (userId, productId, quantity) => {
    const response = await api.put(`/carts/user/${userId}/items/${productId}`, {
      quantity,
    });
    return response.data;
  },

  // Remove item from cart
  removeFromCart: async (userId, productId) => {
    await api.delete(`/carts/user/${userId}/items/${productId}`);
  },

  // Clear entire cart
  clearCart: async (userId) => {
    await api.delete(`/carts/user/${userId}`);
  },

  // Get cart item count
  getCartItemCount: async (userId) => {
    const response = await api.get(`/carts/user/${userId}/count`);
    return response.data;
  },

  // Get cart total
  getCartTotal: async (userId) => {
    const response = await api.get(`/carts/user/${userId}/total`);
    return response.data;
  },
};

export default CartService;
