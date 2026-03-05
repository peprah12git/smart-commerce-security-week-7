import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Package, Tag, ShoppingCart, Heart, Share2, Star } from 'lucide-react';
import Loading from '../../../components/Loading/Loading';
import ProductService from '../../../services/productService';
import CartService from '../../../services/cartService';
import ReviewService from '../../../services/reviewService';
import { useApp } from '../../../context/AppContext';
import './ProductDetail.css';

const ProductDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showNotification, user } = useApp();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [addingToCart, setAddingToCart] = useState(false);
  const [reviews, setReviews] = useState([]);
  const [showReviewForm, setShowReviewForm] = useState(false);
  const [reviewData, setReviewData] = useState({ rating: 5, comment: '' });

  const userId = user?.userId || user?.user_id;

  useEffect(() => {
    fetchProduct();
    fetchReviews();
  }, [id]);

  const fetchProduct = async () => {
    try {
      const data = await ProductService.getProductById(id);
      setProduct(data);
    } catch (error) {
      setError('Product not found');
    } finally {
      setLoading(false);
    }
  };

  const fetchReviews = async () => {
    try {
      const data = await ReviewService.getReviewsByProductId(id);
      setReviews(data);
    } catch (error) {
      console.error('Failed to fetch reviews:', error);
    }
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(price);
  };

  const handleAddToCart = async () => {
    if (!user) {
      showNotification('Please sign in to add items to cart', 'error');
      navigate('/login');
      return;
    }
    setAddingToCart(true);
    try {
      await CartService.addToCart(product.productId, quantity);
      showNotification(`Added ${quantity} ${product.productName} to cart!`, 'success');
    } catch (error) {
      console.error('Failed to add to cart:', error);
      showNotification(error.response?.data?.message || 'Failed to add item to cart', 'error');
    } finally {
      setAddingToCart(false);
    }
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (!user) {
      showNotification('Please sign in to leave a review', 'error');
      navigate('/login');
      return;
    }
    try {
      await ReviewService.createReview({
        userId,
        productId: product.productId,
        rating: reviewData.rating,
        comment: reviewData.comment
      });
      showNotification('Review submitted successfully!', 'success');
      setShowReviewForm(false);
      setReviewData({ rating: 5, comment: '' });
      fetchReviews();
    } catch (error) {
      showNotification('Failed to submit review', 'error');
    }
  };

  if (loading) {
    return <Loading text="Loading product..." />;
  }

  if (error || !product) {
    return (
      <div className="container">
        <div className="not-found">
          <Package size={64} />
          <h2>Product Not Found</h2>
          <p>The product you're looking for doesn't exist or has been removed.</p>
          <button className="btn btn-primary" onClick={() => navigate('/products')}>
            Browse Products
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="product-detail-page">
      <div className="container">
        <button className="back-btn" onClick={() => navigate(-1)}>
          <ArrowLeft size={20} />
          Back to Products
        </button>

        <div className="product-detail">
          <div className="product-image-section">
            <img
              src={`https://picsum.photos/seed/${product.productId}/600/600`}
              alt={product.productName}
            />
            {product.quantityAvailable === 0 && (
              <div className="out-of-stock-overlay">Out of Stock</div>
            )}
          </div>

          <div className="product-info-section">
            <Link to={`/products?category=${product.categoryName}`} className="product-category-link">
              <Tag size={16} />
              {product.categoryName}
            </Link>

            <h1>{product.productName}</h1>

            <div className="price-section">
              <span className="current-price">{formatPrice(product.price)}</span>
            </div>

            <p className="product-description-full">{product.description}</p>

            <div className="stock-info">
              <span className={`stock-status ${product.quantityAvailable > 0 ? 'in-stock' : 'out-of-stock'}`}>
                {product.quantityAvailable > 0 ? (
                  <>
                    <span className="stock-dot" />
                    {product.quantityAvailable} in stock
                  </>
                ) : (
                  'Out of Stock'
                )}
              </span>
            </div>

            {product.quantityAvailable > 0 && (
              <div className="quantity-section">
                <label>Quantity:</label>
                <div className="quantity-controls">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    disabled={quantity <= 1}
                  >
                    -
                  </button>
                  <span>{quantity}</span>
                  <button
                    onClick={() => setQuantity(Math.min(product.quantityAvailable, quantity + 1))}
                    disabled={quantity >= product.quantityAvailable}
                  >
                    +
                  </button>
                </div>
              </div>
            )}

            <div className="action-buttons">
              <button
                className="btn btn-primary btn-lg add-to-cart"
                disabled={product.quantityAvailable === 0 || addingToCart}
                onClick={handleAddToCart}
              >
                <ShoppingCart size={20} />
                {addingToCart ? 'Adding...' : 'Add to Cart'}
              </button>
              <button className="btn btn-outline icon-btn">
                <Heart size={20} />
              </button>
              <button className="btn btn-outline icon-btn">
                <Share2 size={20} />
              </button>
            </div>

            <div className="product-meta">
              <div className="meta-item">
                <span className="meta-label">Product ID:</span>
                <span className="meta-value">#{product.productId}</span>
              </div>
              <div className="meta-item">
                <span className="meta-label">Category:</span>
                <span className="meta-value">{product.categoryName}</span>
              </div>
            </div>
          </div>
        </div>

        <div className="reviews-section">
          <div className="reviews-header">
            <h2>Customer Reviews ({reviews.length})</h2>
            <button className="btn btn-primary" onClick={() => setShowReviewForm(!showReviewForm)}>
              Write a Review
            </button>
          </div>

          {showReviewForm && (
            <form className="review-form" onSubmit={handleSubmitReview}>
              <div className="form-group">
                <label>Rating</label>
                <div className="star-rating">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      size={24}
                      fill={star <= reviewData.rating ? '#fbbf24' : 'none'}
                      stroke={star <= reviewData.rating ? '#fbbf24' : '#d1d5db'}
                      onClick={() => setReviewData({ ...reviewData, rating: star })}
                      style={{ cursor: 'pointer' }}
                    />
                  ))}
                </div>
              </div>
              <div className="form-group">
                <label>Comment</label>
                <textarea
                  value={reviewData.comment}
                  onChange={(e) => setReviewData({ ...reviewData, comment: e.target.value })}
                  placeholder="Share your experience with this product"
                  rows="4"
                  required
                />
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-outline" onClick={() => setShowReviewForm(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Submit Review
                </button>
              </div>
            </form>
          )}

          <div className="reviews-list">
            {reviews.length === 0 ? (
              <p className="no-reviews">No reviews yet. Be the first to review this product!</p>
            ) : (
              reviews.map((review) => (
                <div key={review.reviewId} className="review-item">
                  <div className="review-header">
                    <div className="review-rating">
                      {[...Array(5)].map((_, i) => (
                        <Star
                          key={i}
                          size={16}
                          fill={i < review.rating ? '#fbbf24' : 'none'}
                          stroke={i < review.rating ? '#fbbf24' : '#d1d5db'}
                        />
                      ))}
                    </div>
                    <span className="review-date">
                      {new Date(review.reviewDate).toLocaleDateString()}
                    </span>
                  </div>
                  <p className="review-comment">{review.comment}</p>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductDetail;
