import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  User,
  Mail,
  Lock,
  Phone,
  Hash,
  BookOpen,
  Layers,
  Users,
  Loader2,
  UserPlus,
  AlertCircle,
} from 'lucide-react';
import { registerParent, registerStudent, registerTeacher } from '../api/auth';
import toast from 'react-hot-toast';
import './RegisterPage.css';

export default function RegisterPage() {
  const [role, setRole] = useState('STUDENT');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    mobile: '',
    password: '',
    admissionNumber: '',
    studentClass: '',
    section: '',
    rollNumber: '',
    parentMobile: '',
    studentMobile: '',
    employeeId: '',
    department: '',
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    if (errors[name]) {
      setErrors({ ...errors, [name]: null });
    }
  };

  const validate = () => {
    const errs = {};

    if (!formData.fullName.trim()) {
      errs.fullName = 'Full name is required';
    } else if (formData.fullName.trim().length < 2) {
      errs.fullName = 'Name must be at least 2 characters';
    }

    if (!formData.email.trim()) {
      errs.email = 'Email address is required';
    } else if (!/^\S+@\S+\.\S+$/.test(formData.email)) {
      errs.email = 'Enter a valid email address';
    }

    // Mobile Number validation: must be digits only and exactly 10 digits
    if (!formData.mobile.trim()) {
      errs.mobile = 'Mobile number is required';
    } else if (!/^\d{10}$/.test(formData.mobile.trim())) {
      errs.mobile = 'Mobile number must be exactly 10 digits';
    }

    if (!formData.password) {
      errs.password = 'Password is required';
    } else if (formData.password.length < 6) {
      errs.password = 'Password must be at least 6 characters';
    }

    if (role === 'STUDENT') {
      if (!formData.admissionNumber.trim()) {
        errs.admissionNumber = 'Admission number is required';
      }

      if (!formData.parentMobile.trim()) {
        errs.parentMobile = "Parent's mobile number is required";
      } else if (!/^\d{10}$/.test(formData.parentMobile.trim())) {
        errs.parentMobile = "Parent's mobile number must be exactly 10 digits";
      }

      if (formData.studentMobile.trim() && !/^\d{10}$/.test(formData.studentMobile.trim())) {
        errs.studentMobile = 'Student mobile number must be exactly 10 digits';
      }

      if (!formData.studentClass.trim()) {
        errs.studentClass = 'Class is required';
      }

      if (!formData.section.trim()) {
        errs.section = 'Section is required';
      }

      if (!formData.rollNumber.trim()) {
        errs.rollNumber = 'Roll number is required';
      } else if (!/^\d+$/.test(formData.rollNumber.trim())) {
        errs.rollNumber = 'Roll number must be numeric';
      }
    }

    if (role === 'TEACHER') {
      if (!formData.employeeId.trim()) {
        errs.employeeId = 'Employee ID is required';
      }
      if (!formData.department.trim()) {
        errs.department = 'Department is required';
      }
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) {
      toast.error('Please fix all fields highlighted in red');
      return;
    }

    try {
      setLoading(true);

      if (role === 'STUDENT') {
        await registerStudent({
          fullName: formData.fullName,
          email: formData.email,
          mobile: formData.mobile,
          password: formData.password,
          admissionNumber: formData.admissionNumber,
          studentClass: formData.studentClass,
          section: formData.section,
          rollNumber: formData.rollNumber,
          parentMobile: formData.parentMobile,
          studentMobile: formData.studentMobile || '',
        });
      } else if (role === 'TEACHER') {
        await registerTeacher({
          fullName: formData.fullName,
          email: formData.email,
          mobile: formData.mobile,
          password: formData.password,
          employeeId: formData.employeeId,
          department: formData.department,
        });
      } else {
        await registerParent({
          fullName: formData.fullName,
          email: formData.email,
          mobile: formData.mobile,
          password: formData.password,
        });
      }

      toast.success('Registration successful! You can now sign in.');
      navigate('/login');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-container">
      <div className="register-card-wrapper">
        <div className="register-header">
          <div className="brand-logo">
            <UserPlus size={28} className="text-amber" />
          </div>
          <h1>Create Account</h1>
          <p>Fill in your details to register with TuckZone</p>
        </div>

        <div className="role-tabs">
          <button
            className={`role-tab ${role === 'STUDENT' ? 'active' : ''}`}
            onClick={() => {
              setRole('STUDENT');
              setErrors({});
            }}
            type="button"
          >
            Student
          </button>
          <button
            className={`role-tab ${role === 'TEACHER' ? 'active' : ''}`}
            onClick={() => {
              setRole('TEACHER');
              setErrors({});
            }}
            type="button"
          >
            Teacher
          </button>
          <button
            className={`role-tab ${role === 'PARENT' ? 'active' : ''}`}
            onClick={() => {
              setRole('PARENT');
              setErrors({});
            }}
            type="button"
          >
            Parent
          </button>
        </div>

        <form className="register-form" onSubmit={handleSubmit} noValidate>
          <div className="form-row">
            <div className="form-group">
              <label>Full Name</label>
              <div className="input-with-icon">
                <User className="input-icon" size={20} />
                <input
                  type="text"
                  name="fullName"
                  placeholder="John Doe"
                  value={formData.fullName}
                  onChange={handleChange}
                  className={errors.fullName ? 'input-error' : ''}
                />
              </div>
              {errors.fullName && (
                <span className="field-error-text">
                  <AlertCircle size={14} /> {errors.fullName}
                </span>
              )}
            </div>

            <div className="form-group">
              <label>Email Address</label>
              <div className="input-with-icon">
                <Mail className="input-icon" size={20} />
                <input
                  type="email"
                  name="email"
                  placeholder="john@example.com"
                  value={formData.email}
                  onChange={handleChange}
                  className={errors.email ? 'input-error' : ''}
                />
              </div>
              {errors.email && (
                <span className="field-error-text">
                  <AlertCircle size={14} /> {errors.email}
                </span>
              )}
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>{role === 'STUDENT' ? 'Login Mobile (10 digits)' : 'Mobile Number (10 digits)'}</label>
              <div className="input-with-icon">
                <Phone className="input-icon" size={20} />
                <input
                  type="tel"
                  name="mobile"
                  maxLength={10}
                  placeholder="9876543210"
                  value={formData.mobile}
                  onChange={handleChange}
                  className={errors.mobile ? 'input-error' : ''}
                />
              </div>
              {errors.mobile && (
                <span className="field-error-text">
                  <AlertCircle size={14} /> {errors.mobile}
                </span>
              )}
            </div>

            <div className="form-group">
              <label>Password</label>
              <div className="input-with-icon">
                <Lock className="input-icon" size={20} />
                <input
                  type="password"
                  name="password"
                  placeholder="••••••••"
                  value={formData.password}
                  onChange={handleChange}
                  className={errors.password ? 'input-error' : ''}
                />
              </div>
              {errors.password && (
                <span className="field-error-text">
                  <AlertCircle size={14} /> {errors.password}
                </span>
              )}
            </div>
          </div>

          {role === 'STUDENT' && (
            <>
              <div className="form-row">
                <div className="form-group">
                  <label>Admission Number</label>
                  <div className="input-with-icon">
                    <Hash className="input-icon" size={20} />
                    <input
                      type="text"
                      name="admissionNumber"
                      placeholder="ADM123"
                      value={formData.admissionNumber}
                      onChange={handleChange}
                      className={errors.admissionNumber ? 'input-error' : ''}
                    />
                  </div>
                  {errors.admissionNumber && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.admissionNumber}
                    </span>
                  )}
                </div>

                <div className="form-group">
                  <label>Parent Mobile (10 digits)</label>
                  <div className="input-with-icon">
                    <Phone className="input-icon" size={20} />
                    <input
                      type="tel"
                      name="parentMobile"
                      maxLength={10}
                      placeholder="Parent's 10-digit number"
                      value={formData.parentMobile}
                      onChange={handleChange}
                      className={errors.parentMobile ? 'input-error' : ''}
                    />
                  </div>
                  {errors.parentMobile && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.parentMobile}
                    </span>
                  )}
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Student Contact Number (Optional, 10 digits)</label>
                  <div className="input-with-icon">
                    <Phone className="input-icon" size={20} />
                    <input
                      type="tel"
                      name="studentMobile"
                      maxLength={10}
                      placeholder="Optional student number"
                      value={formData.studentMobile}
                      onChange={handleChange}
                      className={errors.studentMobile ? 'input-error' : ''}
                    />
                  </div>
                  {errors.studentMobile && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.studentMobile}
                    </span>
                  )}
                </div>
              </div>

              <div className="form-row triple">
                <div className="form-group">
                  <label>Class</label>
                  <div className="input-with-icon">
                    <BookOpen className="input-icon" size={20} />
                    <input
                      type="text"
                      name="studentClass"
                      placeholder="10"
                      value={formData.studentClass}
                      onChange={handleChange}
                      className={errors.studentClass ? 'input-error' : ''}
                    />
                  </div>
                  {errors.studentClass && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.studentClass}
                    </span>
                  )}
                </div>

                <div className="form-group">
                  <label>Section</label>
                  <div className="input-with-icon">
                    <Layers className="input-icon" size={20} />
                    <input
                      type="text"
                      name="section"
                      placeholder="A"
                      value={formData.section}
                      onChange={handleChange}
                      className={errors.section ? 'input-error' : ''}
                    />
                  </div>
                  {errors.section && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.section}
                    </span>
                  )}
                </div>

                <div className="form-group">
                  <label>Roll No.</label>
                  <div className="input-with-icon">
                    <Hash className="input-icon" size={20} />
                    <input
                      type="text"
                      name="rollNumber"
                      placeholder="42"
                      value={formData.rollNumber}
                      onChange={handleChange}
                      className={errors.rollNumber ? 'input-error' : ''}
                    />
                  </div>
                  {errors.rollNumber && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.rollNumber}
                    </span>
                  )}
                </div>
              </div>
            </>
          )}

          {role === 'TEACHER' && (
            <div className="form-row">
              <div className="form-group">
                <label>Employee ID</label>
                <div className="input-with-icon">
                  <Hash className="input-icon" size={20} />
                  <input
                    type="text"
                    name="employeeId"
                    placeholder="EMP123"
                    value={formData.employeeId}
                    onChange={handleChange}
                    className={errors.employeeId ? 'input-error' : ''}
                  />
                </div>
                {errors.employeeId && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {errors.employeeId}
                  </span>
                )}
              </div>

              <div className="form-group">
                <label>Department</label>
                <div className="input-with-icon">
                  <Users className="input-icon" size={20} />
                  <input
                    type="text"
                    name="department"
                    placeholder="Science"
                    value={formData.department}
                    onChange={handleChange}
                    className={errors.department ? 'input-error' : ''}
                  />
                </div>
                {errors.department && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {errors.department}
                  </span>
                )}
              </div>
            </div>
          )}

          <div className="form-actions mt-4">
            <button type="submit" className="btn-primary login-btn" disabled={loading}>
              {loading ? <Loader2 className="spinner" size={20} /> : <UserPlus size={20} />}
              <span>Register</span>
            </button>
          </div>
        </form>

        <div className="login-footer">
          <p>Already have an account? <Link to="/login" className="link-amber">Sign in here</Link></p>
        </div>
      </div>
    </div>
  );
}
