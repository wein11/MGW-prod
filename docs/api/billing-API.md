# Billing module API

Base path: `/api`. All request/response bodies are JSON.

Auth: session-token based, same `SessionAuthInterceptor` as the rest of the project — send
`Authorization: Bearer <token>`. All three endpoints below require authentication.

No DTOs: request/response bodies are the `Subscription` entity directly, following the
project-wide convention. None of the three endpoints take a request body — the acting user comes
from the session (`userId` request attribute), same pattern as `POST /api/challenges/{id}/close`.

**Auto-creation:** every user with `role = ARTIST` gets a `Subscription` (`FREE`, `productionsCount
= 0`) lazily on first access — there's no explicit "subscribe" step. `DISCOGRAFICA` and `ADMIN`
never get one (they don't publish beats/toplines, so the production limit doesn't apply to them);
calling any of these endpoints as one of those roles returns 403.

**Payment:** `POST /api/subscriptions/upgrade` charges via `PaymentGateway`, an interface with one
implementation today (`SimulatedPaymentGateway`, always approves). Swapping in a real gateway
(Mercado Pago or otherwise) later means adding a class that implements `PaymentGateway` — no
change needed in `SubscriptionService` or either controller.

**Enforcement:** `POST /api/beats` and `POST /api/toplines` both call
`SubscriptionService.recordProduction(userId)` before saving. On a `FREE` plan at 50 combined
beats+toplines (lifetime total — doesn't decrease if you delete one), that call throws and the
beat/topline is never persisted. `PREMIUM` has no limit.

## GET /api/subscriptions/me

Returns the caller's own subscription (creating it in `FREE` if this is their first call).

- **Auth:** required.
- **Response body** (`Subscription`):

  | Field | Type |
  |---|---|
  | `id` | number |
  | `userId` | number |
  | `plan` | `"FREE"` \| `"PREMIUM"` |
  | `productionsCount` | number — lifetime beats+toplines published, regardless of plan |
  | `createdAt` | ISO-8601 timestamp |

- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Subscription returned (created on first call) |
  | 401 | Not authenticated |
  | 403 | Authenticated caller's role is not `ARTIST` |
  | 500 | Unexpected server error |

## POST /api/subscriptions/upgrade

Charges 15 USD via `PaymentGateway` (simulated — always approves) and sets the plan to `PREMIUM`.

- **Auth:** required.
- **Response body:** `Subscription` (see shape above), `plan` now `"PREMIUM"`.
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Upgraded |
  | 401 | Not authenticated |
  | 403 | Authenticated caller's role is not `ARTIST` |
  | 500 | Unexpected server error |

## PUT /api/subscriptions/downgrade

Sets the plan back to `FREE`. No refund modeled (this is the simulated gateway — there's no real
charge to reverse). If `productionsCount` is already over 50, existing beats/toplines are kept;
the caller just can't publish new ones until they upgrade again.

- **Auth:** required.
- **Response body:** `Subscription` (see shape above), `plan` now `"FREE"`.
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Downgraded |
  | 401 | Not authenticated |
  | 403 | Authenticated caller's role is not `ARTIST` |
  | 500 | Unexpected server error |

No `DELETE`: every `ARTIST` always has exactly one `Subscription` row — "canceling" means calling
`downgrade`, not removing the record.
