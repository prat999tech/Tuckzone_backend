import React, { useEffect, useState } from 'react';
import { Check, X, ShieldAlert, UserCheck } from 'lucide-react';
import { approveUser, listUsers, rejectUser } from '../../api/admin';
import toast from 'react-hot-toast';
import './UserApprovalPage.css';

export default function UserApprovalPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('PENDING');

  useEffect(() => {
    fetchUsers();
  }, [activeTab]);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const data = await listUsers(activeTab);
      setUsers(data);
    } catch (err) {
      toast.error('Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleApproval = async (userId, action) => {
    try {
      if (action === 'APPROVE') {
        await approveUser(userId);
      } else {
        await rejectUser(userId);
      }
      toast.success(`User ${action.toLowerCase()}d successfully`);
      fetchUsers();
    } catch (err) {
      toast.error(`Failed to ${action.toLowerCase()} user`);
    }
  };

  return (
    <div className="admin-container">
      <div className="page-header">
        <h1>User Approvals</h1>
        <p>Review registration requests before anyone starts placing school orders.</p>
      </div>

      <div className="status-tabs">
        <button className={`tab-btn ${activeTab === 'PENDING' ? 'active' : ''}`} onClick={() => setActiveTab('PENDING')}>
          Pending
        </button>
        <button className={`tab-btn ${activeTab === 'APPROVED' ? 'active' : ''}`} onClick={() => setActiveTab('APPROVED')}>
          Approved
        </button>
        <button className={`tab-btn ${activeTab === 'REJECTED' ? 'active' : ''}`} onClick={() => setActiveTab('REJECTED')}>
          Rejected
        </button>
      </div>

      {loading ? (
        <div className="loading-state">Loading users...</div>
      ) : users.length === 0 ? (
        <div className="empty-state">
          {activeTab === 'PENDING' ? <ShieldAlert size={48} className="text-amber" /> : <UserCheck size={48} className="text-amber" />}
          <h2>No {activeTab.toLowerCase()} users</h2>
        </div>
      ) : (
        <div className="users-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Role</th>
                <th>Contact</th>
                <th>Details</th>
                <th>Date</th>
                {activeTab === 'PENDING' && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div className="user-name">{user.fullName}</div>
                  </td>
                  <td>
                    <span className={`role-badge role-${user.role.toLowerCase()}`}>{user.role}</span>
                  </td>
                  <td>
                    <div className="contact-info">
                      <div>{user.email}</div>
                      {user.mobile && <div className="text-muted">{user.mobile}</div>}
                    </div>
                  </td>
                  <td>
                    <div className="user-details">
                      {user.role === 'STUDENT' && user.admissionNumber && (
                        <span>Adm: {user.admissionNumber} | Class: {user.studentClass}-{user.section} | Roll: {user.rollNumber}</span>
                      )}
                      {user.role === 'TEACHER' && user.employeeId && (
                        <span>Emp: {user.employeeId} | Dept: {user.department}</span>
                      )}
                      {user.role === 'PARENT' && <span>Parent account</span>}
                    </div>
                  </td>
                  <td>{user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}</td>
                  {activeTab === 'PENDING' && (
                    <td>
                      <div className="action-buttons">
                        <button
                          className="btn-icon-success"
                          onClick={() => handleApproval(user.id, 'APPROVE')}
                          title="Approve"
                        >
                          <Check size={18} />
                        </button>
                        <button
                          className="btn-icon-danger"
                          onClick={() => handleApproval(user.id, 'REJECT')}
                          title="Reject"
                        >
                          <X size={18} />
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
