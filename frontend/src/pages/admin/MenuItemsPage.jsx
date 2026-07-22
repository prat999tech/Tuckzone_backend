import React, { useEffect, useState } from 'react';
import { Plus, Edit2, Trash2, X, AlertCircle } from 'lucide-react';
import { createMenuItem, deleteMenuItem, getMenuItems, updateMenuItem } from '../../api/admin';
import toast from 'react-hot-toast';
import './MenuItemsPage.css';

export default function MenuItemsPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [errors, setErrors] = useState({});
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: '',
    foodType: 'VEG',
    category: 'SNACKS',
    imageUrl: '',
    allergens: '',
  });

  useEffect(() => {
    fetchItems();
  }, []);

  const fetchItems = async () => {
    try {
      const data = await getMenuItems(true);
      setItems(data);
    } catch (err) {
      toast.error('Failed to load menu items');
    } finally {
      setLoading(false);
    }
  };

  const validate = () => {
    const errs = {};
    if (!formData.name.trim()) {
      errs.name = 'Item name is required';
    }

    const priceNum = parseFloat(formData.price);
    if (!formData.price || isNaN(priceNum) || priceNum <= 0) {
      errs.price = 'Enter a valid price greater than ₹0';
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
      const payload = {
        ...formData,
        price: Number(formData.price).toFixed(2),
      };

      if (editingId) {
        await updateMenuItem(editingId, payload);
        toast.success('Item updated');
      } else {
        await createMenuItem(payload);
        toast.success('Item created');
      }

      setIsModalOpen(false);
      setErrors({});
      fetchItems();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Action failed');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Retire this menu item from future ordering?')) return;
    try {
      await deleteMenuItem(id);
      toast.success('Item retired');
      fetchItems();
    } catch (err) {
      toast.error('Delete failed');
    }
  };

  const openModal = (item = null) => {
    setErrors({});
    if (item) {
      setEditingId(item.id);
      setFormData({
        name: item.name,
        description: item.description || '',
        price: item.price,
        foodType: item.foodType,
        category: item.category,
        imageUrl: item.imageUrl || '',
        allergens: item.allergens || '',
      });
    } else {
      setEditingId(null);
      setFormData({
        name: '',
        description: '',
        price: '',
        foodType: 'VEG',
        category: 'SNACKS',
        imageUrl: '',
        allergens: '',
      });
    }
    setIsModalOpen(true);
  };

  return (
    <div className="admin-container">
      <div className="page-header flex-between">
        <div>
          <h1>Menu Catalog</h1>
          <p>Manage all canteen food items</p>
        </div>
        <button className="btn-primary" onClick={() => openModal()}>
          <Plus size={18} /> Add New Item
        </button>
      </div>

      {loading ? (
        <div className="loading-state">Loading items...</div>
      ) : (
        <div className="items-grid">
          {items.map((item) => (
            <div className={`catalog-card ${!item.active ? 'inactive' : ''}`} key={item.id}>
              <div className="card-image">
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name} />
                ) : (
                  <div className="img-fallback">{item.name.charAt(0)}</div>
                )}
                <div className={`food-type-badge ${item.foodType.toLowerCase()}`}>
                  <div className="dot"></div>
                </div>
              </div>
              <div className="card-content">
                <div className="card-header">
                  <h3>{item.name}</h3>
                  <span className="badge badge-amber">{item.category}</span>
                </div>
                <p className="desc">{item.description}</p>
                <div className="card-footer">
                  <span className="price">₹{item.price}</span>
                  <div className="actions">
                    <button className="btn-icon" onClick={() => openModal(item)} title="Edit">
                      <Edit2 size={16} />
                    </button>
                    {item.active && (
                      <button className="btn-icon-danger" onClick={() => handleDelete(item.id)} title="Delete">
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingId ? 'Edit Menu Item' : 'Add New Menu Item'}</h2>
              <button className="close-btn" onClick={() => setIsModalOpen(false)}>
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleSubmit} noValidate>
              <div className="form-group">
                <label>Item Name</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => {
                    setFormData({ ...formData, name: e.target.value });
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
                <label>Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={3}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Price (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.price}
                    onChange={(e) => {
                      setFormData({ ...formData, price: e.target.value });
                      if (errors.price) setErrors({ ...errors, price: null });
                    }}
                    className={errors.price ? 'input-error' : ''}
                  />
                  {errors.price && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.price}
                    </span>
                  )}
                </div>

                <div className="form-group">
                  <label>Food Type</label>
                  <select
                    value={formData.foodType}
                    onChange={(e) => setFormData({ ...formData, foodType: e.target.value })}
                  >
                    <option value="VEG">Vegetarian (VEG)</option>
                    <option value="NON_VEG">Non-Vegetarian (NON_VEG)</option>
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Category</label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  >
                    <option value="MEALS">MEALS</option>
                    <option value="SNACKS">SNACKS</option>
                    <option value="DRINKS">DRINKS</option>
                    <option value="COMBOS">COMBOS</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Image URL</label>
                  <input
                    type="url"
                    placeholder="https://..."
                    value={formData.imageUrl}
                    onChange={(e) => setFormData({ ...formData, imageUrl: e.target.value })}
                  />
                </div>
              </div>

              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setIsModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  {editingId ? 'Save Changes' : 'Create Item'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
