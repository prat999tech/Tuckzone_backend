# Environment Variables

See `backend/.env.example` for the authoritative, always-up-to-date list. Summary:

| Variable | Default | Notes |
|---|---|---|
| `APP_PAYMENT_PROVIDER` | `mock` | `mock` or `razorpay` |
| `APP_ALLOW_MOCK_TOPUP` | `true` | Gates `/mock-complete` endpoints regardless of provider |
| `RAZORPAY_KEY_ID` | — | Only read when provider=razorpay |
| `RAZORPAY_KEY_SECRET` | — | Only read when provider=razorpay |
| `RAZORPAY_WEBHOOK_SECRET` | — | Configured separately in the Razorpay dashboard's Webhooks section |
| `EMAIL_FROM_ADDRESS` etc. | — | Unrelated to payments — see the main `.env.example` |

`RAZORPAY_KEY_ID`/`RAZORPAY_KEY_SECRET` back the generic `app.payment.key-id`/
`key-secret` properties (`PaymentProperties`) — every provider reads from there, not a
provider-specific property, so switching providers is a value change, not a key rename.

Never commit real values. `RazorpayProvider` fails fast at startup with a clear message if
`provider=razorpay` and either key is blank.
