import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Edit, Trash2, Search, Package } from 'lucide-react';
import ProductService from '../../../services/productService';
import Modal from '../../../components/Modal/Modal';
import Loading from '../../../components/Loading/Loading';
import { useApp } from '../../../context/AppContext';
import './ProductsAdmin.css';

const ProductsAdmin = () => {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [deleteModal, setDeleteModal] = useState({ open: false, product: null });
  const [deleting, setDeleting] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const { showNotification } = useApp();

  useEffect(() => {
    fetchProducts();
  }, [searchTerm]);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const response = await ProductService.getProducts({
        searchTerm: searchTerm || undefined,
      });
      setProducts(Array.isArray(response) ? response : []);
    } catch (error) {
      console.error('Failed to fetch products:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteModal.product) return;

    setDeleting(true);
    try {
      await ProductService.deleteProduct(deleteModal.product.productId);
      showNotification('Product deleted successfully', 'success');
      setDeleteModal({ open: false, product: null });
      fetchProducts();
    } catch (error) {
      showNotification('Failed to delete product', 'error');
    } finally {
      setDeleting(false);
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(price);
  };

  return (
    <div className="products-admin">
      <div className="page-header">
        <h1>Products</h1>
        <Link to="/admin/products/new" className="btn btn-primary">
          <Plus size={18} />
          Add Product
        </Link>
      </div>

      <div className="card">
        <div className="card-header">
          <div className="search-box">
            <Search size={18} />
            <input
              type="text"
              placeholder="Search products..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>
        <div className="card-body">
          {loading ? (
            <Loading text="Loading products..." />
          ) : products.length === 0 ? (
            <div className="empty-state">
              <Package size={48} />
              <h3>No products found</h3>
              <p>Get started by adding your first product</p>
              <Link to="/admin/products/new" className="btn btn-primary">
                Add Product
              </Link>
            </div>
          ) : (
            <>
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Product Name</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Stock</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((product) => (
                    <tr key={product.productId}>
                      <td>#{product.productId}</td>
                      <td>
                        <div className="product-name-cell">
                          <img
                            src={`https://picsum.photos/seed/${product.productId}/40/40`}
                            alt={product.productName}
                          />
                          <span>{product.productName}</span>
                        </div>
                      </td>
                      <td>{product.categoryName}</td>
                      <td>{formatPrice(product.price)}</td>
                      <td>
                        <span
                          className={`badge ${
                            product.quantityAvailable > 10
                              ? 'badge-success'
                              : product.quantityAvailable > 0
                              ? 'badge-warning'
                              : 'badge-danger'
                          }`}
                        >
                          {product.quantityAvailable}
                        </span>
                      </td>
                      <td>
                        <div className="action-buttons">
                          <button
                            className="btn btn-outline btn-sm"
                            onClick={() => navigate(`/admin/products/${product.productId}`)}
                          >
                            <Edit size={16} />
                          </button>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => setDeleteModal({ open: true, product })}
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
        </div>
      </div>

      <Modal
        isOpen={deleteModal.open}
        onClose={() => setDeleteModal({ open: false, product: null })}
        title="Delete Product"
        footer={
          <>
            <button
              className="btn btn-outline"
              onClick={() => setDeleteModal({ open: false, product: null })}
            >
              Cancel
            </button>
            <button className="btn btn-danger" onClick={handleDelete} disabled={deleting}>
              {deleting ? 'Deleting...' : 'Delete'}
            </button>
          </>
        }
      >
        <p>
          Are you sure you want to delete <strong>{deleteModal.product?.productName}</strong>?
          This action cannot be undone.
        </p>
      </Modal>
    </div>
  );
};

export default ProductsAdmin;
