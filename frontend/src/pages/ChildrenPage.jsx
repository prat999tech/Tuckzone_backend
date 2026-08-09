import React, { useEffect, useState } from 'react';
import { Users, UserPlus, Pencil, Trash2, GraduationCap, AlertCircle } from 'lucide-react';
import { listWards, createWard, updateWard, deleteWard } from '../api/wards';
import toast from 'react-hot-toast';
import './ChildrenPage.css';

const EMPTY_FORM = { name: '', studentClass: '', section: '' };

export default function ChildrenPage() {
  const [wards, setWards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errors, setErrors] = useState({});
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingWardId, setEditingWardId] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchWards();
  }, []);

  const fetchWards = async () => {
    try {
      const data = await listWards();
      setWards(data);
    } catch (err) {
      toast.error('Failed to load your wards');
    } finally {
      setLoading(false);
    }
  };

  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = 'Ward name is required';
    if (!form.studentClass.trim()) errs.studentClass = 'Class is required';
    if (!form.section.trim()) errs.section = 'Section is required';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const startEdit = (ward) => {
    setEditingWardId(ward.id);
    setForm({ name: ward.name, studentClass: ward.studentClass, section: ward.section });
    setErrors({});
  };

  const cancelEdit = () => {
    setEditingWardId(null);
    setForm(EMPTY_FORM);
    setErrors({});
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) {
      toast.error('Please fix fields highlighted in red');
      return;
    }

    const payload = {
      name: form.name.trim(),
      studentClass: form.studentClass.trim(),
      section: form.section.trim(),
    };

    setSaving(true);
    try {
      if (editingWardId) {
        await updateWard(editingWardId, payload);
        toast.success('Ward updated');
      } else {
        await createWard(payload);
        toast.success('Ward added');
      }
      cancelEdit();
      fetchWards();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save ward');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (ward) => {
    if (!window.confirm(`Remove ${ward.name} from your wards?`)) return;
    try {
      await deleteWard(ward.id);
      toast.success('Ward removed');
      if (editingWardId === ward.id) cancelEdit();
      fetchWards();
    } catch (err) {
      toast.error('Failed to remove ward');
    }
  };

  return (
    <div className="children-container">
      <div className="page-header">
        <h1>My Wards</h1>
        <p>Add the children you order food for</p>
      </div>

      <div className="children-content">
        <div className="link-section">
          <div className="card-glass p-4">
            <h3><UserPlus size={20} /> {editingWardId ? 'Edit Ward' : 'Add Your Ward'}</h3>
            <form onSubmit={handleSubmit} className="link-form" noValidate>
              <div className="form-group">
                <label>Ward Name</label>
                <input
                  type="text"
                  placeholder="e.g. Aarav Sharma"
                  value={form.name}
                  onChange={(e) => {
                    setForm({ ...form, name: e.target.value });
                    if (errors.name) setErrors({ ...errors, name: null });
                  }}
                  className={errors.name ? 'input-error' : ''}
                />
                {errors.name && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {errors.name}
                  </span>
                )}
              </div>
              <div className="form-group">
                <label>Class</label>
                <input
                  type="text"
                  placeholder="e.g. VIII"
                  value={form.studentClass}
                  onChange={(e) => {
                    setForm({ ...form, studentClass: e.target.value });
                    if (errors.studentClass) setErrors({ ...errors, studentClass: null });
                  }}
                  className={errors.studentClass ? 'input-error' : ''}
                />
                {errors.studentClass && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {errors.studentClass}
                  </span>
                )}
              </div>
              <div className="form-group">
                <label>Section</label>
                <input
                  type="text"
                  placeholder="e.g. B"
                  value={form.section}
                  onChange={(e) => {
                    setForm({ ...form, section: e.target.value });
                    if (errors.section) setErrors({ ...errors, section: null });
                  }}
                  className={errors.section ? 'input-error' : ''}
                />
                {errors.section && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {errors.section}
                  </span>
                )}
              </div>
              <button type="submit" className="btn-primary w-100" disabled={saving}>
                {editingWardId ? 'Save Changes' : 'Add Ward'}
              </button>
              {editingWardId && (
                <button type="button" className="btn-secondary w-100" onClick={cancelEdit}>
                  Cancel
                </button>
              )}
            </form>
          </div>
        </div>

        <div className="children-list-section">
          {loading ? (
            <div className="loading-state">Loading wards...</div>
          ) : wards.length === 0 ? (
            <div className="empty-state">
              <Users size={48} className="text-amber" />
              <h2>No wards added yet</h2>
              <p>Add a ward to order food on their behalf.</p>
            </div>
          ) : (
            <div className="children-grid">
              {wards.map((ward) => (
                <div className="child-card" key={ward.id}>
                  <div className="child-avatar">
                    <GraduationCap size={32} />
                  </div>
                  <div className="child-info">
                    <h3>{ward.name}</h3>
                    <div className="badges">
                      <span className="badge badge-blue">Class: {ward.studentClass}-{ward.section}</span>
                    </div>
                  </div>
                  <div className="action-buttons">
                    <button
                      className="btn-icon"
                      onClick={() => startEdit(ward)}
                      title="Edit ward"
                    >
                      <Pencil size={18} />
                    </button>
                    <button
                      className="btn-icon-danger"
                      onClick={() => handleDelete(ward)}
                      title="Remove ward"
                    >
                      <Trash2 size={18} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
