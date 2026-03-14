import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Mail, Lock, LogIn, Eye, EyeOff, Shield, User } from 'lucide-react';
import UserService from '../../services/userService';
import { useApp } from '../../context/AppContext';
import './UnifiedLogin.css';

const GOOGLE_OAUTH_URL = 'http://localhost:8080/oauth2/authorization/google';

const UnifiedLogin = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { showNotification, setUser } = useApp();
  const [loginType, setLoginType] = useState('client');
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // Handle OAuth2 redirects using HttpOnly cookie-based auth.
  useEffect(() => {
    const oauthError = searchParams.get('error');
    if (oauthError) {
      setErrors((prev) => ({ ...prev, submit: oauthError }));
      showNotification('Google sign-in failed', 'error');
      return;
    }

    const oauthStatus = searchParams.get('oauth');
    if (oauthStatus !== 'success') {
      return;
    }

    const hydrateSessionFromCookie = async () => {
      try {
        const userObj = await UserService.getMyProfile();
        localStorage.setItem('user', JSON.stringify(userObj));
        setUser(userObj);
        showNotification(`Welcome, ${userObj.name || userObj.email}!`, 'success');
        navigate('/home', { replace: true });
      } catch (error) {
        const message = error.response?.data?.message || 'OAuth login succeeded but session could not be loaded';
        setErrors((prev) => ({ ...prev, submit: message }));
        showNotification('Sign-in completed, but session setup failed', 'error');
      }
    };

    hydrateSessionFromCookie();
  }, [searchParams, navigate, setUser, showNotification]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Invalid email format';
    }
    if (!formData.password) {
      newErrors.password = 'Password is required';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    try {
      // Both admin and client use the same /api/auth/login endpoint.
      // The brute-force protection (rate limiting, lockout) lives there.
      const data = await UserService.login(formData.email, formData.password);

      // Backend returns a flat LoginResponseDTO: {userId, name, email, role, token, message}
      const userObj = UserService.parseUserFromResponse(data);

      if (loginType === 'admin') {
        if (userObj.role !== 'ADMIN') {
          throw new Error('Access denied. Admin privileges required.');
        }
        localStorage.setItem('adminToken', data.token);
        localStorage.setItem('adminUser', JSON.stringify(userObj));
        setUser(userObj);
        showNotification('Admin login successful', 'success');
        navigate('/admin');
      } else {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify(userObj));
        setUser(userObj);
        showNotification(`Welcome back, ${userObj.name}!`, 'success');
        navigate('/home');
      }
    } catch (error) {
      const message = error.response?.data?.message || error.message || 'Login failed';
      setErrors({ submit: message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="unified-login-page">
      <div className="container">
        <div className="unified-login-card">
          <div className="login-header">
            <LogIn size={48} />
            <h1>Welcome to SmartCommerce</h1>
            <p>Sign in to continue</p>
          </div>

          <div className="login-type-selector">
            <button
              type="button"
              className={`type-btn ${loginType === 'client' ? 'active' : ''}`}
              onClick={() => setLoginType('client')}
            >
              <User size={20} />
              Client Login
            </button>
            <button
              type="button"
              className={`type-btn ${loginType === 'admin' ? 'active' : ''}`}
              onClick={() => setLoginType('admin')}
            >
              <Shield size={20} />
              Admin Login
            </button>
          </div>

          <form onSubmit={handleSubmit} className="login-form">
            <div className="form-group">
              <label htmlFor="email">
                <Mail size={16} />
                Email Address
              </label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder={loginType === 'admin' ? 'admin@test.com' : 'Enter your email'}
                className={errors.email ? 'error' : ''}
                disabled={loading}
              />
              {errors.email && <span className="error-message">{errors.email}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="password">
                <Lock size={16} />
                Password
              </label>
              <div className="password-input">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Enter your password"
                  className={errors.password ? 'error' : ''}
                  disabled={loading}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  disabled={loading}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {errors.password && <span className="error-message">{errors.password}</span>}
            </div>

            {errors.submit && (
              <div className="error-message submit-error">
                {errors.submit}
              </div>
            )}

            <button 
              type="submit" 
              className="btn btn-primary btn-block"
              disabled={loading}
            >
              {loading ? 'Signing In...' : 'Sign In'}
            </button>
          </form>

          {loginType === 'client' && (
            <div className="login-footer">
              <p className="register-link">
                Don't have an account? <Link to="/register">Sign up</Link>
              </p>

              <div className="oauth-divider">
                <span>or</span>
              </div>

              <a
                href={GOOGLE_OAUTH_URL}
                className="btn-google"
                aria-label="Sign in with Google"
              >
                <svg className="google-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
                Continue with Google
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UnifiedLogin;
