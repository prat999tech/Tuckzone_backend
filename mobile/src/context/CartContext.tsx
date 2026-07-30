import React, { createContext, useContext, useMemo, useState, useCallback } from 'react';
import type { DailyMenuItemResponse } from '../api/types';

export interface CartLine {
  dayItem: DailyMenuItemResponse;
  quantity: number;
}

/**
 * Why `addToCart` reports a result instead of just doing the thing: two cases have to be
 * explained to the person rather than silently swallowed.
 *
 * - `date-conflict` — an order is placed for ONE menu date. Mixing dates used to be
 *   possible, and checkout then submitted whichever date happened to be added first,
 *   producing a baffling "you can't order for <yesterday>" at the last step.
 * - `stock-limit` — the cart already holds every remaining portion.
 */
export type AddToCartResult =
  | { status: 'added' }
  | { status: 'stock-limit'; remaining: number }
  | { status: 'date-conflict'; currentDate: string; attemptedDate: string };

interface CartContextValue {
  lines: CartLine[];
  itemCount: number;
  total: number;
  /** The single menu date this cart is for, or null while the cart is empty. */
  cartDate: string | null;
  addToCart: (dayItem: DailyMenuItemResponse) => AddToCartResult;
  /** Empties the cart and starts a new one with this item — resolves a date conflict. */
  startNewCartWith: (dayItem: DailyMenuItemResponse) => void;
  /** Decrements by one, dropping the line when it reaches zero. */
  removeFromCart: (dayItemId: string) => void;
  /** Removes the whole line regardless of quantity. */
  removeLine: (dayItemId: string) => void;
  quantityOf: (dayItemId: string) => number;
  clearCart: () => void;
}

const CartContext = createContext<CartContextValue | undefined>(undefined);

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>([]);

  // Derived rather than stored, so it can never drift out of sync with the lines.
  const cartDate = lines[0]?.dayItem.menuDate ?? null;

  const addToCart = useCallback((dayItem: DailyMenuItemResponse): AddToCartResult => {
    // Computed from the current state before the updater runs, so the caller gets a result
    // it can act on synchronously. `lines` is in the dependency list to keep it current.
    const existingDate = lines[0]?.dayItem.menuDate ?? null;
    if (existingDate && existingDate !== dayItem.menuDate) {
      return { status: 'date-conflict', currentDate: existingDate, attemptedDate: dayItem.menuDate };
    }

    const currentQty = lines.find((line) => line.dayItem.id === dayItem.id)?.quantity ?? 0;
    // Never let the cart exceed what is actually left in stock — the backend would reject
    // the excess anyway, but catching it here gives instant feedback instead of a
    // checkout failure.
    if (currentQty >= dayItem.remainingQuantity) {
      return { status: 'stock-limit', remaining: dayItem.remainingQuantity };
    }

    setLines((prev) => {
      const existing = prev.find((line) => line.dayItem.id === dayItem.id);
      if (existing) {
        return prev.map((line) =>
          line.dayItem.id === dayItem.id ? { ...line, quantity: line.quantity + 1 } : line,
        );
      }
      return [...prev, { dayItem, quantity: 1 }];
    });
    return { status: 'added' };
  }, [lines]);

  const startNewCartWith = useCallback((dayItem: DailyMenuItemResponse) => {
    setLines(dayItem.remainingQuantity > 0 ? [{ dayItem, quantity: 1 }] : []);
  }, []);

  const removeFromCart = useCallback((dayItemId: string) => {
    setLines((prev) =>
      prev
        .map((line) => (line.dayItem.id === dayItemId ? { ...line, quantity: line.quantity - 1 } : line))
        .filter((line) => line.quantity > 0),
    );
  }, []);

  const removeLine = useCallback((dayItemId: string) => {
    setLines((prev) => prev.filter((line) => line.dayItem.id !== dayItemId));
  }, []);

  const quantityOf = useCallback(
    (dayItemId: string) => lines.find((line) => line.dayItem.id === dayItemId)?.quantity ?? 0,
    [lines],
  );

  const clearCart = useCallback(() => setLines([]), []);

  const value = useMemo<CartContextValue>(() => {
    const itemCount = lines.reduce((sum, line) => sum + line.quantity, 0);
    const total = lines.reduce((sum, line) => sum + line.dayItem.menuItem.price * line.quantity, 0);
    return {
      lines,
      itemCount,
      total,
      cartDate,
      addToCart,
      startNewCartWith,
      removeFromCart,
      removeLine,
      quantityOf,
      clearCart,
    };
  }, [lines, cartDate, addToCart, startNewCartWith, removeFromCart, removeLine, quantityOf, clearCart]);

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart(): CartContextValue {
  const context = useContext(CartContext);
  if (!context) throw new Error('useCart must be used within a CartProvider');
  return context;
}
