import React, { createContext, useContext, useState, useEffect } from 'react';
import CategoryService from '../services/categoryService';
import UserService from '../services/userService';

const AppContext = createContext();

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};

export const AppProvider = ({ children }) => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState(null);
  const [user, setUser] = useState(() => {
    // Check client user first, then admin user
    const clientUser = localStorage.getItem('user');
    if (clientUser) return JSON.parse(clientUser);
    const adminUser = localStorage.getItem('adminUser');
    if (adminUser) return JSON.parse(adminUser);
    return null;
  });

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async () => {
    try {
      const data = await CategoryService.getAllCategories();
      setCategories(data);
    } catch (error) {
      console.error('Failed to fetch categories:', error);
    } finally {
      setLoading(false);
    }
  };

  const showNotification = (message, type = 'info') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 5000);
  };

  const logout = async () => {
    await UserService.logout(); // revokes token server-side + clears localStorage
    setUser(null);
  };

  const value = {
    categories,
    setCategories,
    loading,
    notification,
    showNotification,
    refreshCategories: fetchCategories,
    user,
    setUser,
    logout,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
};

export default AppContext;