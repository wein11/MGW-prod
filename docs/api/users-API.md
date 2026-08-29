# Users module API

Base path: `/api`. All request/response bodies are JSON.

Auth: session-token based. `POST /api/auth/login` returns a `token`. Send it on
protected endpoints as `Authorization: Bearer <token>`. `SessionAuthInterceptor`
reads this header on every request; if it's absent, the request proceeds
unauthenticated (no `userId` attribute set) rather than being rejected at the
interceptor level — each endpoint decides for itself whether auth is required.

## POST /api/auth/register

Creates a new user (and its `ProducerProfile` or `ArtistProfile`, based on `role`).

- **Auth:** not required.
- **Request body:**

  | Field | Type | Notes |
  |---|---|---|
  | `email` | string | required, must be a valid email |
  | `password` | string | required, min 8 characters |
  | `displayName` | string | required |
  | `role` | `"PRODUCER"` \| `"ARTIST"` | required |
  | `city` | string | optional |

- **Response body** (`UserResponse`):

  | Field | Type |
  |---|---|
  | `id` | number |
  | `email` | string |
  | `displayName` | string |
  | `role` | `"PRODUCER"` \| `"ARTIST"` |
  | `city` | string \| null |
  | `isAdmin` | boolean |
  | `createdAt` | ISO-8601 timestamp |
  | `producerProfile` | object \| null — `{ genres, bpmMin, bpmMax, experienceLevel }`, present when `role = PRODUCER` |
  | `artistProfile` | object \| null — `{ genres, bio }`, present when `role = ARTIST` |

- **Status codes:**

  | Code | When |
  |---|---|
  | 201 | User created |
  | 400 | Validation failure (blank email/password/displayName, invalid email format, password < 8 chars, missing role) or malformed JSON body |
  | 409 | `email` already registered |
  | 500 | Unexpected server error |

## POST /api/auth/login

Authenticates a user and issues a session token (24h expiry).

- **Auth:** not required.
- **Request body:**

  | Field | Type | Notes |
  |---|---|---|
  | `email` | string | required |
  | `password` | string | required |

- **Response body** (`LoginResponse`):

  | Field | Type |
  |---|---|
  | `token` | string — pass as `Authorization: Bearer <token>` on subsequent requests |
  | `userId` | number |
  | `displayName` | string |
  | `role` | `"PRODUCER"` \| `"ARTIST"` |

- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Login successful |
  | 400 | Validation failure (blank email/password) or malformed JSON body |
  | 401 | Email not found or password does not match |
  | 500 | Unexpected server error |

## GET /api/users/{id}

Fetches a user's public profile.

- **Auth:** not required.
- **Path params:** `id` — user id (number).
- **Response body:** `UserResponse` (see shape above).
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | User found |
  | 400 | `id` is not a valid number (type mismatch on the path variable) |
  | 404 | No user with that id |
  | 500 | Unexpected server error |

## PUT /api/users/{id}

Updates the caller's own profile. All body fields are optional — a field
omitted (`null`) leaves the current value unchanged; a field sent as an empty
string is rejected as invalid (it must either be omitted or non-empty).

- **Auth:** required — `Authorization: Bearer <token>`.
- **Path params:** `id` — id of the user being edited.
- **Request body** (`UpdateUserRequest`, all fields optional):

  | Field | Type | Applies to | Notes |
  |---|---|---|---|
  | `displayName` | string | any role | if present, must be non-empty |
  | `city` | string | any role | if present, must be non-empty |
  | `genres` | string | PRODUCER, ARTIST | if present, must be non-empty |
  | `bpmMin` | number | PRODUCER only | if present, must be >= 1 |
  | `bpmMax` | number | PRODUCER only | if present, must be >= 1 |
  | `experienceLevel` | string | PRODUCER only | if present, must be non-empty |
  | `bio` | string | ARTIST only | if present, must be non-empty |

- **Response body:** `UserResponse` (see shape above), reflecting the updated profile.
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Update applied |
  | 400 | A present field fails validation (empty string, `bpmMin`/`bpmMax` < 1), or malformed JSON body, or `id` is not a valid number |
  | 401 | No valid `Authorization: Bearer <token>` was sent (not authenticated) |
  | 403 | Authenticated, but `id` in the path is not the caller's own user id (editing someone else's profile) |
  | 404 | No user with that id |
  | 500 | Unexpected server error |

### 401 vs 403 on PUT /api/users/{id}

This is the one place in the module where the distinction matters: **401** means
"we don't know who you are" (missing/invalid/expired token — no `userId` request
attribute was set); **403** means "we know who you are, but you're not allowed to
do this" (authenticated as a different user than the one in the path). The 403
case genuinely stays in `UserService.update` (it needs to compare
`targetUserId` vs `requestingUserId`); the 401 case is a controller-level check
before the service is even called.
