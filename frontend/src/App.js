import React from 'react';
import { BrowserRouter as Router, Routes, Route, Outlet } from 'react-router-dom';
import { AppProvider } from './context/AppContext';

// Components
import Header from './components/Header/Header';
import Footer from './components/Footer/Footer';

// Unified Login
import UnifiedLogin from './pages/UnifiedLogin/UnifiedLogin';

// Client Pages
import Home from './pages/client/Home/Home';
import Products from './pages/client/Products/Products';
import ProductDetail from './pages/client/ProductDetail/ProductDetail';
import Categories from './pages/client/Categories/Categories';
import Register from './pages/client/Register/Register';
import Cart from './pages/client/Cart/Cart';
import Orders from './pages/client/Orders/Orders';
import OrderDetail from './pages/client/Orders/OrderDetail';
import Profile from './pages/client/Profile/Profile';

// Admin Pages
import AdminLayout from './pages/admin/AdminLayout/AdminLayout';
import AdminLogin from './pages/admin/AdminLogin/AdminLogin';
import ProtectedAdminRoute from './components/ProtectedAdminRoute';
import Dashboard from './pages/admin/Dashboard/Dashboard';
import ProductsAdmin from './pages/admin/Products/ProductsAdmin';
import ProductForm from './pages/admin/Products/ProductForm';
import CategoriesAdmin from './pages/admin/Categories/CategoriesAdmin';
import CategoryForm from './pages/admin/Categories/CategoryForm';
import OrdersAdmin from './pages/admin/Orders/OrdersAdmin';
import InventoryAdmin from './pages/admin/Inventory/InventoryAdmin';
import PerformanceReport from './pages/admin/Performance/PerformanceReport';

// Client Layout wrapper
const ClientLayout = () => (
  <>
    <Header />
    <Outlet />
    <Footer />
  </>
);

function App() {
  return (
    <AppProvider>
      <Router>
        <Routes>
          {/* Default Route - Login */}
          <Route path="/" element={<UnifiedLogin />} />
          <Route path="/login" element={<UnifiedLogin />} />
          
          {/* Client Routes */}
          <Route element={<ClientLayout />}>
            <Route path="/home" element={<Home />} />
            <Route path="/products" element={<Products />} />
            <Route path="/products/:id" element={<ProductDetail />} />
            <Route path="/categories" element={<Categories />} />
            <Route path="/register" element={<Register />} />
            <Route path="/cart" element={<Cart />} />
            <Route path="/orders" element={<Orders />} />
            <Route path="/orders/:orderId" element={<OrderDetail />} />
            <Route path="/profile" element={<Profile />} />
          </Route>

          {/* Admin Routes */}
          <Route path="/admin/login" element={<AdminLogin />} />
          <Route path="/admin" element={<ProtectedAdminRoute><AdminLayout /></ProtectedAdminRoute>}>
            <Route index element={<Dashboard />} />
            <Route path="products" element={<ProductsAdmin />} />
            <Route path="products/new" element={<ProductForm />} />
            <Route path="products/:id" element={<ProductForm />} />
            <Route path="categories" element={<CategoriesAdmin />} />
            <Route path="categories/new" element={<CategoryForm />} />
            <Route path="orders" element={<OrdersAdmin />} />
            <Route path="inventory" element={<InventoryAdmin />} />
            <Route path="performance" element={<PerformanceReport />} />
          </Route>
        </Routes>
      </Router>
    </AppProvider>
  );
}

export default App;
