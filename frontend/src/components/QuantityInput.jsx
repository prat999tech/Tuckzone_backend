import React, { useEffect, useState } from 'react';
import { Minus, Plus } from 'lucide-react';
import './QuantityInput.css';

/**
 * The -/+ control used in the menu grid and the cart, with a numeric field in the middle
 * for typing a quantity directly instead of clicking repeatedly (useful for large
 * quantities). Buffered locally so a mid-edit state (briefly empty while retyping) isn't
 * clobbered by the `quantity` prop on every keystroke — only committed values flow back
 * via onChange.
 */
export default function QuantityInput({ quantity, onDecrement, onIncrement, onChange, incrementDisabled, small }) {
  const [text, setText] = useState(String(quantity));

  useEffect(() => {
    setText(String(quantity));
  }, [quantity]);

  const commit = () => {
    const parsed = parseInt(text, 10);
    if (text.trim() === '' || Number.isNaN(parsed)) {
      setText(String(quantity));
      return;
    }
    if (parsed !== quantity) onChange(parsed);
    else setText(String(quantity));
  };

  return (
    <div className={`qty-controls ${small ? 'small' : ''}`}>
      <button type="button" onClick={onDecrement}>
        <Minus size={small ? 14 : 16} />
      </button>
      <input
        type="text"
        inputMode="numeric"
        className="qty-input-field"
        value={text}
        onChange={(e) => {
          const next = e.target.value.replace(/[^0-9]/g, '');
          setText(next);
        }}
        onBlur={commit}
        onKeyDown={(e) => {
          if (e.key === 'Enter') e.currentTarget.blur();
        }}
      />
      <button type="button" onClick={onIncrement} disabled={incrementDisabled}>
        <Plus size={small ? 14 : 16} />
      </button>
    </div>
  );
}
