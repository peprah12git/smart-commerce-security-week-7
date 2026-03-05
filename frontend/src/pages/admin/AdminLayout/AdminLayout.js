import React, { useEffect } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useApp } from '../../../context/AppContext';
import UserService from '../../../services/userService';
import {
  LayoutDashboard,
  Package,
  Tag,
  Users,
  Settings,
  ArrowLeft,
  Menu,
  ClipboardList,
  Warehouse,
  LogOut,
  Activity,
} from 'lucide-react';
import './AdminLayout.css';

const AdminLayout = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, showNotification } = useApp();

  useEffect(() => {
    const adminUser = localStorage.getItem('adminUser');
    if (!adminUser) {
      navigate('/admin/login');
      return;
    }
    
    try {
      const parsedUser = JSON.parse(adminUser);
      if (parsedUser.role !== 'ADMIN') {
        localStorage.removeItem('adminUser');
        localStorage.removeItem('adminToken');
        navigate('/admin/login');
      }
    } catch (error) {
      localStorage.removeItem('adminUser');
      localStorage.removeItem('adminToken');
      navigate('/admin/login');
    }
  }, [navigate]);

  const navItems = [
    { path: '/admin', icon: LayoutDashboard, label: 'Dashboard', exact: true },
    { path: '/admin/products', icon: Package, label: 'Products' },
    { path: '/admin/categories', icon: Tag, label: 'Categories' },
    { path: '/admin/orders', icon: ClipboardList, label: 'Orders' },
    { path: '/admin/inventory', icon: Warehouse, label: 'Inventory' },
    { path: '/admin/performance', icon: Activity, label: 'Performance' },
  ];

  const isActive = (path, exact = false) => {
    if (exact) {
      return location.pathname === path;
    }
    return location.pathname.startsWith(path);
  };

  const handleLogout = async () => {
    await UserService.logout(); // revokes JWT server-side + clears localStorage
    showNotification('Successfully logged out', 'info');
    navigate('/login');
  };

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <div className="sidebar-header">
          <Link to="/admin" className="admin-logo">
            <Package size={24} />
            <span>Admin Panel</span>
          </Link>
        </div>

        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`nav-item ${isActive(item.path, item.exact) ? 'active' : ''}`}
            >
              <item.icon size={20} />
              <span>{item.label}</span>
            </Link>
          ))}
        </nav>

        <div className="sidebar-footer">
          <button onClick={handleLogout} className="logout-btn">
            <LogOut size={18} />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      <main className="admin-main">
        <header className="admin-header">
          <button className="mobile-menu-btn">
            <Menu size={24} />
          </button>
          <div className="admin-header-title">
            {navItems.find((item) => isActive(item.path, item.exact))?.label || 'Admin'}
          </div>
        </header>

        <div className="admin-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
};

export default AdminLayout;
