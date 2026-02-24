import api from './api';

const InventoryService = {
  // Get all inventory items
  getAllInventory: async () => {
    const response = await api.get('/inventory');
    return response.data;
  },

  // Get inventory for a specific product
  getInventoryByProductId: async (productId) => {
    const response = await api.get(`/inventory/${productId}`);
    return response.data;
  },

  // Get low stock items
  getLowStockItems: async (threshold = 10) => {
    const response = await api.get(`/inventory/low-stock?threshold=${threshold}`);
    return response.data;
  },

  // Get out of stock items
  getOutOfStockItems: async () => {
    const response = await api.get('/inventory/out-of-stock');
    return response.data;
  },

  // Update inventory quantity
  updateInventory: async (productId, quantity) => {
    const response = await api.put(`/inventory/${productId}`, { quantity });
    return response.data;
  },

  // Add stock
  addStock: async (productId, quantity) => {
    const response = await api.post(`/inventory/${productId}/stock-additions`, { quantity });
    return response.data;
  },

  // Reduce stock
  reduceStock: async (productId, quantity) => {
    const response = await api.post(`/inventory/${productId}/stock-reductions`, { quantity });
    return response.data;
  },

  // Check if product is in stock
  checkStock: async (productId) => {
    const response = await api.get(`/inventory/${productId}/check`);
    return response.data;
  },
};

export default InventoryService;
