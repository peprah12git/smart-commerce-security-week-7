import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, Filter, X, SlidersHorizontal } from 'lucide-react';
import ProductCard from '../../../components/ProductCard/ProductCard';
import Loading from '../../../components/Loading/Loading';
import ProductService from '../../../services/productService';
import { useApp } from '../../../context/AppContext';
import './Products.css';

const Products = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showFilters, setShowFilters] = useState(false);
  const { categories } = useApp();

  // Filter states
  const [filters, setFilters] = useState({
    sortBy: searchParams.get('sortBy') || 'productId',
    sortDirection: searchParams.get('sortDirection') || 'ASC',
    category: searchParams.get('category') || '',
    minPrice: searchParams.get('minPrice') || '',
    maxPrice: searchParams.get('maxPrice') || '',
    searchTerm: searchParams.get('searchTerm') || '',
    inStock: searchParams.get('inStock') || '',
  });

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        ...filters,
        minPrice: filters.minPrice ? parseFloat(filters.minPrice) : undefined,
        maxPrice: filters.maxPrice ? parseFloat(filters.maxPrice) : undefined,
        inStock: filters.inStock === 'true' ? true : filters.inStock === 'false' ? false : undefined,
      };

      const response = await ProductService.getProducts(params);
      setProducts(Array.isArray(response) ? response : []);
    } catch (error) {
      console.error('Failed to fetch products:', error);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  useEffect(() => {
    // Update URL with filters
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value && value !== 'productId' && value !== 'ASC') {
        params.set(key, value.toString());
      }
    });
    setSearchParams(params, { replace: true });
  }, [filters, setSearchParams]);

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const clearFilters = () => {
    setFilters({
      sortBy: 'productId',
      sortDirection: 'ASC',
      category: '',
      minPrice: '',
      maxPrice: '',
      searchTerm: '',
      inStock: '',
    });
  };

  const hasActiveFilters =
    filters.category || filters.minPrice || filters.maxPrice || filters.searchTerm || filters.inStock;

  return (
    <div className="products-page">
      <div className="container">
        <div className="products-header">
          <div>
            <h1>Products</h1>
            <p>{products.length} products found</p>
          </div>

          <div className="products-actions">
            <div className="search-box">
              <Search size={20} />
              <input
                type="text"
                placeholder="Search products..."
                value={filters.searchTerm}
                onChange={(e) => handleFilterChange('searchTerm', e.target.value)}
              />
              {filters.searchTerm && (
                <button onClick={() => handleFilterChange('searchTerm', '')}>
                  <X size={18} />
                </button>
              )}
            </div>

            <button className="filter-toggle" onClick={() => setShowFilters(!showFilters)}>
              <SlidersHorizontal size={20} />
              Filters
              {hasActiveFilters && <span className="filter-badge" />}
            </button>
          </div>
        </div>

        {showFilters && (
          <div className="filters-panel">
            <div className="filter-group">
              <label>Category</label>
              <select
                value={filters.category}
                onChange={(e) => handleFilterChange('category', e.target.value)}
              >
                <option value="">All Categories</option>
                {categories.map((cat) => (
                  <option key={cat.categoryId} value={cat.categoryName}>
                    {cat.categoryName}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-group">
              <label>Price Range</label>
              <div className="price-inputs">
                <input
                  type="number"
                  placeholder="Min"
                  value={filters.minPrice}
                  onChange={(e) => handleFilterChange('minPrice', e.target.value)}
                />
                <span>to</span>
                <input
                  type="number"
                  placeholder="Max"
                  value={filters.maxPrice}
                  onChange={(e) => handleFilterChange('maxPrice', e.target.value)}
                />
              </div>
            </div>

            <div className="filter-group">
              <label>Availability</label>
              <select
                value={filters.inStock}
                onChange={(e) => handleFilterChange('inStock', e.target.value)}
              >
                <option value="">All</option>
                <option value="true">In Stock</option>
                <option value="false">Out of Stock</option>
              </select>
            </div>

            <div className="filter-group">
              <label>Sort By</label>
              <select
                value={filters.sortBy}
                onChange={(e) => handleFilterChange('sortBy', e.target.value)}
              >
                <option value="productId">Default</option>
                <option value="productName">Name</option>
                <option value="price">Price</option>
                <option value="createdAt">Newest</option>
              </select>
            </div>

            <div className="filter-group">
              <label>Order</label>
              <select
                value={filters.sortDirection}
                onChange={(e) => handleFilterChange('sortDirection', e.target.value)}
              >
                <option value="ASC">Ascending</option>
                <option value="DESC">Descending</option>
              </select>
            </div>

            {hasActiveFilters && (
              <button className="btn btn-outline clear-filters" onClick={clearFilters}>
                <X size={18} />
                Clear Filters
              </button>
            )}
          </div>
        )}

        {loading ? (
          <Loading text="Loading products..." />
        ) : products.length === 0 ? (
          <div className="no-products">
            <Package size={64} />
            <h2>No products found</h2>
            <p>Try adjusting your filters or search term</p>
            {hasActiveFilters && (
              <button className="btn btn-primary" onClick={clearFilters}>
                Clear Filters
              </button>
            )}
          </div>
        ) : (
          <div className="products-grid">
            {products.map((product) => (
              <ProductCard key={product.productId} product={product} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

// Import Package icon
const Package = ({ size }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <path d="m16.5 9.4-9-5.19M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
    <polyline points="3.29 7 12 12 20.71 7" />
    <line x1="12" y1="22" x2="12" y2="12" />
  </svg>
);

export default Products;
