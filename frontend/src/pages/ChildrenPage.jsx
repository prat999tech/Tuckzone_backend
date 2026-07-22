import React, { useEffect, useState } from 'react';
import { Users, Link as LinkIcon, Trash2, GraduationCap, AlertCircle } from 'lucide-react';
import { getChildren, linkChild, unlinkChild } from '../api/parent';
import toast from 'react-hot-toast';
import './ChildrenPage.css';

export default function ChildrenPage() {
  const [children, setChildren] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errors, setErrors] = useState({});
  const [linkForm, setLinkForm] = useState({
    admissionNumber: '',
    parentMobile: '',
  });

  useEffect(() => {
    fetchChildren();
  }, []);

  const fetchChildren = async () => {
    try {
      const data = await getChildren();
      setChildren(data);
    } catch (err) {
      toast.error('Failed to load linked children');
    } finally {
      setLoading(false);
    }
  };

  const validate = () => {
    const errs = {};
    if (!linkForm.admissionNumber.trim()) {
      errs.admissionNumber = 'Admission number is required';
    }

    if (!linkForm.parentMobile.trim()) {
      errs.parentMobile = 'Parent mobile number is required';
    } else if (!/^\d{10}$/.test(linkForm.parentMobile.trim())) {
      errs.parentMobile = 'Parent mobile number must be exactly 10 digits';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleLink = async (e) => {
    e.preventDefault();
    if (!validate()) {
      toast.error('Please fix fields highlighted in red');
      return;
    }

    try {
      await linkChild(linkForm);
      toast.success('Child linked successfully');
      setLinkForm({ admissionNumber: '', parentMobile: '' });
      setErrors({});
      fetchChildren();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to link child');
    }
  };

  const handleUnlink = async (linkId) => {
    if (!window.confirm('Are you sure you want to unlink this child account?')) return;
    try {
      await unlinkChild(linkId);
      toast.success('Child unlinked successfully');
      fetchChildren();
    } catch (err) {
      toast.error('Failed to unlink child');
    }
  };

  return (
    <div className="children-container">
      <div className="page-header">
        <h1>Linked Children</h1>
        <p>Manage canteen access for your kids</p>
      </div>

      <div className="children-content">
        <div className="link-section">
          <div className="card-glass p-4">
            <h3><LinkIcon size={20} /> Link a Child</h3>
            <form onSubmit={handleLink} className="link-form" noValidate>
              <div className="form-group">
                <label>Admission Number</label>
                <input
                  type="text"
                  placeholder="e.g. ADM123"
                  value={linkForm.admissionNumber}
                  onChange={(e) => {
                    setLinkForm({ ...linkForm, admissionNumber: e.target.value });
                    if (errors.admissionNumber) setErrors({ ...errors, admissionNumber: null });
                  }}
                  className={errors.admissionNumber ? 'input-error' : ''}
                />
                {errors.admissionNumber && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {errors.admissionNumber}
                  </span>
                )}
              </div>
              <div className="form-group">
                <label>Registered Parent Mobile (10 digits)</label>
                <input
                  type="tel"
                  maxLength={10}
                  placeholder="10-digit mobile number"
                  value={linkForm.parentMobile}
                  onChange={(e) => {
                    setLinkForm({ ...linkForm, parentMobile: e.target.value });
                    if (errors.parentMobile) setErrors({ ...errors, parentMobile: null });
                  }}
                  className={errors.parentMobile ? 'input-error' : ''}
                />
                {errors.parentMobile && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {errors.parentMobile}
                  </span>
                )}
              </div>
              <button type="submit" className="btn-primary w-100">
                Link Account
              </button>
            </form>
          </div>
        </div>

        <div className="children-list-section">
          {loading ? (
            <div className="loading-state">Loading profiles...</div>
          ) : children.length === 0 ? (
            <div className="empty-state">
              <Users size={48} className="text-amber" />
              <h2>No children linked yet</h2>
              <p>Link your child&apos;s account to order food on their behalf.</p>
            </div>
          ) : (
            <div className="children-grid">
              {children.map((child) => (
                <div className="child-card" key={child.linkId}>
                  <div className="child-avatar">
                    <GraduationCap size={32} />
                  </div>
                  <div className="child-info">
                    <h3>{child.fullName}</h3>
                    <div className="badges">
                      <span className="badge badge-blue">Class: {child.studentClass}-{child.section}</span>
                      <span className="badge badge-gray">Roll: {child.rollNumber}</span>
                    </div>
                    <p className="admin-no">Adm No: {child.admissionNumber}</p>
                  </div>
                  <button
                    className="btn-icon-danger"
                    onClick={() => handleUnlink(child.linkId)}
                    title="Unlink child"
                  >
                    <Trash2 size={20} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
