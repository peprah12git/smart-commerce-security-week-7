import api from './api';

// Decode a JWT payload without verifying the signature (client-side only)
const decodeTokenPayload = (token) => {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch {
    return {};
  }
};

const UserService = {
  // Register new user — POST /api/users
  register: async (userData) => {
    const response = await api.post('/users', userData);
    return response.data; // LoginResponseDTO: {userId, name, email, role, token, message}
  },

  // Login user — POST /api/auth/login (includes brute-force protection)
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    return response.data; // LoginResponseDTO: {userId, name, email, role, token, message}
  },

  // Logout — POST /api/auth/logout (revokes JWT in server-side blacklist)
  logout: async () => {
    try {
      await api.post('/auth/logout');
    } catch (err) {
      // Best-effort — clear local storage regardless
      console.warn('Logout request failed:', err.message);
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      localStorage.removeItem('adminToken');
      localStorage.removeItem('adminUser');
    }
  },

  // Build a user object from a flat LoginResponseDTO
  parseUserFromResponse: (data) => ({
    userId: data.userId,
    name: data.name,
    email: data.email,
    role: data.role,
  }),

  // Decode user info from a raw JWT (used after OAuth2 redirect)
  parseUserFromToken: (token) => {
    const payload = decodeTokenPayload(token);
    return {
      email: payload.sub || '',
      role: (payload.roles && payload.roles[0]) || 'ROLE_CUSTOMER',
      name: payload.sub || '',
    };
  },
};

export default UserService;
