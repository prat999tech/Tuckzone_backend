import React, { useEffect, useState } from 'react';
import { Wallet, ArrowUpRight, ArrowDownLeft, CreditCard, AlertCircle } from 'lucide-react';
import { getTransactions, getWallet, initiateTopup, mockCompleteTopup } from '../api/wallet';
import toast from 'react-hot-toast';
import './WalletPage.css';

export default function WalletPage() {
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [topupAmount, setTopupAmount] = useState('');
  const [error, setError] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    fetchWalletData();
  }, []);

  const fetchWalletData = async () => {
    try {
      const [walletData, transactionData] = await Promise.all([
        getWallet(),
        getTransactions(),
      ]);
      setWallet(walletData);
      setTransactions(transactionData);
    } catch (err) {
      toast.error('Failed to load wallet data');
    } finally {
      setLoading(false);
    }
  };

  const validateAmount = (value) => {
    const num = parseFloat(value);
    if (!value || isNaN(num)) {
      return 'Enter a valid amount';
    }
    if (num < 1) {
      return 'Top-up amount must be at least ₹1.00';
    }
    if (num > 10000) {
      return 'Top-up amount cannot exceed ₹10,000';
    }
    return null;
  };

  const handleTopup = async (amount) => {
    const value = amount || topupAmount;
    const validationErr = validateAmount(value);
    if (validationErr) {
      setError(validationErr);
      toast.error(validationErr);
      return;
    }
    setError(null);

    try {
      setIsProcessing(true);
      const topup = await initiateTopup({ amount: Number(value).toFixed(2) });

      toast.loading('Processing payment...', { id: 'payment' });
      await mockCompleteTopup({ gatewayOrderId: topup.gatewayOrderId });

      toast.success('Wallet recharged successfully', { id: 'payment' });
      setTopupAmount('');
      setError(null);
      fetchWalletData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Top-up failed', { id: 'payment' });
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="wallet-container">
      <div className="page-header">
        <h1>My Wallet</h1>
        <p>Manage your school canteen funds</p>
      </div>

      {loading ? (
        <div className="loading-state">Loading wallet...</div>
      ) : (
        <div className="wallet-content">
          <div className="balance-card">
            <div className="card-bg-decoration"></div>
            <div className="balance-info">
              <span className="label">Available Balance</span>
              <h2>{wallet?.currency} {Number(wallet?.balance || 0).toFixed(2)}</h2>
            </div>
            <Wallet size={48} className="card-icon" />
          </div>

          <div className="topup-section">
            <h3>Quick Top-up</h3>
            <div className="quick-amounts">
              {[100, 200, 500, 1000].map((amt) => (
                <button
                  key={amt}
                  className="btn-quick-amt"
                  onClick={() => handleTopup(amt)}
                  disabled={isProcessing}
                >
                  +₹{amt}
                </button>
              ))}
            </div>
            <div className="custom-topup">
              <div className="input-group-col" style={{ flex: 1 }}>
                <div className="input-with-icon">
                  <span className="currency-symbol">₹</span>
                  <input
                    type="number"
                    placeholder="Enter custom amount"
                    value={topupAmount}
                    onChange={(e) => {
                      setTopupAmount(e.target.value);
                      if (error) setError(null);
                    }}
                    className={error ? 'input-error' : ''}
                  />
                </div>
                {error && (
                  <span className="field-error-text">
                    <AlertCircle size={14} /> {error}
                  </span>
                )}
              </div>
              <button
                className="btn-primary"
                onClick={() => handleTopup()}
                disabled={isProcessing || !topupAmount}
              >
                <CreditCard size={18} /> Add Money
              </button>
            </div>
          </div>

          <div className="transactions-section">
            <h3>Recent Transactions</h3>
            {transactions.length === 0 ? (
              <div className="empty-state small">No transactions yet</div>
            ) : (
              <div className="transaction-list">
                {transactions.map((tx) => (
                  <div className="transaction-item" key={tx.id}>
                    <div className={`tx-icon ${tx.type.toLowerCase()}`}>
                      {tx.type === 'CREDIT' ? <ArrowDownLeft size={20} /> : <ArrowUpRight size={20} />}
                    </div>
                    <div className="tx-details">
                      <span className="tx-desc">{tx.description}</span>
                      <span className="tx-date">{new Date(tx.createdAt).toLocaleString()}</span>
                    </div>
                    <div className={`tx-amount ${tx.type.toLowerCase()}`}>
                      {tx.type === 'CREDIT' ? '+' : '-'}₹{Number(tx.amount).toFixed(2)}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
