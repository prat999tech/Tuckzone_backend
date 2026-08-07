import React, { useEffect, useState } from 'react';
import { getTodayMenu, getFixedMenu } from '../api/menu';
import { getChildren } from '../api/parent';
import { placeOrder } from '../api/orders';
import { useAuth } from '../context/AuthContext';
import { classLabel } from '../utils/format';
import { ShoppingCart, AlertCircle, Search } from 'lucide-react';
import toast from 'react-hot-toast';
import QuantityInput from '../components/QuantityInput';
import './MenuPage.css';

export default function MenuPage() {
  const { user } = useAuth();
  const [dailyMenu, setDailyMenu] = useState([]);
  const [fixedMenu, setFixedMenu] = useState([]);
  const [children, setChildren] = useState([]);
  const [loading, setLoading] = useState(true);

  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [search, setSearch] = useState('');

  // Cart & Order Form State
  const [cart, setCart] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [selectedChild, setSelectedChild] = useState('');
  const [deliveryLocation, setDeliveryLocation] = useState('');
  const [errors, setErrors] = useState({});

  useEffect(() => {
    fetchInitialData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDate]);

  const fetchInitialData = async () => {
    try {
      setLoading(true);
      const [dailyData, fixedData] = await Promise.all([
        getTodayMenu({ date: selectedDate }),
        getFixedMenu(),
      ]);
      setDailyMenu(dailyData);
      setFixedMenu(fixedData);

      if (user?.role === 'PARENT') {
        const childData = await getChildren();
        setChildren(childData);
      }
    } catch (err) {
      toast.error('Failed to load menu');
    } finally {
      setLoading(false);
    }
  };

  const addToCart = (item) => {
    setCart((prev) => {
      const existing = prev.find((i) => i.menuItem.id === item.id);
      if (existing) {
        return prev.map((i) =>
          i.menuItem.id === item.id ? { ...i, quantity: i.quantity + 1 } : i
        );
      }
      return [...prev, { menuItem: item, quantity: 1 }];
    });
  };

  const removeFromCart = (itemId) => {
    setCart((prev) =>
      prev
        .map((i) => (i.menuItem.id === itemId ? { ...i, quantity: i.quantity - 1 } : i))
        .filter((i) => i.quantity > 0)
    );
  };

  const getQuantityInCart = (itemId) => {
    const item = cart.find((i) => i.menuItem.id === itemId);
    return item ? item.quantity : 0;
  };

  /** Sets an exact quantity (e.g. from typing into the quantity field) rather than
   *  stepping by one. Clamped to [0, maxQty] when a stock limit applies; 0 removes the
   *  line. */
  const setQuantityInCart = (item, qty, maxQty) => {
    const clamped = Math.max(0, maxQty != null ? Math.min(qty, maxQty) : qty);
    setCart((prev) => {
      if (clamped === 0) return prev.filter((i) => i.menuItem.id !== item.id);
      const existing = prev.find((i) => i.menuItem.id === item.id);
      if (existing) {
        return prev.map((i) => (i.menuItem.id === item.id ? { ...i, quantity: clamped } : i));
      }
      return [...prev, { menuItem: item, quantity: clamped }];
    });
  };

  const getCartTotal = () => {
    return cart.reduce((total, item) => total + item.menuItem.price * item.quantity, 0);
  };

  const validateOrderForm = () => {
    const errs = {};

    if (user?.role === 'PARENT') {
      if (!selectedChild) {
        errs.child = 'Please select a child for this order';
      }
    } else if (user?.role === 'TEACHER') {
      if (!deliveryLocation.trim()) {
        errs.location = 'Delivery location is required';
      }
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const placeOrderAction = async () => {
    if (cart.length === 0) return toast.error('Your cart is empty');

    if (!validateOrderForm()) {
      toast.error('Please fix the order details marked in red');
      return;
    }

    try {
      const orderPayload = {
        menuDate: selectedDate,
        deliveryLocation: user?.role === 'TEACHER' ? deliveryLocation.trim() : undefined,
        items: cart.map((i) => ({ menuItemId: i.menuItem.id, quantity: i.quantity })),
        idempotencyKey: crypto.randomUUID(),
        ...(user?.role === 'PARENT' && { beneficiaryStudentProfileId: selectedChild }),
      };

      await placeOrder(orderPayload);
      toast.success('Order placed successfully!');
      setCart([]);
      setIsCartOpen(false);
      setErrors({});
      fetchInitialData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to place order');
    }
  };

  const matchesSearch = (name) =>
    !search.trim() || name.toLowerCase().includes(search.trim().toLowerCase());

  const filteredDaily = dailyMenu.filter((dayItem) => matchesSearch(dayItem.menuItem.name));
  const filteredFixed = fixedMenu.filter((item) => matchesSearch(item.name));

  const renderDailyCard = (dayItem) => {
    const { menuItem, remainingQuantity, available } = dayItem;
    const isSoldOut = !available || remainingQuantity <= 0;
    const qtyInCart = getQuantityInCart(menuItem.id);

    return (
      <div className={`food-card ${isSoldOut ? 'sold-out' : ''}`} key={dayItem.id}>
        {isSoldOut && <div className="sold-out-badge">Sold Out</div>}
        <div className="food-img-placeholder">
          {menuItem.imageUrl ? (
            <img src={menuItem.imageUrl} alt={menuItem.name} />
          ) : (
            <div className="img-fallback">{menuItem.name.charAt(0)}</div>
          )}
        </div>
        <div className="food-info">
          <h3>{menuItem.name}</h3>
          <p className="desc">{menuItem.description}</p>
          <div className="food-meta">
            <span className="price">₹{menuItem.price}</span>
            <span className="stock">{remainingQuantity} left</span>
          </div>

          <div className="food-actions">
            {qtyInCart > 0 ? (
              <QuantityInput
                quantity={qtyInCart}
                onDecrement={() => removeFromCart(menuItem.id)}
                onIncrement={() => addToCart(menuItem)}
                onChange={(next) => setQuantityInCart(menuItem, next, remainingQuantity)}
                incrementDisabled={isSoldOut || qtyInCart >= remainingQuantity}
              />
            ) : (
              <button className="btn-add" disabled={isSoldOut} onClick={() => addToCart(menuItem)}>
                Add to Cart
              </button>
            )}
          </div>
        </div>
      </div>
    );
  };

  const renderFixedCard = (menuItem) => {
    const isSoldOut = !menuItem.available;
    const qtyInCart = getQuantityInCart(menuItem.id);

    return (
      <div className={`food-card ${isSoldOut ? 'sold-out' : ''}`} key={menuItem.id}>
        {isSoldOut && <div className="sold-out-badge">Out of Stock</div>}
        <div className="food-img-placeholder">
          {menuItem.imageUrl ? (
            <img src={menuItem.imageUrl} alt={menuItem.name} />
          ) : (
            <div className="img-fallback">{menuItem.name.charAt(0)}</div>
          )}
        </div>
        <div className="food-info">
          <h3>{menuItem.name}</h3>
          <p className="desc">{menuItem.description}</p>
          <div className="food-meta">
            <span className="price">₹{menuItem.price}</span>
          </div>

          <div className="food-actions">
            {qtyInCart > 0 ? (
              <QuantityInput
                quantity={qtyInCart}
                onDecrement={() => removeFromCart(menuItem.id)}
                onIncrement={() => addToCart(menuItem)}
                onChange={(next) => setQuantityInCart(menuItem, next)}
                incrementDisabled={isSoldOut}
              />
            ) : (
              <button className="btn-add" disabled={isSoldOut} onClick={() => addToCart(menuItem)}>
                Add to Cart
              </button>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="menu-container">
      <div className="menu-header">
        <div>
          <h1>Menu</h1>
          <p>Order fresh meals delivered right to your classroom</p>
        </div>

        <div className="date-picker">
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
          />
        </div>
      </div>

      <div className="menu-search">
        <Search size={18} className="menu-search-icon" />
        <input
          type="text"
          placeholder="Search menu items..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <section className="menu-section">
        <h2 className="menu-section-title">Meal of the Day</h2>
        {loading ? (
          <div className="loading-state">Loading menu...</div>
        ) : filteredDaily.length === 0 ? (
          <div className="empty-state">
            No Meal of the Day is published for {selectedDate}. Ask the canteen admin to schedule that day&apos;s items.
          </div>
        ) : (
          <div className="menu-grid">
            {filteredDaily.map(renderDailyCard)}
          </div>
        )}
      </section>

      <section className="menu-section">
        <h2 className="menu-section-title">Daily Delights</h2>
        {loading ? (
          <div className="loading-state">Loading menu...</div>
        ) : filteredFixed.length === 0 ? (
          <div className="empty-state">No Daily Delights items available right now.</div>
        ) : (
          <div className="menu-grid">
            {filteredFixed.map(renderFixedCard)}
          </div>
        )}
      </section>

      {cart.length > 0 && (
        <div className="floating-cart-bar" onClick={() => setIsCartOpen(true)}>
          <div className="cart-summary">
            <div className="cart-icon-wrapper">
              <ShoppingCart size={24} />
              <span className="cart-badge">{cart.reduce((acc, item) => acc + item.quantity, 0)}</span>
            </div>
            <span>View Cart • ₹{getCartTotal().toFixed(2)}</span>
          </div>
        </div>
      )}

      {isCartOpen && (
        <div className="cart-drawer-overlay" onClick={() => setIsCartOpen(false)}>
          <div className="cart-drawer" onClick={(e) => e.stopPropagation()}>
            <h2>Your Order</h2>

            <div className="cart-items">
              {cart.map((item) => (
                <div key={item.menuItem.id} className="cart-item">
                  <div className="cart-item-info">
                    <h4>{item.menuItem.name}</h4>
                    <span>₹{item.menuItem.price} x {item.quantity}</span>
                  </div>
                  <QuantityInput
                    small
                    quantity={item.quantity}
                    onDecrement={() => removeFromCart(item.menuItem.id)}
                    onIncrement={() => addToCart(item.menuItem)}
                    onChange={(next) => setQuantityInCart(item.menuItem, next)}
                  />
                </div>
              ))}
            </div>

            <div className="cart-total">
              <span>Total Amount</span>
              <span>₹{getCartTotal().toFixed(2)}</span>
            </div>

            <div className="order-details-form">
              <div className="form-group">
                <label>Recess</label>
                <p className="recess-note">Your order will be ready for Recess.</p>
              </div>

              {user?.role === 'PARENT' ? (
                <div className="form-group">
                  <label>Order for Child</label>
                  <select
                    value={selectedChild}
                    onChange={(e) => {
                      setSelectedChild(e.target.value);
                      if (errors.child) setErrors({ ...errors, child: null });
                    }}
                    className={errors.child ? 'input-error' : ''}
                  >
                    <option value="">Select child...</option>
                    {children.map((child) => (
                      <option key={child.linkId} value={child.studentProfileId}>
                        {child.fullName} ({child.studentClass}-{child.section})
                      </option>
                    ))}
                  </select>
                  {errors.child && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.child}
                    </span>
                  )}
                </div>
              ) : user?.role === 'STUDENT' ? (
                <div className="form-row">
                  <div className="form-group">
                    <label>Student Name</label>
                    <input type="text" value={user.fullName} disabled />
                  </div>
                  <div className="form-group">
                    <label>Class</label>
                    <input type="text" value={classLabel(user.studentClass, user.section)} disabled />
                  </div>
                </div>
              ) : (
                <div className="form-group">
                  <label>Delivery Location</label>
                  <input
                    type="text"
                    value={deliveryLocation}
                    onChange={(e) => {
                      setDeliveryLocation(e.target.value);
                      if (errors.location) setErrors({ ...errors, location: null });
                    }}
                    placeholder="e.g. Staff Room / Science Dept"
                    className={errors.location ? 'input-error' : ''}
                  />
                  {errors.location && (
                    <span className="field-error-text">
                      <AlertCircle size={14} /> {errors.location}
                    </span>
                  )}
                </div>
              )}
            </div>

            <button className="btn-primary w-100" onClick={placeOrderAction}>
              Pay & Place Order
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
