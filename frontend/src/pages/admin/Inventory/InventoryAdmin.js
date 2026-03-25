import React, { useState, useEffect } from 'react';
import { Warehouse, AlertTriangle, Package, RefreshCw, Plus, Minus } from 'lucide-react';
import ProductService from '../../../services/productService';
import InventoryService from '../../../services/inventoryService';
import Modal from '../../../components/Modal/Modal';
import Loading from '../../../components/Loading/Loading';
import { useApp } from '../../../context/AppContext';
import './InventoryAdmin.css';

const InventoryAdmin = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all'); // all, low, out
  const [updateModal, setUpdateModal] = useState({ open: false, product: null });
  const [quantity, setQuantity] = useState('');
  const [updating, setUpdating] = useState(false);
  const [updateMode, setUpdateMode] = useState('set'); // 'set' or 'add'
  const { showNotification } = useApp();

  const LOW_STOCK_THRESHOLD = 10;

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const data = await ProductService.getAllProducts();
      const normalized = Array.isArray(data) ? data : (data?.content || []);
      setProducts(normalized);
    } catch (error) {
      console.error('Failed to fetch products:', error);
      showNotification('Failed to load inventory', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateQuantity = async () => {
    if (!updateModal.product || quantity === '') return;

    setUpdating(true);
    try {
      if (updateMode === 'add') {
        await InventoryService.addStock(
          updateModal.product.productId,
          parseInt(quantity)
        );
      } else {
        await ProductService.updateProductQuantity(
          updateModal.product.productId,
          parseInt(quantity)
        );
      }
      showNotification('Inventory updated successfully', 'success');
      setUpdateModal({ open: false, product: null });
      setQuantity('');
      setUpdateMode('set');
      fetchProducts();
    } catch (error) {
      showNotification(error.response?.data?.message || 'Failed to update inventory', 'error');
    } finally {
      setUpdating(false);
    }
  };

  const openUpdateModal = (product) => {
    setUpdateModal({ open: true, product });
    setQuantity('');
    setUpdateMode('set');
  };

  const getFilteredProducts = () => {
    const source = Array.isArray(products) ? products : [];

    switch (filter) {
      case 'low':
        return source.filter(
          (p) => p.inventory?.quantityAvailable > 0 && p.inventory?.quantityAvailable <= LOW_STOCK_THRESHOLD
        );
      case 'out':
        return source.filter((p) => p.inventory?.quantityAvailable === 0);
      default:
        return source;
    }
  };

  const getStockStatus = (qty) => {
    if (qty === 0) return { label: 'Out of Stock', color: '#dc2626', bg: '#fee2e2' };
    if (qty <= LOW_STOCK_THRESHOLD) return { label: 'Low Stock', color: '#f59e0b', bg: '#fef3c7' };
    return { label: 'In Stock', color: '#10b981', bg: '#d1fae5' };
  };

  const stats = {
    total: products.length,
    inStock: products.filter((p) => p.inventory?.quantityAvailable > LOW_STOCK_THRESHOLD).length,
    lowStock: products.filter(
      (p) => p.inventory?.quantityAvailable > 0 && p.inventory?.quantityAvailable <= LOW_STOCK_THRESHOLD
    ).length,
    outOfStock: products.filter((p) => p.inventory?.quantityAvailable === 0).length,
  };

  const filteredProducts = getFilteredProducts();

  return (
    <div className="inventory-admin">
      <div className="page-header">
        <h1>
          <Warehouse size={24} />
          Inventory Management
        </h1>
        <button className="btn btn-outline" onClick={fetchProducts} disabled={loading}>
          <RefreshCw size={18} className={loading ? 'spin' : ''} />
          Refresh
        </button>
      </div>

      {/* Stats Cards */}
      <div className="stats-grid">
        <div className="stat-card" onClick={() => setFilter('all')}>
          <Package size={24} className="stat-icon" />
          <div className="stat-info">
            <span className="stat-value">{stats.total}</span>
            <span className="stat-label">Total Products</span>
          </div>
        </div>
        <div className="stat-card success" onClick={() => setFilter('all')}>
          <Package size={24} className="stat-icon" />
          <div className="stat-info">
            <span className="stat-value">{stats.inStock}</span>
            <span className="stat-label">In Stock</span>
          </div>
        </div>
        <div className="stat-card warning" onClick={() => setFilter('low')}>
          <AlertTriangle size={24} className="stat-icon" />
          <div className="stat-info">
            <span className="stat-value">{stats.lowStock}</span>
            <span className="stat-label">Low Stock</span>
          </div>
        </div>
        <div className="stat-card danger" onClick={() => setFilter('out')}>
          <AlertTriangle size={24} className="stat-icon" />
          <div className="stat-info">
            <span className="stat-value">{stats.outOfStock}</span>
            <span className="stat-label">Out of Stock</span>
          </div>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="filter-tabs">
        <button
          className={`filter-tab ${filter === 'all' ? 'active' : ''}`}
          onClick={() => setFilter('all')}
        >
          All Products
        </button>
        <button
          className={`filter-tab ${filter === 'low' ? 'active' : ''}`}
          onClick={() => setFilter('low')}
        >
          Low Stock ({stats.lowStock})
        </button>
        <button
          className={`filter-tab ${filter === 'out' ? 'active' : ''}`}
          onClick={() => setFilter('out')}
        >
          Out of Stock ({stats.outOfStock})
        </button>
      </div>

      {/* Products Table */}
      <div className="card">
        <div className="card-body">
          {loading ? (
            <Loading text="Loading inventory..." />
          ) : filteredProducts.length === 0 ? (
            <div className="empty-state">
              <Package size={48} />
              <h3>No products found</h3>
              <p>
                {filter === 'low'
                  ? 'No products with low stock'
                  : filter === 'out'
                  ? 'No products out of stock'
                  : 'Add some products to manage inventory'}
              </p>
            </div>
          ) : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Quantity</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredProducts.map((product) => {
                    const status = getStockStatus(product.inventory?.quantityAvailable);
                    return (
                      <tr key={product.productId}>
                        <td>
                          <div className="product-info">
                            <span className="product-name">{product.productName}</span>
                            <span className="product-id">#{product.productId}</span>
                          </div>
                        </td>
                        <td>{product.categoryName || '-'}</td>
                        <td className="price">
                          ${product.price?.toFixed(2) || '0.00'}
                        </td>
                        <td>
                          <span className="quantity">{product.inventory?.quantityAvailable || 0}</span>
                        </td>
                        <td>
                          <span
                            className="status-badge"
                            style={{ backgroundColor: status.bg, color: status.color }}
                          >
                            {status.label}
                          </span>
                        </td>
                        <td>
                          <button
                            className="btn btn-sm btn-primary"
                            onClick={() => openUpdateModal(product)}
                          >
                            Update Stock
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Update Quantity Modal */}
      <Modal
        isOpen={updateModal.open}
        onClose={() => {
          setUpdateModal({ open: false, product: null });
          setQuantity('');
          setUpdateMode('set');
        }}
        title="Update Stock"
      >
        {updateModal.product && (
          <div className="update-form">
            <div className="product-header">
              <h4>{updateModal.product.productName}</h4>
              <p>Current stock: {updateModal.product.inventory?.quantityAvailable || 0}</p>
            </div>

            <div className="mode-tabs" style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
              <button
                className={`btn ${updateMode === 'set' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => {
                  setUpdateMode('set');
                  setQuantity('');
                }}
                disabled={updating}
              >
                Set Quantity
              </button>
              <button
                className={`btn ${updateMode === 'add' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => {
                  setUpdateMode('add');
                  setQuantity('');
                }}
                disabled={updating}
              >
                Add Stock
              </button>
            </div>

            <div className="quantity-input-group">
              <button
                className="qty-btn"
                onClick={() => setQuantity((prev) => Math.max(0, parseInt(prev || 0) - 1).toString())}
                disabled={updating}
              >
                <Minus size={18} />
              </button>
              <input
                type="number"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                min="0"
                className="quantity-input"
                disabled={updating}
                placeholder={updateMode === 'set' ? 'Enter new quantity' : 'Enter quantity to add'}
              />
              <button
                className="qty-btn"
                onClick={() => setQuantity((prev) => (parseInt(prev || 0) + 1).toString())}
                disabled={updating}
              >
                <Plus size={18} />
              </button>
            </div>

            <div className="modal-actions">
              <button
                className="btn btn-outline"
                onClick={() => {
                  setUpdateModal({ open: false, product: null });
                  setQuantity('');
                  setUpdateMode('set');
                }}
                disabled={updating}
              >
                Cancel
              </button>
              <button
                className="btn btn-primary"
                onClick={handleUpdateQuantity}
                disabled={updating || quantity === ''}
              >
                {updating ? 'Updating...' : 'Save Changes'}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default InventoryAdmin;
