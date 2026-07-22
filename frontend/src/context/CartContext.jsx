import React, { createContext, useContext, useMemo, useState } from 'react';

const CartContext = createContext();

export const CartProvider = ({ children }) => {
  const [cart, setCart] = useState([]);

  const addToCart = (menuItem) => {
    setCart((previousCart) => {
      const existing = previousCart.find((item) => item.menuItem.id === menuItem.id);
      if (existing) {
        return previousCart.map((item) =>
          item.menuItem.id === menuItem.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        );
      }
      return [...previousCart, { menuItem, quantity: 1 }];
    });
  };

  const removeFromCart = (menuItemId) => {
    setCart((previousCart) =>
      previousCart.flatMap((item) => {
        if (item.menuItem.id !== menuItemId) {
          return [item];
        }
        if (item.quantity <= 1) {
          return [];
        }
        return [{ ...item, quantity: item.quantity - 1 }];
      })
    );
  };

  const clearCart = () => setCart([]);

  const itemCount = useMemo(
    () => cart.reduce((sum, item) => sum + item.quantity, 0),
    [cart]
  );

  const getCartTotal = () =>
    cart.reduce((sum, item) => sum + Number(item.menuItem.price) * item.quantity, 0);

  return (
    <CartContext.Provider
      value={{
        cart,
        addToCart,
        removeFromCart,
        clearCart,
        itemCount,
        getCartTotal,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => useContext(CartContext);
