import React, { useEffect, useState } from 'react';
import { Plus, Edit2, Trash2, Power, X, AlertCircle } from 'lucide-react';
import {
  activateSubAdmin,
  createSubAdmin,
  deactivateSubAdmin,
  deleteSubAdmin,
  listSubAdmins,
  updateSubAdmin,
} from '../../api/admin';
import PasswordInput from '../../components/PasswordInput';
import toast from 'react-hot-toast';
import './SubAdminsPage.css';

const EMPTY_FORM = { fullName: '', email: '', mobile: '', password: '' };

export default function SubAdminsPage() {
  const [subAdmins, setSubAdmins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [errors, setErrors] = useState({});
  const [formData, setFormData] = useState(EMPTY_FORM);

  useEffect(() => {
    fetchSubAdmins();
  }, []);

  const fetchSubAdmins = async () => {
    setLoading(true);
    try {
      const data = await listSubAdmins();
      setSubAdmins(data);
    } catch (err) {
      toast.error('Failed to load sub admins');
    } finally {
      setLoading(false);
    }
  };

  const validate = () => {
    const errs = {};
    if (!formData.fullName.trim()) errs.fullName = 'Name is required';
    if (!/^\S+@\S+\.\S+$/.test(formData.email)) errs.email = 'Enter a valid email address';
    if (!/^[6-9]\d{9}$/.test(formData.mobile)) errs.mobile = 'Enter a valid 10-digit mobile number';
    if (!editingId && (!formData.password || formData.password.length < 8)) {
      errs.password = 'Password must be at least 8 characters';
    }
    if (editingId && formData.password && formData.password.length < 8) {
      errs.password = 'Password must be at least 8 characters';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) {
      toast.error('Please fix errors highlighted in red');
      return;
    }
    try {
      if (editingId) {
        const payload = { fullName: formData.fullName, email: formData.email, mobile: formData.mobile };
        if (formData.password) payload.newPassword = formData.password;
        await updateSubAdmin(editingId, payload);
        toast.success('Sub admin updated');
      } else {
        await createSubAdmin(formData);
        toast.success('Sub admin created');
      }
      setIsModalOpen(false);
      setErrors({});
      fetchSubAdmins();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const handleToggleStatus = async (subAdmin) => {
    try {
      if (subAdmin.status === 'ACTIVE') {
        await deactivateSubAdmin(subAdmin.id);
        toast.success('Sub admin deactivated');
      } else {
        await activateSubAdmin(subAdmin.id);
        toast.success('Sub admin activated');
      }
      fetchSubAdmins();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update status');
    }
  };

  const handleDelete = async (subAdmin) => {
    if (!window.confirm(`Permanently delete ${subAdmin.fullName}'s sub admin account?`)) return;
    try {
      await deleteSubAdmin(subAdmin.id);
      toast.success('Sub admin deleted');
      fetchSubAdmins();
    } catch (err) {
      toast.error('Delete failed');
    }
  };

  const openModal = (subAdmin = null) => {
    setErrors({});
    if (subAdmin) {
      setEditingId(subAdmin.id);
      setFormData({ fullName: subAdmin.fullName, email: subAdmin.email, mobile: subAdmin.mobile, password: '' });
    } else {
      setEditingId(null);
      setFormData(EMPTY_FORM);
    }
    setIsModalOpen(true);
  };

  return (
    <div className="admin-container">
      <div className="page-header flex-between">
        <div>
          <h1>Manage Sub Admins</h1>
          <p>Create restricted staff accounts for order handling and menu upkeep.</p>
        </div>
        <button className="btn-primary" onClick={() => openModal()}>
          <Plus size={18} /> Add Sub Admin
        </button>
      </div>

      {loading ? (
        <div className="loading-state">Loading sub admins...</div>
      ) : subAdmins.length === 0 ? (
        <div className="empty-state">No sub admins yet. Add one to delegate day-to-day order handling.</div>
      ) : (
        <div className="users-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Mobile</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {subAdmins.map((sa) => (
                <tr key={sa.id}>
                  <td><div className="user-name">{sa.fullName}</div></td>
                  <td>{sa.email}</td>
                  <td>{sa.mobile}</td>
                  <td>
                    <span className={`badge ${sa.status === 'ACTIVE' ? 'badge-success' : 'badge-error'}`}>
                      {sa.status}
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button className="btn-icon" onClick={() => openModal(sa)} title="Edit">
                        <Edit2 size={16} />
                      </button>
                      <button
                        className="btn-icon"
                        onClick={() => handleToggleStatus(sa)}
                        title={sa.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                      >
                        <Power size={16} />
                      </button>
                      <button className="btn-icon-danger" onClick={() => handleDelete(sa)} title="Delete">
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingId ? 'Edit Sub Admin' : 'Add New Sub Admin'}</h2>
              <button className="close-btn" onClick={() => setIsModalOpen(false)}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSubmit} noValidate>
              <div className="form-group">
                <label>Full Name</label>
                <input
                  type="text"
                  value={formData.fullName}
                  onChange={(e) => {
                    setFormData({ ...formData, fullName: e.target.value });
                    if (errors.fullName) setErrors({ ...errors, fullName: null });
                  }}
                  className={errors.fullName ? 'input-error' : ''}
                />
                {errors.fullName && <span className="field-error-text"><AlertCircle size={14} /> {errors.fullName}</span>}
              </div>

              <div className="form-group">
                <label>Email Address</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => {
                    setFormData({ ...formData, email: e.target.value });
                    if (errors.email) setErrors({ ...errors, email: null });
                  }}
                  className={errors.email ? 'input-error' : ''}
                />
                {errors.email && <span className="field-error-text"><AlertCircle size={14} /> {errors.email}</span>}
              </div>

              <div className="form-group">
                <label>Mobile Number</label>
                <input
                  type="tel"
                  value={formData.mobile}
                  onChange={(e) => {
                    setFormData({ ...formData, mobile: e.target.value });
                    if (errors.mobile) setErrors({ ...errors, mobile: null });
                  }}
                  className={errors.mobile ? 'input-error' : ''}
                />
                {errors.mobile && <span className="field-error-text"><AlertCircle size={14} /> {errors.mobile}</span>}
              </div>

              <div className="form-group">
                <label>{editingId ? 'New Password (leave blank to keep current)' : 'Password'}</label>
                <PasswordInput
                  value={formData.password}
                  onChange={(e) => {
                    setFormData({ ...formData, password: e.target.value });
                    if (errors.password) setErrors({ ...errors, password: null });
                  }}
                  className={errors.password ? 'input-error' : ''}
                />
                {errors.password && <span className="field-error-text"><AlertCircle size={14} /> {errors.password}</span>}
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setIsModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn-primary">{editingId ? 'Save Changes' : 'Create Sub Admin'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
