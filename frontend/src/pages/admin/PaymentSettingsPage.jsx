import React, { useEffect, useState } from 'react';
import { Edit2, X, AlertCircle } from 'lucide-react';
import { getPlatformFeeSettings, updatePlatformFeeSettings } from '../../api/payments';
import toast from 'react-hot-toast';
import './PaymentSettingsPage.css';

const USE_CASE_LABELS = {
  WALLET_RECHARGE: 'Wallet Recharge',
  CHECKOUT: 'Checkout',
  SUBSCRIPTION: 'Subscription (not yet available)',
};

const EMPTY_FORM = { enabled: false, feeType: 'PERCENTAGE', feeValue: '0', minFee: '', maxFee: '' };

export default function PaymentSettingsPage() {
  const [settings, setSettings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingUseCase, setEditingUseCase] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    fetchSettings();
  }, []);

  const fetchSettings = async () => {
    setLoading(true);
    try {
      const data = await getPlatformFeeSettings();
      setSettings(data);
    } catch (err) {
      toast.error('Failed to load payment settings');
    } finally {
      setLoading(false);
    }
  };

  const openEdit = (row) => {
    setErrors({});
    setEditingUseCase(row.useCase);
    setFormData({
      enabled: row.enabled,
      feeType: row.feeType,
      feeValue: String(row.feeValue ?? '0'),
      minFee: row.minFee != null ? String(row.minFee) : '',
      maxFee: row.maxFee != null ? String(row.maxFee) : '',
    });
  };

  const validate = () => {
    const errs = {};
    const value = Number(formData.feeValue);
    if (formData.feeValue === '' || isNaN(value) || value < 0) {
      errs.feeValue = 'Enter a fee value of 0 or more';
    }
    if (formData.feeType === 'PERCENTAGE' && value > 100) {
      errs.feeValue = 'A percentage fee cannot exceed 100';
    }
    if (formData.minFee !== '' && formData.maxFee !== '' && Number(formData.minFee) > Number(formData.maxFee)) {
      errs.maxFee = 'Maximum fee must be at least the minimum fee';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) {
      toast.error('Please fix the highlighted fields');
      return;
    }
    try {
      await updatePlatformFeeSettings(editingUseCase, {
        enabled: formData.enabled,
        feeType: formData.feeType,
        feeValue: formData.feeValue,
        minFee: formData.minFee === '' ? null : formData.minFee,
        maxFee: formData.maxFee === '' ? null : formData.maxFee,
      });
      toast.success('Payment settings updated');
      setEditingUseCase(null);
      fetchSettings();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Update failed');
    }
  };

  const describeFee = (row) => {
    if (!row.enabled) return 'Disabled';
    const amount = row.feeType === 'PERCENTAGE' ? `${row.feeValue}%` : `₹${row.feeValue}`;
    const bounds = [];
    if (row.minFee != null) bounds.push(`min ₹${row.minFee}`);
    if (row.maxFee != null) bounds.push(`max ₹${row.maxFee}`);
    return bounds.length ? `${amount} (${bounds.join(', ')})` : amount;
  };

  return (
    <div className="admin-container">
      <div className="page-header">
        <h1>Payment Settings</h1>
        <p>
          Configure the platform fee — your revenue on top of wallet recharges and order
          checkouts. Disabled by default; nothing changes for customers until you enable it here.
        </p>
      </div>

      {loading ? (
        <div className="loading-state">Loading payment settings...</div>
      ) : (
        <div className="users-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Use Case</th>
                <th>Status</th>
                <th>Fee</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {settings.map((row) => (
                <tr key={row.useCase}>
                  <td><div className="user-name">{USE_CASE_LABELS[row.useCase] || row.useCase}</div></td>
                  <td>
                    <span className={`badge ${row.enabled ? 'badge-success' : 'badge-error'}`}>
                      {row.enabled ? 'Enabled' : 'Disabled'}
                    </span>
                  </td>
                  <td>{describeFee(row)}</td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-icon"
                        onClick={() => openEdit(row)}
                        title="Edit"
                        disabled={row.useCase === 'SUBSCRIPTION'}
                      >
                        <Edit2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {editingUseCase && (
        <div className="modal-overlay" onClick={() => setEditingUseCase(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{USE_CASE_LABELS[editingUseCase] || editingUseCase} Fee</h2>
              <button className="close-btn" onClick={() => setEditingUseCase(null)}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSubmit} noValidate>
              <div className="form-group form-row-inline">
                <label className="toggle-label">
                  <input
                    type="checkbox"
                    checked={formData.enabled}
                    onChange={(e) => setFormData({ ...formData, enabled: e.target.checked })}
                  />
                  Enable platform fee for this use case
                </label>
              </div>

              <div className="form-group">
                <label>Fee Type</label>
                <select
                  value={formData.feeType}
                  onChange={(e) => setFormData({ ...formData, feeType: e.target.value })}
                >
                  <option value="PERCENTAGE">Percentage</option>
                  <option value="FIXED">Fixed Amount</option>
                </select>
              </div>

              <div className="form-group">
                <label>{formData.feeType === 'PERCENTAGE' ? 'Fee Percentage (%)' : 'Fee Amount (₹)'}</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={formData.feeValue}
                  onChange={(e) => {
                    setFormData({ ...formData, feeValue: e.target.value });
                    if (errors.feeValue) setErrors({ ...errors, feeValue: null });
                  }}
                  className={errors.feeValue ? 'input-error' : ''}
                />
                {errors.feeValue && <span className="field-error-text"><AlertCircle size={14} /> {errors.feeValue}</span>}
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Minimum Fee (₹, optional)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={formData.minFee}
                    onChange={(e) => setFormData({ ...formData, minFee: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Maximum Fee (₹, optional)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={formData.maxFee}
                    onChange={(e) => {
                      setFormData({ ...formData, maxFee: e.target.value });
                      if (errors.maxFee) setErrors({ ...errors, maxFee: null });
                    }}
                    className={errors.maxFee ? 'input-error' : ''}
                  />
                  {errors.maxFee && <span className="field-error-text"><AlertCircle size={14} /> {errors.maxFee}</span>}
                </div>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setEditingUseCase(null)}>Cancel</button>
                <button type="submit" className="btn-primary">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
