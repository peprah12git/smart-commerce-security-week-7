import api from './api';
import graphqlService from './graphqlService';

const ProductService = {
  // Get all products without pagination - USE GRAPHQL
  getAllProducts: async () => {
    return await graphqlService.getAllProducts();
  },

  // Get products with filtering - USE GRAPHQL
  getProducts: async (params = {}) => {
    const {
      sortBy = 'productId',
      sortDirection = 'ASC',
      category,
      minPrice,
      maxPrice,
      searchTerm,
      inStock,
    } = params;

    // Use GraphQL for simple filtering (no sorting/inStock filter)
    if (sortBy === 'productId' && sortDirection === 'ASC' && !inStock) {
      return await graphqlService.getProducts({ category, minPrice, maxPrice, searchTerm });
    }

    // Use REST for complex queries with sorting and inStock filter
    const queryParams = new URLSearchParams({
      sortBy,
      sortDirection,
    });

    if (category) queryParams.append('category', category);
    if (minPrice) queryParams.append('minPrice', minPrice.toString());
    if (maxPrice) queryParams.append('maxPrice', maxPrice.toString());
    if (searchTerm) queryParams.append('searchTerm', searchTerm);
    if (inStock !== undefined) queryParams.append('inStock', inStock.toString());

    const response = await api.get(`/products?${queryParams}`);
    return response.data;
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
    const response = await api.patch(`/products/${id}/quantity`, { quantity });
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
