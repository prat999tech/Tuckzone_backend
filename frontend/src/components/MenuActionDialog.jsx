import React from 'react';
import { Archive, Trash2 } from 'lucide-react';
import './MenuActionDialog.css';

/**
 * First step of deleting a menu item: choose Retire (soft, reversible) or Permanently
 * Delete (hard, asks for a second confirmation before it actually runs). Kept as its own
 * component since both DailyMenuPage and FixedMenuPage need the identical choice.
 */
export default function MenuActionDialog({ open, itemName, onRetire, onPermanentDelete, onCancel }) {
  if (!open) return null;

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-content menu-action-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>What would you like to do with this menu item?</h2>
        </div>
        <div className="modal-body">
          {itemName && <p className="menu-action-item-name">{itemName}</p>}
          <button type="button" className="menu-action-btn menu-action-retire" onClick={onRetire}>
            <Archive size={18} /> Retire
          </button>
          <button type="button" className="menu-action-btn menu-action-delete" onClick={onPermanentDelete}>
            <Trash2 size={18} /> Permanently Delete
          </button>
        </div>
        <div className="modal-actions menu-action-actions">
          <button type="button" className="btn-secondary" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
