import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  UtensilsCrossed,
  Mail,
  Lock,
  LogIn,
  Loader2,
  AlertCircle,
  ChefHat,
  ShoppingBag,
  Clock,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import PasswordInput from '../components/PasswordInput';
import toast from 'react-hot-toast';
import './LoginPage.css';

export default function LoginPage() {
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors]     = useState({});
  const [loading, setLoading]   = useState(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  const validate = () => {
    const errs = {};
    if (!email.trim()) {
      errs.email = 'Email address is required';
    } else if (!/^\S+@\S+\.\S+$/.test(email)) {
      errs.email = 'Enter a valid email address';
    }
    if (!password) {
      errs.password = 'Password is required';
    } else if (password.length < 6) {
      errs.password = 'Password must be at least 6 characters';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleLoginSubmit = async (loginEmail, loginPassword) => {
    try {
      setLoading(true);
      const res = await login(loginEmail, loginPassword);
      toast.success(`Welcome back, ${res.user.fullName?.split(' ')[0]}!`);
      if (res.user.role === 'CANTEEN_ADMIN') {
        navigate('/admin/orders');
      } else if (res.user.role === 'SUB_ADMIN') {
        navigate('/subadmin/orders');
      } else if (res.user.role === 'SCHOOL_ADMIN') {
        navigate('/admin/users');
      } else {
        navigate('/menu');
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;
    handleLoginSubmit(email, password);
  };

  const handleQuickCanteenAdminLogin = () => {
    handleLoginSubmit('canteenadmin@school.local', 'Admin@12345');
  };

  return (
    <div className="login-container">
      {/* Left Branding Panel */}
      <div className="login-left">
        <div className="login-left-icon">
          <UtensilsCrossed size={40} color="white" />
        </div>
        <h2>TuckZone</h2>
        <p>Fresh, healthy meals delivered straight to your classroom — every school day.</p>

        <div style={{ marginTop: '2rem', display: 'flex', flexDirection: 'column', gap: '1rem', width: '100%', maxWidth: '260px' }}>
          {[
            { icon: ShoppingBag, text: 'Order food from the canteen' },
            { icon: Clock,       text: 'Track delivery in real-time' },
            { icon: ChefHat,     text: 'Fresh menu every day' },
          ].map(({ icon: Icon, text }) => (
            <div key={text} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'rgba(255,255,255,0.9)', fontSize: '0.875rem' }}>
              <div style={{ width: 32, height: 32, background: 'rgba(255,255,255,0.15)', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                <Icon size={16} />
              </div>
              {text}
            </div>
          ))}
        </div>
      </div>

      {/* Right Form Panel */}
      <div className="login-right">
        <div className="login-card-wrapper">
          <div className="login-header">
            <div className="brand-logo">
              <UtensilsCrossed size={28} className="text-amber" />
            </div>
            <h1>Sign In</h1>
            <p>Enter your credentials to access TuckZone</p>
          </div>

          <form className="login-form" onSubmit={handleSubmit} noValidate>
            <div className="form-group">
              <label htmlFor="login-email">Email Address</label>
              <div className="input-with-icon">
                <Mail className="input-icon" size={18} />
                <input
                  id="login-email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (errors.email) setErrors({ ...errors, email: null });
                  }}
                  className={errors.email ? 'input-error' : ''}
                  autoComplete="email"
                />
              </div>
              {errors.email && (
                <span className="field-error-text">
                  <AlertCircle size={13} /> {errors.email}
                </span>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="login-password">Password</label>
              <PasswordInput
                id="login-password"
                icon={Lock}
                iconSize={18}
                placeholder="••••••••"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (errors.password) setErrors({ ...errors, password: null });
                }}
                className={errors.password ? 'input-error' : ''}
                autoComplete="current-password"
              />
              {errors.password && (
                <span className="field-error-text">
                  <AlertCircle size={13} /> {errors.password}
                </span>
              )}
            </div>

            <div className="form-actions">
              <button type="submit" className="btn-primary login-btn" disabled={loading}>
                {loading ? <Loader2 className="spinner" size={18} /> : <LogIn size={18} />}
                <span>{loading ? 'Signing in...' : 'Sign In'}</span>
              </button>
            </div>
          </form>

          <div className="login-divider">OR</div>

          <button
            type="button"
            className="btn-admin-quick"
            onClick={handleQuickCanteenAdminLogin}
            disabled={loading}
          >
            <ChefHat size={18} />
            <span>Login as Canteen Admin</span>
          </button>

          <div className="login-footer">
            <p>
              Don&apos;t have an account?{' '}
              <Link to="/register" className="link-amber">Register here</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
