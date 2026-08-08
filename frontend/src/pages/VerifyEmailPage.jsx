import React, { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { MailCheck, ShieldCheck, Loader2, AlertCircle } from 'lucide-react';
import { requestOtp, verifyEmail } from '../api/auth';
import { getConfig } from '../api/config';
import toast from 'react-hot-toast';
import './VerifyEmailPage.css';

/** Used only until the first "send code" response tells us the server's real policy. */
const DEFAULT_RESEND_COOLDOWN_SECONDS = 30;

/**
 * Second half of registration: the account exists but cannot sign in with a password
 * until the address is confirmed. Reached straight after signing up — mirrors
 * mobile/src/screens/auth/VerifyEmailScreen.tsx.
 */
export default function VerifyEmailPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const email = location.state?.email;

  const [otpLength, setOtpLength] = useState(6);
  const [code, setCode] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const cooldownRef = useRef(null);

  useEffect(() => {
    if (!email) {
      navigate('/register', { replace: true });
    }
  }, [email, navigate]);

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

  if (!email) {
    return null;
  }

  const handleVerify = async (e) => {
    e.preventDefault();
    if (!code || code.trim().length < 4) {
      setError('Enter the code from your email');
      return;
    }
    setError(null);
    try {
      setSubmitting(true);
      await verifyEmail(email, code.trim());
      toast.success('Email verified — you can sign in now.');
      navigate('/login', { replace: true });
    } catch (err) {
      toast.error(err.response?.data?.message || 'Verification failed');
    } finally {
      setSubmitting(false);
    }
  };

  const handleResend = async () => {
    try {
      setResending(true);
      const res = await requestOtp(email, 'EMAIL_VERIFICATION');
      startCooldown(res.resendAfterSeconds ?? DEFAULT_RESEND_COOLDOWN_SECONDS);
      if (res.devCode) {
        setCode(res.devCode);
        toast.success(`Dev mode — code: ${res.devCode}`);
      } else {
        toast.success('A new code is on its way.');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not resend the code.');
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="verify-container">
      <div className="verify-card-wrapper">
        <div className="verify-header">
          <div className="brand-logo">
            <MailCheck size={28} className="text-amber" />
          </div>
          <h1>Verify your email</h1>
          <p>
            We sent a {otpLength}-digit code to <strong>{email}</strong>
          </p>
        </div>

        <form className="verify-form" onSubmit={handleVerify} noValidate>
          <div className="form-group">
            <label htmlFor="verify-code">Verification Code</label>
            <div className="input-with-icon">
              <ShieldCheck className="input-icon" size={20} />
              <input
                id="verify-code"
                type="text"
                inputMode="numeric"
                placeholder={'0'.repeat(otpLength)}
                maxLength={otpLength}
                value={code}
                onChange={(e) => {
                  setCode(e.target.value.replace(/\D/g, ''));
                  if (error) setError(null);
                }}
                className={error ? 'input-error' : ''}
                autoComplete="one-time-code"
                autoFocus
              />
            </div>
            {error && (
              <span className="field-error-text">
                <AlertCircle size={14} /> {error}
              </span>
            )}
          </div>

          <div className="form-actions">
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? <Loader2 className="spinner" size={20} /> : <ShieldCheck size={20} />}
              <span>{submitting ? 'Verifying...' : 'Verify & continue'}</span>
            </button>
          </div>

          <button
            type="button"
            className="btn-resend"
            onClick={handleResend}
            disabled={cooldown > 0 || resending}
          >
            {resending ? 'Resending...' : cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
          </button>
        </form>

        <p className="verify-footnote">
          Wrong address?{' '}
          <Link to="/register" className="link-amber">Register again</Link> — the account
          is not usable until it is verified.
        </p>
      </div>
    </div>
  );
}
