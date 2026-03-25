import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Package, Clock, CheckCircle, Truck, XCircle } from 'lucide-react';
import OrderService from '../../../services/orderService';
import Loading from '../../../components/Loading/Loading';
import { useApp } from '../../../context/AppContext';
import './Orders.css';

const Orders = () => {
  const { user, showNotification } = useApp();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [hasMore, setHasMore] = useState(true);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    if (user?.userId || user?.user_id) {
      // Reset pagination on user change and fetch first page
      setOrders([]);
      setCurrentPage(0);
      fetchOrdersPage(0);
    }
  }, [user]);

  const fetchOrdersPage = async (pageNum) => {
    try {
      pageNum === 0 ? setLoading(true) : setLoadingMore(true);
      const response = await OrderService.getOrdersByUserPaged(pageNum, pageSize);
      
      // response should be a PagedResponse with content, totalPages, etc.
      const newOrders = response.content || response;
      const respTotalPages = response.totalPages || 1;
      
      if (pageNum === 0) {
        // First page: replace
        setOrders(newOrders);
      } else {
        // Subsequent pages: append
        setOrders((prev) => [...prev, ...newOrders]);
      }
      
      setTotalPages(respTotalPages);
      setCurrentPage(pageNum);
      setHasMore(pageNum < respTotalPages - 1);
    } catch (error) {
      console.error('Failed to fetch orders:', error);
      showNotification('Failed to load orders', 'error');
    } finally {
      pageNum === 0 ? setLoading(false) : setLoadingMore(false);
    }
  };

  const loadMoreOrders = () => {
    if (hasMore && !loadingMore) {
      fetchOrdersPage(currentPage + 1);
    }
  };

  const getStatusIcon = (status) => {
    switch (status.toLowerCase()) {
      case 'confirmed':
        return <CheckCircle size={20} className="status-icon confirmed" />;
      case 'processing':
        return <Clock size={20} className="status-icon processing" />;
      case 'shipped':
        return <Truck size={20} className="status-icon shipped" />;
      case 'delivered':
        return <CheckCircle size={20} className="status-icon delivered" />;
      case 'cancelled':
        return <XCircle size={20} className="status-icon cancelled" />;
      default:
        return <Clock size={20} className="status-icon pending" />;
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(price);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  if (loading) {
    return (
      <div className="orders-page">
        <div className="container">
          <Loading text="Loading orders..." />
        </div>
      </div>
    );
  }

  return (
    <div className="orders-page">
      <div className="container">
        <div className="page-header">
          <h1>
            <Package size={28} />
            My Orders
          </h1>
        </div>

        {orders.length === 0 ? (
          <div className="no-orders">
            <Package size={64} />
            <h2>No orders yet</h2>
            <p>Start shopping to see your orders here</p>
            <Link to="/products" className="btn btn-primary">
              Browse Products
            </Link>
          </div>
        ) : (
          <div className="orders-list">
            {orders.map((order) => (
              <div key={order.orderId} className="order-card">
                <div className="order-header">
                  <div className="order-info">
                    <h3>Order #{order.orderId}</h3>
                    <span className="order-date">{formatDate(order.orderDate)}</span>
                  </div>
                  <div className="order-status">
                    {getStatusIcon(order.status)}
                    <span className={`status-text ${order.status.toLowerCase()}`}>
                      {order.status.charAt(0).toUpperCase() + order.status.slice(1)}
                    </span>
                  </div>
                </div>

                <div className="order-items">
                  {order.items.map((item) => (
                    <div key={item.orderItemId} className="order-item">
                      <div className="item-details">
                        <span className="item-name">{item.productName}</span>
                        <span className="item-quantity">Qty: {item.quantity}</span>
                      </div>
                      <span className="item-price">{formatPrice(item.unitPrice)}</span>
                    </div>
                  ))}
                </div>

                <div className="order-footer">
                  <div className="order-total">
                    <span>Total:</span>
                    <strong>{formatPrice(order.totalAmount)}</strong>
                  </div>
                  <Link to={`/orders/${order.orderId}`} className="btn btn-outline btn-sm">
                    View Details
                  </Link>
                </div>
              </div>
            ))}

            {hasMore && (
              <div className="pagination-controls">
                <button
                  onClick={loadMoreOrders}
                  disabled={loadingMore}
                  className="btn btn-primary"
                >
                  {loadingMore ? 'Loading...' : 'Load More Orders'}
                </button>
                <p className="pagination-info">
                  Showing {orders.length} of {totalPages * pageSize}+ orders
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Orders;
