import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Package, Tag, Users, TrendingUp, ChevronLeft, ChevronRight } from 'lucide-react';
import ProductCard from '../../../components/ProductCard/ProductCard';
import Loading from '../../../components/Loading/Loading';
import ProductService from '../../../services/productService';
import { useApp } from '../../../context/AppContext';
import './Home.css';

// ── Hero Slider data ─────────────────────────────────────────────────────────
const SLIDES = [
  {
    id: 1,
    gradient: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
    tag: '🛍️ New Arrivals',
    title: 'Welcome to SmartCommerce',
    subtitle: 'Discover amazing products at unbeatable prices. Shop the latest trends and enjoy fast delivery.',
    primaryCta: { label: 'Shop Now', to: '/products' },
    secondaryCta: { label: 'Browse Categories', to: '/categories' },
    visual: (
      <svg viewBox="0 0 220 220" fill="none" xmlns="http://www.w3.org/2000/svg" className="slide-visual">
        <circle cx="110" cy="110" r="100" fill="rgba(255,255,255,0.08)" />
        <circle cx="110" cy="110" r="70"  fill="rgba(255,255,255,0.08)" />
        <rect x="65" y="85" width="90" height="75" rx="8" fill="rgba(255,255,255,0.25)" />
        <path d="M85 85 Q85 65 110 65 Q135 65 135 85" stroke="white" strokeWidth="5" fill="none" strokeLinecap="round"/>
        <circle cx="88" cy="168" r="8" fill="white" opacity="0.8"/>
        <circle cx="132" cy="168" r="8" fill="white" opacity="0.8"/>
        <path d="M95 115 L105 125 L125 105" stroke="white" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    ),
  },
  {
    id: 2,
    gradient: 'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)',
    tag: '🔥 Hot Deals',
    title: 'Unbeatable Deals Every Day',
    subtitle: 'Save big on thousands of items. Limited-time offers updated daily — don\'t miss out.',
    primaryCta: { label: 'See Deals', to: '/products' },
    secondaryCta: { label: 'View Categories', to: '/categories' },
    visual: (
      <svg viewBox="0 0 220 220" fill="none" xmlns="http://www.w3.org/2000/svg" className="slide-visual">
        <circle cx="110" cy="110" r="100" fill="rgba(255,255,255,0.08)" />
        <circle cx="110" cy="110" r="70"  fill="rgba(255,255,255,0.08)" />
        <path d="M110 55 C110 55 85 95 85 115 C85 128 96 138 110 138 C124 138 135 128 135 115 C135 95 110 55 110 55Z" fill="rgba(255,255,255,0.3)"/>
        <path d="M100 118 C100 118 104 108 110 108 C116 108 118 120 118 120" stroke="white" strokeWidth="4" fill="none" strokeLinecap="round"/>
        <text x="78" y="175" fill="white" fontSize="22" fontWeight="bold" opacity="0.85">50% OFF</text>
      </svg>
    ),
  },
  {
    id: 3,
    gradient: 'linear-gradient(135deg, #10b981 0%, #0891b2 100%)',
    tag: '📦 Fast & Free Delivery',
    title: 'Free Shipping on All Orders',
    subtitle: 'Get your favourite products delivered quickly and safely, straight to your doorstep.',
    primaryCta: { label: 'Order Now', to: '/products' },
    secondaryCta: { label: 'Browse Products', to: '/products' },
    visual: (
      <svg viewBox="0 0 220 220" fill="none" xmlns="http://www.w3.org/2000/svg" className="slide-visual">
        <circle cx="110" cy="110" r="100" fill="rgba(255,255,255,0.08)" />
        <circle cx="110" cy="110" r="70"  fill="rgba(255,255,255,0.08)" />
        <rect x="55" y="95" width="80" height="50" rx="5" fill="rgba(255,255,255,0.25)"/>
        <path d="M135 110 L165 110 L165 145 L135 145 Z" fill="rgba(255,255,255,0.2)"/>
        <path d="M155 110 L165 125 L165 110 Z" fill="rgba(255,255,255,0.4)"/>
        <circle cx="80"  cy="152" r="9" fill="white" opacity="0.8"/>
        <circle cx="148" cy="152" r="9" fill="white" opacity="0.8"/>
        <path d="M60 95 L80 70 L135 70 L135 95" stroke="white" strokeWidth="4" fill="none" strokeLinejoin="round"/>
      </svg>
    ),
  },
  {
    id: 4,
    gradient: 'linear-gradient(135deg, #6366f1 0%, #ec4899 100%)',
    tag: '⭐ Top Rated',
    title: 'Trusted by 1,000+ Customers',
    subtitle: 'Premium quality products, verified reviews, and 24/7 customer support at your service.',
    primaryCta: { label: 'Shop Top Picks', to: '/products' },
    secondaryCta: { label: 'View All', to: '/products' },
    visual: (
      <svg viewBox="0 0 220 220" fill="none" xmlns="http://www.w3.org/2000/svg" className="slide-visual">
        <circle cx="110" cy="110" r="100" fill="rgba(255,255,255,0.08)" />
        <circle cx="110" cy="110" r="70"  fill="rgba(255,255,255,0.08)" />
        {[0,1,2,3,4].map(i => (
          <polygon key={i}
            points="110,72 116,90 135,90 121,101 126,120 110,109 94,120 99,101 85,90 104,90"
            transform={`rotate(${i * 6 - 12} 110 110) translate(${(i-2)*28} ${i===2?0:18})`}
            fill="white" opacity={i === 2 ? 0.9 : 0.4}
          />
        ))}
        <circle cx="110" cy="155" r="20" fill="rgba(255,255,255,0.2)"/>
        <path d="M102 155 L108 161 L120 149" stroke="white" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    ),
  },
];

// ── HeroSlider component ─────────────────────────────────────────────────────
const HeroSlider = () => {
  const [current, setCurrent] = useState(0);
  const [animating, setAnimating] = useState(false);
  const total = SLIDES.length;

  const goTo = useCallback((index) => {
    if (animating) return;
    setAnimating(true);
    setCurrent((index + total) % total);
    setTimeout(() => setAnimating(false), 600);
  }, [animating, total]);

  const next = useCallback(() => goTo(current + 1), [current, goTo]);
  const prev = useCallback(() => goTo(current - 1), [current, goTo]);

  // Auto-advance every 5 seconds
  useEffect(() => {
    const timer = setInterval(next, 5000);
    return () => clearInterval(timer);
  }, [next]);

  return (
    <section className="hero-slider">
      {/* Slides track */}
      <div className="slides-track" style={{ transform: `translateX(-${current * 100}%)` }}>
        {SLIDES.map((slide) => (
          <div
            key={slide.id}
            className="slide"
            style={{ background: slide.gradient }}
          >
            <div className="container">
              <div className="slide-content">
                <div className="slide-text">
                  <span className="slide-tag">{slide.tag}</span>
                  <h1>{slide.title}</h1>
                  <p>{slide.subtitle}</p>
                  <div className="hero-actions">
                    <Link to={slide.primaryCta.to} className="btn btn-white btn-lg">
                      {slide.primaryCta.label} <ArrowRight size={20} />
                    </Link>
                    <Link to={slide.secondaryCta.to} className="btn btn-outline-white btn-lg">
                      {slide.secondaryCta.label}
                    </Link>
                  </div>
                </div>
                <div className="slide-visual-wrap">
                  {slide.visual}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Prev / Next arrows */}
      <button className="slider-arrow slider-arrow--prev" onClick={prev} aria-label="Previous slide">
        <ChevronLeft size={28} />
      </button>
      <button className="slider-arrow slider-arrow--next" onClick={next} aria-label="Next slide">
        <ChevronRight size={28} />
      </button>

      {/* Dot indicators */}
      <div className="slider-dots">
        {SLIDES.map((_, i) => (
          <button
            key={i}
            className={`slider-dot${i === current ? ' active' : ''}`}
            onClick={() => goTo(i)}
            aria-label={`Go to slide ${i + 1}`}
          />
        ))}
      </div>
    </section>
  );
};

// ── Home page ────────────────────────────────────────────────────────────────
const Home = () => {
  const [featuredProducts, setFeaturedProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const { categories } = useApp();

  useEffect(() => {
    fetchFeaturedProducts();
  }, []);

  const fetchFeaturedProducts = async () => {
    try {
      const response = await ProductService.getProducts({ size: 8 });
      setFeaturedProducts(response.content || []);
    } catch (error) {
      console.error('Failed to fetch products:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="home">
      {/* Hero Slider */}
      <HeroSlider />

      {/* Stats Section */}
      <section className="stats">
        <div className="container">
          <div className="stats-grid">
            <div className="stat-card">
              <Package size={32} />
              <div>
                <h3>{featuredProducts.length}+</h3>
                <p>Products</p>
              </div>
            </div>
            <div className="stat-card">
              <Tag size={32} />
              <div>
                <h3>{categories.length}</h3>
                <p>Categories</p>
              </div>
            </div>
            <div className="stat-card">
              <Users size={32} />
              <div>
                <h3>1000+</h3>
                <p>Happy Customers</p>
              </div>
            </div>
            <div className="stat-card">
              <TrendingUp size={32} />
              <div>
                <h3>24/7</h3>
                <p>Support</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Categories Section */}
      <section className="categories-section">
        <div className="container">
          <div className="section-header">
            <h2>Shop by Category</h2>
            <Link to="/categories" className="view-all">
              View All <ArrowRight size={18} />
            </Link>
          </div>
          <div className="categories-grid">
            {categories.slice(0, 6).map((category) => (
              <Link
                key={category.categoryId}
                to={`/products?category=${category.categoryName}`}
                className="category-card"
              >
                <div className="category-icon">
                  <Tag size={24} />
                </div>
                <h3>{category.categoryName}</h3>
                <p>{category.description}</p>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Featured Products Section */}
      <section className="featured-section">
        <div className="container">
          <div className="section-header">
            <h2>Featured Products</h2>
            <Link to="/products" className="view-all">
              View All <ArrowRight size={18} />
            </Link>
          </div>
          {loading ? (
            <Loading text="Loading products..." />
          ) : (
            <div className="products-grid">
              {featuredProducts.map((product) => (
                <ProductCard key={product.productId} product={product} />
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
};

export default Home;

