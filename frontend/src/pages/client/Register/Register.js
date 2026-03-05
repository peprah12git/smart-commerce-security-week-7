import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { User, Mail, Lock, Phone, MapPin } from 'lucide-react';
import UserService from '../../../services/userService';
import { useApp } from '../../../context/AppContext';
import './Register.css';

const Register = () => {
  const navigate = useNavigate();
  const { showNotification, setUser } = useApp();
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    phone: '',
    address: '',
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const validate = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Name is required';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Invalid email format';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validate()) return;

    setLoading(true);
    try {
      // POST /api/users returns LoginResponseDTO with auto-issued JWT
      const data = await UserService.register({
        name: formData.name,
        email: formData.email,
        password: formData.password,
        phone: formData.phone || null,
        address: formData.address || null,
      });

      const userObj = UserService.parseUserFromResponse(data);
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(userObj));
      setUser(userObj);
      showNotification(`Welcome, ${userObj.name}! Account created successfully.`, 'success');
      navigate('/home');
    } catch (error) {
      const message = error.response?.data?.message || 'Registration failed';
      setErrors({ submit: message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-page">
      <div className="container">
        <div className="register-card">
          <div className="register-header">
            <h1>Create Account</h1>
            <p>Join SmartCommerce today</p>
          </div>

          <form onSubmit={handleSubmit} className="register-form">
            {errors.submit && (
              <div className="alert alert-error">{errors.submit}</div>
            )}

            <div className="form-group">
              <label className="form-label">Full Name *</label>
              <div className="input-wrapper">
                <User size={18} />
                <input
                  type="text"
                  name="name"
                  className={`form-input ${errors.name ? 'error' : ''}`}
                  placeholder="John Doe"
                  value={formData.name}
                  onChange={handleChange}
                />
              </div>
              {errors.name && <p className="error-message">{errors.name}</p>}
            </div>

            <div className="form-group">
              <label className="form-label">Email *</label>
              <div className="input-wrapper">
                <Mail size={18} />
                <input
                  type="email"
                  name="email"
                  className={`form-input ${errors.email ? 'error' : ''}`}
                  placeholder="john@example.com"
                  value={formData.email}
                  onChange={handleChange}
                />
              </div>
              {errors.email && <p className="error-message">{errors.email}</p>}
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Password *</label>
                <div className="input-wrapper">
                  <Lock size={18} />
                  <input
                    type="password"
                    name="password"
                    className={`form-input ${errors.password ? 'error' : ''}`}
                    placeholder="••••••••"
                    value={formData.password}
                    onChange={handleChange}
                  />
                </div>
                {errors.password && <p className="error-message">{errors.password}</p>}
              </div>

              <div className="form-group">
                <label className="form-label">Confirm Password *</label>
                <div className="input-wrapper">
                  <Lock size={18} />
                  <input
                    type="password"
                    name="confirmPassword"
                    className={`form-input ${errors.confirmPassword ? 'error' : ''}`}
                    placeholder="••••••••"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                  />
                </div>
                {errors.confirmPassword && <p className="error-message">{errors.confirmPassword}</p>}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Phone (optional)</label>
              <div className="input-wrapper">
                <Phone size={18} />
                <input
                  type="tel"
                  name="phone"
                  className="form-input"
                  placeholder="+1 (555) 000-0000"
                  value={formData.phone}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Address (optional)</label>
              <div className="input-wrapper">
                <MapPin size={18} />
                <input
                  type="text"
                  name="address"
                  className="form-input"
                  placeholder="123 Main St, City, Country"
                  value={formData.address}
                  onChange={handleChange}
                />
              </div>
            </div>

            <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={loading}>
              {loading ? 'Creating Account...' : 'Create Account'}
            </button>

            <p className="login-link">
              Already have an account? <Link to="/login">Sign in</Link>
            </p>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Register;
