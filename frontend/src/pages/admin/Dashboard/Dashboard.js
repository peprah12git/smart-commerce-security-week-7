import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Package, Tag, TrendingUp, DollarSign, Plus } from 'lucide-react';
import ProductService from '../../../services/productService';
import { useApp } from '../../../context/AppContext';
import './Dashboard.css';

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalProducts: 0,
    outOfStock: 0,
  });
  const [recentProducts, setRecentProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const { categories } = useApp();

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const products = await ProductService.getAllProducts();
      const outOfStock = products.filter((p) => (p.inventory?.quantityAvailable || 0) === 0).length;

      setStats({
        totalProducts: products.length,
        outOfStock,
      });

      setRecentProducts(products.slice(0, 5));
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(price);
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1>Dashboard</h1>
        <div className="quick-actions">
          <Link to="/admin/products/new" className="btn btn-primary">
            <Plus size={18} />
            Add Product
          </Link>
          <Link to="/admin/categories/new" className="btn btn-outline">
            <Plus size={18} />
            Add Category
          </Link>
        </div>
      </div>

      <div className="stats-cards">
        <div className="stat-card">
          <div className="stat-icon products">
            <Package size={24} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats.totalProducts}</span>
            <span className="stat-label">Total Products</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon categories">
            <Tag size={24} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{categories.length}</span>
            <span className="stat-label">Categories</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon stock">
            <TrendingUp size={24} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats.totalProducts - stats.outOfStock}</span>
            <span className="stat-label">In Stock</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon warning">
            <DollarSign size={24} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats.outOfStock}</span>
            <span className="stat-label">Out of Stock</span>
          </div>
        </div>
      </div>

      <div className="dashboard-grid">
        <div className="card recent-products">
          <div className="card-header">
            <h2>Recent Products</h2>
            <Link to="/admin/products" className="view-all-link">
              View All
            </Link>
          </div>
          <div className="card-body">
            {loading ? (
              <p>Loading...</p>
            ) : recentProducts.length === 0 ? (
              <p className="empty-message">No products yet</p>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Stock</th>
                  </tr>
                </thead>
                <tbody>
                  {recentProducts.map((product) => (
                    <tr key={product.productId}>
                      <td>
                        <Link to={`/admin/products/${product.productId}`}>
                          {product.productName}
                        </Link>
                      </td>
                      <td>{product.categoryName}</td>
                      <td>{formatPrice(product.price)}</td>
                      <td>
                        <span
                          className={`badge ${
                            (product.inventory?.quantityAvailable || 0) > 0 ? 'badge-success' : 'badge-danger'
                          }`}
                        >
                          {product.inventory?.quantityAvailable || 0}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        <div className="card categories-overview">
          <div className="card-header">
            <h2>Categories</h2>
            <Link to="/admin/categories" className="view-all-link">
              View All
            </Link>
          </div>
          <div className="card-body">
            {categories.length === 0 ? (
              <p className="empty-message">No categories yet</p>
            ) : (
              <ul className="category-list">
                {categories.map((category) => (
                  <li key={category.categoryId}>
                    <Link to={`/admin/categories/${category.categoryId}`}>
                      <Tag size={16} />
                      <span>{category.categoryName}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
