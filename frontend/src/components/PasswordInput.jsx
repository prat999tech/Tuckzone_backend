import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import './PasswordInput.css';

/**
 * Drop-in replacement for a plain `<input type="password">`. Renders masked by
 * default; a right-side icon button toggles it to plain text and back. The
 * toggle only ever flips the `type` attribute — the value, onChange, name,
 * autoComplete, etc. all pass straight through untouched, so nothing about
 * how the password is captured, validated or submitted changes.
 */
export default function PasswordInput({
  icon: Icon,
  iconSize = 18,
  showIcon = Boolean(Icon),
  className = '',
  wrapperClassName = '',
  ...inputProps
}) {
  const [visible, setVisible] = useState(false);

  return (
    <div className={`password-field ${showIcon ? 'has-left-icon' : ''} ${wrapperClassName}`.trim()}>
      {showIcon && Icon && <Icon className="input-icon" size={iconSize} />}
      <input {...inputProps} type={visible ? 'text' : 'password'} className={className} />
      <button
        type="button"
        className="password-toggle-btn"
        onClick={() => setVisible((prev) => !prev)}
        aria-label={visible ? 'Hide password' : 'Show password'}
        aria-pressed={visible}
      >
        {visible ? <EyeOff size={18} /> : <Eye size={18} />}
      </button>
    </div>
  );
}
