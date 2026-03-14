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

const normalizeRole = (role) => {
  if (!role) return 'CUSTOMER';
  return role.startsWith('ROLE_') ? role.replace('ROLE_', '') : role;
};

const parseUserProfile = (data) => ({
  userId: data.userId,
  name: data.name,
  email: data.email,
  phone: data.phone,
  address: data.address,
  role: normalizeRole(data.role),
});

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

  getMyProfile: async () => {
    const response = await api.get('/users/me');
    return parseUserProfile(response.data);
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
    role: normalizeRole(data.role),
  }),

  // Decode user info from a raw JWT (used after OAuth2 redirect)
  parseUserFromToken: (token) => {
    const payload = decodeTokenPayload(token);
    const rawRole = (payload.roles && payload.roles[0]) || 'ROLE_CUSTOMER';
    return {
      email: payload.sub || '',
      role: normalizeRole(rawRole),
      name: payload.sub || '',
    };
  },
};

export default UserService;
