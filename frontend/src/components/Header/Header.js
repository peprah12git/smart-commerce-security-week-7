import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ShoppingBag, Search, User, Menu, ShoppingCart, LogOut } from 'lucide-react';
import { useApp } from '../../context/AppContext';
import UserService from '../../services/userService';
import './Header.css';

const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { showNotification } = useApp();
  const [user, setUser] = useState(null);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const isAdmin = location.pathname.startsWith('/admin');

  useEffect(() => {
    checkAuthStatus();
  }, [location.pathname]);

  const checkAuthStatus = () => {
    try {
      const userData = localStorage.getItem('user');
      const token = localStorage.getItem('token');
      
      if (token && userData) {
        setUser(JSON.parse(userData));
      } else {
        setUser(null);
      }
    } catch (error) {
      console.error('Failed to check auth status:', error);
      setUser(null);
    }
  };

  const handleLogout = async () => {
    await UserService.logout(); // revokes JWT server-side + clears localStorage
    setUser(null);
    setShowUserMenu(false);
    showNotification('Successfully logged out', 'info');
    navigate('/login');
  };

  const toggleUserMenu = () => {
    setShowUserMenu(!showUserMenu);
  };

  // Close user menu when clicking outside
  useEffect(() => {
    const handleClickOutside = () => {
      setShowUserMenu(false);
    };
    
    if (showUserMenu) {
      document.addEventListener('click', handleClickOutside);
      return () => document.removeEventListener('click', handleClickOutside);
    }
  }, [showUserMenu]);

  return (
    <header className="header">
      <div className="container header-container">
        <Link to="/home" className="logo">
          <ShoppingBag size={28} />
          <span>SmartCommerce</span>
        </Link>

        <nav className="nav-links">
          <Link to="/home" className={location.pathname === '/home' ? 'active' : ''}>
            Home
          </Link>
          <Link to="/products" className={location.pathname.startsWith('/products') ? 'active' : ''}>
            Products
          </Link>
          <Link to="/categories" className={location.pathname.startsWith('/categories') ? 'active' : ''}>
            Categories
          </Link>
        </nav>

        <div className="header-actions">
          <Link to="/products" className="search-btn">
            <Search size={20} />
          </Link>
          <Link to="/cart" className="cart-btn">
            <ShoppingCart size={20} />
          </Link>
          
          {user ? (
            <div className="user-menu" onClick={(e) => e.stopPropagation()}>
              <button className="user-btn logged-in" onClick={toggleUserMenu}>
                <User size={20} />
                <span>{user.name}</span>
              </button>
              {showUserMenu && (
                <div className="user-dropdown">
                  <div className="user-info">
                    <strong>{user.name}</strong>
                    <span>{user.email}</span>
                  </div>
                  <hr />
                  <Link to="/profile" onClick={() => setShowUserMenu(false)}>
                    My Profile
                  </Link>
                  <Link to="/orders" onClick={() => setShowUserMenu(false)}>
                    My Orders
                  </Link>
                  <Link to="/cart" onClick={() => setShowUserMenu(false)}>
                    My Cart
                  </Link>
                  <hr />
                  <button className="logout-btn" onClick={handleLogout}>
                    <LogOut size={16} />
                    Logout
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="auth-buttons">
              <Link to="/login" className="btn btn-outline btn-sm">
                Sign In
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm">
                Sign Up
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
