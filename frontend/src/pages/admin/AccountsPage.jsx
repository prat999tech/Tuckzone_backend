import React, { useEffect, useState } from 'react';
import { Users as UsersIcon } from 'lucide-react';
import { listUsers, disableUser, enableUser } from '../../api/admin';
import ConfirmDialog from '../../components/ConfirmDialog';
import toast from 'react-hot-toast';
import './AccountsPage.css';

const ROLE_FILTERS = ['ALL', 'STUDENT', 'TEACHER', 'PARENT', 'CANTEEN_ADMIN'];

export default function AccountsPage() {
  const [role, setRole] = useState('ALL');
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [confirmUser, setConfirmUser] = useState(null);

  useEffect(() => {
    fetchUsers();
  }, [role]);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const data = await listUsers(role === 'ALL' ? undefined : role);
      setUsers(data);
    } catch (err) {
      toast.error('Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = async () => {
    if (!confirmUser) return;
    const disabling = confirmUser.status === 'ACTIVE';
    setBusyId(confirmUser.id);
    try {
      if (disabling) await disableUser(confirmUser.id);
      else await enableUser(confirmUser.id);
      toast.success(disabling ? 'Account disabled' : 'Account enabled');
      setConfirmUser(null);
      fetchUsers();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="accounts-container">
      <div className="page-header">
        <h1>Accounts</h1>
        <p>Enable or disable access</p>
      </div>

      <div className="role-filters-pills">
        {ROLE_FILTERS.map((r) => (
          <button
            key={r}
            className={`pill-btn ${role === r ? 'active' : ''}`}
            onClick={() => setRole(r)}
          >
            {r.replace('_', ' ')}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading-state">Loading accounts...</div>
      ) : users.length === 0 ? (
        <div className="empty-state">
          <UsersIcon size={48} className="text-amber" />
          <h2>No accounts found</h2>
        </div>
      ) : (
        <div className="accounts-list">
          {users.map((user) => (
            <div className="card account-row" key={user.id}>
              <div className="account-info">
                <span className="account-name">{user.fullName}</span>
                <span className="account-contact">{user.email} · {user.mobile}</span>
                <div className="account-badges">
                  <span className="badge badge-info">{user.role.replace('_', ' ')}</span>
                  <span className={`badge ${user.status === 'ACTIVE' ? 'badge-success' : 'badge-error'}`}>
                    {user.status}
                  </span>
                </div>
              </div>
              <button
                type="button"
                className={user.status === 'ACTIVE' ? 'btn-danger-outline' : 'btn-primary'}
                disabled={busyId === user.id}
                onClick={() => setConfirmUser(user)}
              >
                {user.status === 'ACTIVE' ? 'Disable' : 'Enable'}
              </button>
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={!!confirmUser}
        title={confirmUser?.status === 'ACTIVE' ? 'Disable account?' : 'Enable account?'}
        message={
          confirmUser?.status === 'ACTIVE'
            ? `${confirmUser?.fullName} will be signed out and unable to use the app.`
            : `${confirmUser?.fullName} will be able to sign in again.`
        }
        confirmLabel={confirmUser?.status === 'ACTIVE' ? 'Disable' : 'Enable'}
        onConfirm={handleToggle}
        onCancel={() => setConfirmUser(null)}
      />
    </div>
  );
}
