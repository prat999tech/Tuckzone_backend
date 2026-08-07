// Loads Razorpay's hosted Checkout.js widget on demand — never bundled, never loaded
// until a real (non-mock) gateway payment is actually being started.

let scriptPromise = null;

function loadRazorpayScript() {
  if (typeof window !== 'undefined' && window.Razorpay) {
    return Promise.resolve();
  }
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Could not load the payment widget'));
      document.body.appendChild(script);
    });
  }
  return scriptPromise;
}

/**
 * Opens Razorpay's checkout widget for a payment this app already created server-side.
 * Resolves with the provider's callback fields once the user completes checkout; rejects
 * if they dismiss it or the widget fails to load. Nothing here decides what was charged —
 * the widget only ever displays what providerOrderId already committed to server-side.
 */
export async function openRazorpayCheckout({ providerOrderId, providerKeyId, name, description, prefill }) {
  await loadRazorpayScript();
  return new Promise((resolve, reject) => {
    const rzp = new window.Razorpay({
      key: providerKeyId,
      order_id: providerOrderId,
      name: name || 'TuckZone',
      description,
      prefill,
      handler: (response) => resolve({
        providerOrderId: response.razorpay_order_id,
        providerPaymentId: response.razorpay_payment_id,
        signature: response.razorpay_signature,
      }),
      modal: {
        ondismiss: () => reject(new Error('Payment was cancelled')),
      },
    });
    rzp.open();
  });
}
