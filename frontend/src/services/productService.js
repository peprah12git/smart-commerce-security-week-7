import api from './api';
import graphqlService from './graphqlService';

const ProductService = {
  // Get all products without pagination - USE GRAPHQL
  getAllProducts: async () => {
    return await graphqlService.getAllProducts();
  },

  // Get products with filtering - USE GRAPHQL
  getProducts: async (params = {}) => {
    const { category, minPrice, maxPrice, searchTerm, page = 0, size = 10 } = params;

    return await graphqlService.getProducts({ 
      category, 
      minPrice, 
      maxPrice, 
      searchTerm,
      page,
      size
    });
  },

  // Get single product by ID - USE GRAPHQL
  getProductById: async (id) => {
    return await graphqlService.getProductById(id);
  },

  // Get products by category
  getProductsByCategory: async (categoryName) => {
    const response = await api.get(`/products/category/${categoryName}`);
    return response.data;
  },

  // Search products
  searchProducts: async (term) => {
    const response = await api.get(`/products/search?term=${encodeURIComponent(term)}`);
    return response.data;
  },

  // Create new product
  createProduct: async (productData) => {
    const response = await api.post('/products', productData);
    return response.data;
  },

  // Update product
  updateProduct: async (id, productData) => {
    const response = await api.put(`/products/${id}`, productData);
    return response.data;
  },
// Update product quantity
  updateProductQuantity: async (id, quantity) => {
    const response = await api.put(`/inventory/${id}`, { quantity });
    return response.data;
  },

  // Delete product
  deleteProduct: async (id) => {
    await api.delete(`/products/${id}`);
  },

  // Clear cache
  clearCache: async () => {
    await api.delete('/products/cache');
  },
};

export default ProductService;
