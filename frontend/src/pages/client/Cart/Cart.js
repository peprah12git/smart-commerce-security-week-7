import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, Trash2, Plus, Minus, ShoppingBag, ArrowRight } from 'lucide-react';
import CartService from '../../../services/cartService';
import OrderService from '../../../services/orderService';
import Loading from '../../../components/Loading/Loading';
import Modal from '../../../components/Modal/Modal';
import { useApp } from '../../../context/AppContext';
import './Cart.css';

const Cart = () => {
  const navigate = useNavigate();
  const { showNotification, user } = useApp();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [checkoutModal, setCheckoutModal] = useState(false);
  const [checkingOut, setCheckingOut] = useState(false);

  const userId = user?.userId || user?.user_id;

  useEffect(() => {
    fetchCart();
  }, []);

  const fetchCart = async () => {
    setLoading(true);
    try {
      const data = await CartService.getCart();
      setCart(data);
    } catch (error) {
      console.error('Failed to fetch cart:', error);
      showNotification('Failed to load cart', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateQuantity = async (productId, newQuantity) => {
    if (newQuantity < 1) return;
    
    setUpdating(true);
    try {
      await CartService.updateQuantity(productId, newQuantity);
      await fetchCart();
      showNotification('Cart updated', 'success');
    } catch (error) {
      console.error('Failed to update quantity:', error);
      showNotification(error.response?.data?.message || 'Failed to update cart', 'error');
    } finally {
      setUpdating(false);
    }
  };

  const handleRemoveItem = async (productId) => {
    setUpdating(true);
    try {
      await CartService.removeFromCart(productId);
      await fetchCart();
      showNotification('Item removed from cart', 'success');
    } catch (error) {
      console.error('Failed to remove item:', error);
      showNotification('Failed to remove item', 'error');
    } finally {
      setUpdating(false);
    }
  };

  const handleClearCart = async () => {
    setUpdating(true);
    try {
      await CartService.clearCart();
      await fetchCart();
      showNotification('Cart cleared', 'success');
    } catch (error) {
      console.error('Failed to clear cart:', error);
      showNotification('Failed to clear cart', 'error');
    } finally {
      setUpdating(false);
    }
  };

  const handleCheckout = async () => {
    if (!user) {
      showNotification('Please sign in to proceed to checkout', 'error');
      navigate('/login');
      return;
    }
    setCheckingOut(true);
    try {
      const order = await OrderService.checkoutFromCart(userId);
      setCheckoutModal(false);
      showNotification('Order placed successfully!', 'success');
      await fetchCart();
      navigate(`/orders/${order.orderId}`);
    } catch (error) {
      console.error('Failed to checkout:', error);
      showNotification(error.response?.data?.message || 'Failed to place order', 'error');
    } finally {
      setCheckingOut(false);
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(price);
  };

  if (loading) {
    return (
      <div className="cart-page">
        <div className="container">
          <Loading text="Loading cart..." />
        </div>
      </div>
    );
  }

  const isEmpty = !cart || !cart.items || cart.items.length === 0;

  return (
    <div className="cart-page">
      <div className="container">
        <div className="page-header">
          <h1>
            <ShoppingCart size={28} />
            Shopping Cart
          </h1>
          {!isEmpty && (
            <button 
              className="btn btn-outline btn-sm"
              onClick={handleClearCart}
              disabled={updating}
            >
              Clear Cart
            </button>
          )}
        </div>

        {isEmpty ? (
          <div className="empty-cart">
            <ShoppingBag size={64} />
            <h2>Your cart is empty</h2>
            <p>Looks like you haven't added anything to your cart yet.</p>
            <Link to="/products" className="btn btn-primary">
              Start Shopping <ArrowRight size={18} />
            </Link>
          </div>
        ) : (
          <div className="cart-content">
            <div className="cart-items">
              {cart.items.map((item) => (
                <div key={item.cartItemId} className="cart-item">
                  <div className="item-image">
                    <ShoppingBag size={48} />
                  </div>
                  <div className="item-details">
                    <Link to={`/products/${item.productId}`} className="item-name">
                      {item.productName}
                    </Link>
                    <p className="item-price">{formatPrice(item.productPrice)}</p>
                  </div>
                  <div className="item-quantity">
                    <button
                      className="qty-btn"
                      onClick={() => handleUpdateQuantity(item.productId, item.quantity - 1)}
                      disabled={updating || item.quantity <= 1}
                    >
                      <Minus size={16} />
                    </button>
                    <span className="qty-value">{item.quantity}</span>
                    <button
                      className="qty-btn"
                      onClick={() => handleUpdateQuantity(item.productId, item.quantity + 1)}
                      disabled={updating}
                    >
                      <Plus size={16} />
                    </button>
                  </div>
                  <div className="item-subtotal">
                    {formatPrice(item.subtotal)}
                  </div>
                  <button
                    className="remove-btn"
                    onClick={() => handleRemoveItem(item.productId)}
                    disabled={updating}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              ))}
            </div>

            <div className="cart-summary">
              <h3>Order Summary</h3>
              <div className="summary-row">
                <span>Items ({cart.itemCount})</span>
                <span>{formatPrice(cart.totalAmount)}</span>
              </div>
              <div className="summary-row">
                <span>Shipping</span>
                <span>Free</span>
              </div>
              <div className="summary-total">
                <span>Total</span>
                <span>{formatPrice(cart.totalAmount)}</span>
              </div>
              <button
                className="btn btn-primary btn-block"
                onClick={() => {
                  if (!user) {
                    showNotification('Please sign in to proceed to checkout', 'error');
                    navigate('/login');
                  } else {
                    setCheckoutModal(true);
                  }
                }}
                disabled={updating}
              >
                Proceed to Checkout
              </button>
            </div>
          </div>
        )}
      </div>

      <Modal
        isOpen={checkoutModal}
        onClose={() => setCheckoutModal(false)}
        title="Confirm Order"
      >
        <div className="checkout-modal">
          <p>Are you sure you want to place this order?</p>
          <div className="order-summary">
            <span>Total: {cart && formatPrice(cart.totalAmount)}</span>
          </div>
          <div className="modal-actions">
            <button
              className="btn btn-outline"
              onClick={() => setCheckoutModal(false)}
              disabled={checkingOut}
            >
              Cancel
            </button>
            <button
              className="btn btn-primary"
              onClick={handleCheckout}
              disabled={checkingOut}
            >
              {checkingOut ? 'Placing Order...' : 'Confirm Order'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default Cart;
