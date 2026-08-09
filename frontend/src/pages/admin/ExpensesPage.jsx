import React, { useEffect, useState } from 'react';
import { Plus, Trash2, Receipt, X, AlertCircle } from 'lucide-react';
import { listExpenses, addExpense, deleteExpense } from '../../api/admin';
import { getConfig } from '../../api/config';
import ConfirmDialog from '../../components/ConfirmDialog';
import toast from 'react-hot-toast';
import './ExpensesPage.css';

const CATEGORIES = ['INGREDIENTS', 'STAFF_WAGES', 'GAS_FUEL', 'RENT', 'UTILITIES', 'PACKAGING', 'EQUIPMENT', 'OTHER'];

function isoDaysAgo(days) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().slice(0, 10);
}

const EMPTY_FORM = { expenseDate: isoDaysAgo(0), category: 'INGREDIENTS', description: '', amount: '' };

export default function ExpensesPage() {
  const [from, setFrom] = useState(isoDaysAgo(29));
  const [to, setTo] = useState(isoDaysAgo(0));
  const [currency, setCurrency] = useState('INR');
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [confirmExpense, setConfirmExpense] = useState(null);

  useEffect(() => {
    fetchExpenses();
  }, [from, to]);

  useEffect(() => {
    getConfig().then((c) => setCurrency(c.currency)).catch(() => undefined);
  }, []);

  const fetchExpenses = async () => {
    setLoading(true);
    try {
      const data = await listExpenses(from, to);
      setExpenses(data);
    } catch (err) {
      toast.error('Failed to load expenses');
    } finally {
      setLoading(false);
    }
  };

  const total = expenses.reduce((sum, e) => sum + Number(e.amount), 0);
  const money = (value) => `${currency} ${Number(value || 0).toFixed(2)}`;

  const openAdd = () => {
    setFormData(EMPTY_FORM);
    setErrors({});
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const amountNum = parseFloat(formData.amount);
    if (!formData.amount || Number.isNaN(amountNum) || amountNum <= 0) {
      setErrors({ amount: 'Enter a valid amount' });
      return;
    }
    setSaving(true);
    try {
      await addExpense({
        expenseDate: formData.expenseDate,
        category: formData.category,
        description: formData.description.trim() || undefined,
        amount: amountNum,
      });
      toast.success('Expense recorded');
      setModalOpen(false);
      fetchExpenses();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not save expense');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!confirmExpense) return;
    try {
      await deleteExpense(confirmExpense.id);
      setConfirmExpense(null);
      fetchExpenses();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not delete');
    }
  };

  return (
    <div className="expenses-container">
      <div className="page-header">
        <h1>Expenses</h1>
        <p>Track gas, wages, rent and supplies for accurate profit</p>
      </div>

      <div className="date-range-row">
        <div className="form-group">
          <label>From</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="form-group">
          <label>To</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>

      <div className="card total-card">
        <span className="total-label">Total in this period</span>
        <span className="total-value">{money(total)}</span>
      </div>

      <button type="button" className="btn-primary add-expense-btn" onClick={openAdd}>
        <Plus size={18} />
        <span>Record Expense</span>
      </button>

      {loading ? (
        <div className="loading-state">Loading expenses...</div>
      ) : expenses.length === 0 ? (
        <div className="empty-state">
          <Receipt size={48} className="text-amber" />
          <h2>No expenses recorded</h2>
          <p>Track gas, wages, rent and supplies for accurate profit.</p>
        </div>
      ) : (
        <div className="expenses-list">
          {expenses.map((expense) => (
            <div className="card expense-row" key={expense.id}>
              <div className="expense-info">
                <span className="expense-category">{expense.category.replace(/_/g, ' ')}</span>
                {expense.description && <span className="expense-desc">{expense.description}</span>}
                <span className="expense-date">{expense.expenseDate}</span>
              </div>
              <span className="expense-amount">{money(expense.amount)}</span>
              <button type="button" className="icon-btn-danger" onClick={() => setConfirmExpense(expense)}>
                <Trash2 size={16} />
              </button>
            </div>
          ))}
        </div>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Record Expense</h2>
              <button className="close-btn" onClick={() => setModalOpen(false)}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSave} noValidate>
              <div className="form-group">
                <label>Date</label>
                <input
                  type="date"
                  value={formData.expenseDate}
                  onChange={(e) => setFormData({ ...formData, expenseDate: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Category</label>
                <div className="category-pills">
                  {CATEGORIES.map((cat) => (
                    <button
                      key={cat}
                      type="button"
                      className={`pill-btn ${formData.category === cat ? 'active' : ''}`}
                      onClick={() => setFormData({ ...formData, category: cat })}
                    >
                      {cat.replace(/_/g, ' ')}
                    </button>
                  ))}
                </div>
              </div>

              <div className="form-group">
                <label>Description (optional)</label>
                <input
                  type="text"
                  placeholder="e.g. LPG cylinder refill"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label>Amount ({currency})</label>
                <input
                  type="number"
                  step="0.01"
                  value={formData.amount}
                  onChange={(e) => {
                    setFormData({ ...formData, amount: e.target.value });
                    if (errors.amount) setErrors({});
                  }}
                  className={errors.amount ? 'input-error' : ''}
                />
                {errors.amount && (
                  <span className="field-error-text"><AlertCircle size={14} /> {errors.amount}</span>
                )}
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? 'Saving...' : 'Save Expense'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={!!confirmExpense}
        title="Delete expense?"
        message={confirmExpense ? `Remove this ${confirmExpense.category.replace(/_/g, ' ').toLowerCase()} entry?` : ''}
        onConfirm={handleDelete}
        onCancel={() => setConfirmExpense(null)}
      />
    </div>
  );
}
