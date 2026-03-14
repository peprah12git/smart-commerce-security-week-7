import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token') || localStorage.getItem('adminToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401 errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      localStorage.removeItem('adminToken');
      localStorage.removeItem('adminUser');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// GraphQL helper
export const graphqlQuery = async (query, variables = {}) => {
  try {
    const response = await api.post('/graphql', { query, variables });
    if (response.data.errors) {
      console.error('✗ GraphQL errors:', response.data.errors);
      throw new Error(response.data.errors[0].message);
    }
    console.log('✓ GraphQL query successful');
    return response.data.data;
  } catch (error) {
    console.error('✗ GraphQL request failed:', error.message);
    throw error;
  }
};

export default api;
