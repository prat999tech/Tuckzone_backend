import React, { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { KeyRound, Mail, Lock, ShieldCheck, Loader2, AlertCircle } from 'lucide-react';
import { requestOtp, resetPassword } from '../api/auth';
import { getConfig } from '../api/config';
import PasswordInput from '../components/PasswordInput';
import toast from 'react-hot-toast';
import './ForgotPasswordPage.css';

const DEFAULT_RESEND_COOLDOWN_SECONDS = 30;

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [otpLength, setOtpLength] = useState(6);

  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [errors, setErrors] = useState({});
  const [sendingCode, setSendingCode] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const cooldownRef = useRef(null);

  useEffect(() => {
    getConfig().then((c) => setOtpLength(c.otpLength)).catch(() => undefined);
  }, []);

  useEffect(() => () => clearInterval(cooldownRef.current), []);

  const startCooldown = (seconds) => {
    clearInterval(cooldownRef.current);
    setCooldown(seconds);
    cooldownRef.current = setInterval(() => {
      setCooldown((s) => {
        if (s <= 1) {
          clearInterval(cooldownRef.current);
          return 0;
        }
        return s - 1;
      });
    }, 1000);
  };

  const handleSendCode = async (e) => {
    e.preventDefault();
    if (!email.trim() || !/^\S+@\S+\.\S+$/.test(email)) {
      setErrors({ email: 'Enter a valid email address' });
      return;
    }
    setErrors({});
    try {
      setSendingCode(true);
      const res = await requestOtp(email.trim(), 'PASSWORD_RESET');
      setCodeSent(true);
      startCooldown(res.resendAfterSeconds ?? DEFAULT_RESEND_COOLDOWN_SECONDS);
      if (res.devCode) {
        setCode(res.devCode);
        toast.success(`Dev mode — code: ${res.devCode}`);
      } else {
        toast.success('Check your inbox for the code.');
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Could not send the code. Try again.');
    } finally {
      setSendingCode(false);
    }
  };

  const handleResend = async () => {
    try {
      setSendingCode(true);
      const res = await requestOtp(email.trim(), 'PASSWORD_RESET');
      startCooldown(res.resendAfterSeconds ?? DEFAULT_RESEND_COOLDOWN_SECONDS);
      toast.success('A new code is on its way.');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Could not resend the code.');
    } finally {
      setSendingCode(false);
    }
  };

  const handleReset = async (e) => {
    e.preventDefault();
    const errs = {};
    if (!code || code.trim().length < 4) errs.code = 'Enter the code from your email';
    if (!newPassword || newPassword.length < 8) errs.newPassword = 'Password must be at least 8 characters';
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    try {
      setSubmitting(true);
      await resetPassword(email.trim(), code.trim(), newPassword);
      toast.success('Password updated — please sign in with your new password.');
      navigate('/login');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Could not reset password');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="forgot-container">
      <div className="forgot-card-wrapper">
        <div className="forgot-header">
          <div className="brand-logo">
            <KeyRound size={28} className="text-amber" />
          </div>
          <h1>Forgot your password?</h1>
          <p>We&apos;ll email a one-time code to your registered address.</p>
        </div>

        {!codeSent ? (
          <form className="forgot-form" onSubmit={handleSendCode} noValidate>
            <div className="form-group">
              <label htmlFor="fp-email">Email Address</label>
              <div className="input-with-icon">
                <Mail className="input-icon" size={18} />
                <input
                  id="fp-email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (errors.email) setErrors({});
                  }}
                  className={errors.email ? 'input-error' : ''}
                  autoComplete="email"
                />
              </div>
              {errors.email && (
                <span className="field-error-text"><AlertCircle size={13} /> {errors.email}</span>
              )}
            </div>

            <button type="submit" className="btn-primary" disabled={sendingCode}>
              {sendingCode ? <Loader2 className="spinner" size={18} /> : <ShieldCheck size={18} />}
              <span>{sendingCode ? 'Sending code...' : 'Send Code'}</span>
            </button>
          </form>
        ) : (
          <form className="forgot-form" onSubmit={handleReset} noValidate>
            <p className="code-sent-to">
              Code sent to <strong>{email}</strong>
            </p>

            <div className="form-group">
              <label htmlFor="fp-code">{otpLength}-digit code</label>
              <div className="input-with-icon">
                <ShieldCheck className="input-icon" size={18} />
                <input
                  id="fp-code"
                  type="text"
                  inputMode="numeric"
                  placeholder={'0'.repeat(otpLength)}
                  maxLength={otpLength}
                  value={code}
                  onChange={(e) => {
                    setCode(e.target.value.replace(/\D/g, ''));
                    if (errors.code) setErrors({ ...errors, code: null });
                  }}
                  className={errors.code ? 'input-error' : ''}
                  autoComplete="one-time-code"
                />
              </div>
              {errors.code && (
                <span className="field-error-text"><AlertCircle size={13} /> {errors.code}</span>
              )}
            </div>

            <div className="form-group">
              <label>New Password</label>
              <PasswordInput
                icon={Lock}
                iconSize={18}
                placeholder="••••••••"
                value={newPassword}
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  if (errors.newPassword) setErrors({ ...errors, newPassword: null });
                }}
                className={errors.newPassword ? 'input-error' : ''}
                autoComplete="new-password"
              />
              {errors.newPassword && (
                <span className="field-error-text"><AlertCircle size={13} /> {errors.newPassword}</span>
              )}
            </div>

            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? <Loader2 className="spinner" size={18} /> : <KeyRound size={18} />}
              <span>{submitting ? 'Resetting...' : 'Reset Password'}</span>
            </button>

            <button
              type="button"
              className="btn-resend"
              onClick={handleResend}
              disabled={cooldown > 0 || sendingCode}
            >
              {sendingCode ? 'Resending...' : cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
            </button>
          </form>
        )}

        <p className="forgot-footnote">
          <Link to="/login" className="link-amber">Back to Sign In</Link>
        </p>
      </div>
    </div>
  );
}
