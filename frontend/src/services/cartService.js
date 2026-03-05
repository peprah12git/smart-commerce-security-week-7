import api from './api';

const CartService = {
  // Get user's full cart with items and totals
  getCart: async () => {
    const response = await api.get('/carts/me');
    return response.data;
  },

  // Get cart items for the authenticated user
  getCartItems: async () => {
    const response = await api.get('/carts/me/items');
    return response.data;
  },

  // Get specific cart item
  getCartItem: async (productId) => {
    const response = await api.get(`/carts/me/items/${productId}`);
    return response.data;
  },

  // Add item to cart
  addToCart: async (productId, quantity) => {
    const response = await api.post('/carts/items', {
      productId,
      quantity,
    });
    return response.data;
  },

  // Update cart item quantity
  updateQuantity: async (productId, quantity) => {
    const response = await api.put(`/carts/me/items/${productId}`, {
      quantity,
    });
    return response.data;
  },

  // Remove item from cart
  removeFromCart: async (productId) => {
    await api.delete(`/carts/me/items/${productId}`);
  },

  // Clear entire cart
  clearCart: async () => {
    await api.delete('/carts/me');
  },

  // Get cart item count
  getCartItemCount: async () => {
    const response = await api.get('/carts/me/count');
    return response.data;
  },

  // Get cart total
  getCartTotal: async () => {
    const response = await api.get('/carts/me/total');
    return response.data;
  },
};

export default CartService;
