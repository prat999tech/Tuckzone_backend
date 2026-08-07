import React from 'react';

/**
 * Consistent "nothing here yet" panel — icon, title, optional message and optional
 * action — instead of a plain unstyled string. Reuses the `.empty-state` rules already
 * defined in index.css, so it drops into any page without extra CSS.
 */
export default function EmptyState({ icon: Icon, title, message, action }) {
  return (
    <div className="empty-state">
      {Icon && <Icon size={40} strokeWidth={1.5} />}
      <h2>{title}</h2>
      {message && <p>{message}</p>}
      {action}
    </div>
  );
}
