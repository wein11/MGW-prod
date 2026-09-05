# Users module API

Base path: `/api`. All request/response bodies are JSON.

Auth: session-token based. `POST /api/auth/login` returns a `token`. Send it on
protected endpoints as `Authorization: Bearer <token>`. `SessionAuthInterceptor`
reads this header on every request; if it's absent, the request proceeds
unauthenticated (no `userId` attribute set) rather than being rejected at the
interceptor level — each endpoint decides for itself whether auth is required.

## POST /api/auth/register

Creates a new user (and its `ArtistProfile`, only when `role = ARTIST`).

- **Auth:** not required.
- **Request body:**

  | Field | Type | Notes |
  |---|---|---|
  | `email` | string | required, must be a valid email |
  | `password` | string | required, min 8 characters |
  | `displayName` | string | required |
  | `role` | `"ARTIST"` \| `"DISCOGRAFICA"` \| `"ADMIN"` | required |
  | `city` | string | optional |

- **Response body** (`UserResponse`):

  | Field | Type |
  |---|---|
  | `id` | number |
  | `email` | string |
  | `displayName` | string |
  | `role` | `"ARTIST"` \| `"DISCOGRAFICA"` \| `"ADMIN"` |
  | `city` | string \| null |
  | `createdAt` | ISO-8601 timestamp |

  No hay campo `isAdmin` — `ADMIN` es un valor más de `role`, no un flag separado. El
  perfil (`ArtistProfile`) no viaja en esta respuesta; se consulta aparte con
  `GET /api/users/{id}/profile`.

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
  | `role` | `"ARTIST"` \| `"DISCOGRAFICA"` \| `"ADMIN"` |

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

  Profile fields (`genres`, `bio`, `bpmMin`, `bpmMax`, `experienceLevel`) are not part
  of this endpoint — see `PUT /api/users/{id}/artist-profile` below.

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

## GET /api/users/{id}/profile

Fetches a user's `ArtistProfile`. Only `role = ARTIST` users have one — every
other role gets a 403 (there is no `DISCOGRAFICA`/`ADMIN` profile in this
entrega).

- **Auth:** not required.
- **Path params:** `id` — user id.
- **Response body** (`ArtistProfile`): `{ id, genres, bio, bpmMin, bpmMax, experienceLevel, verified }`.
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Profile found |
  | 403 | The user exists but is not `ARTIST` |
  | 404 | No user with that id |
  | 500 | Unexpected server error |

## PUT /api/users/{id}/artist-profile

Updates the caller's own `ArtistProfile`. All body fields are optional and
merge into the existing profile (`null`/omitted leaves the field unchanged).

- **Auth:** required — `Authorization: Bearer <token>`.
- **Path params:** `id` — id of the user whose profile is being edited (must
  match the caller).
- **Request body** (all fields optional):

  | Field | Type | Notes |
  |---|---|---|
  | `genres` | string | if present, must be non-empty |
  | `bio` | string | if present, must be non-empty |
  | `bpmMin` | number | if present, must be >= 1 |
  | `bpmMax` | number | if present, must be >= 1 |
  | `experienceLevel` | string | if present, must be non-empty |

- **Response body:** the updated `ArtistProfile`.
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Update applied |
  | 401 | Not authenticated |
  | 403 | Editing someone else's profile, or the target user is not `ARTIST` |
  | 404 | No user with that id |
  | 500 | Unexpected server error |

## PUT /api/artists/{id}/verify

Marks an artist's `ArtistProfile` as verified. Admin-only (replaces the old
"verify producer" endpoint — every artist is verifiable now, not just
producers).

- **Auth:** required — `Authorization: Bearer <token>`, caller must be `role = ADMIN`.
- **Path params:** `id` — id of the artist to verify.
- **Response body:** the updated `ArtistProfile` (`verified: true`).
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Verified |
  | 401 | Not authenticated |
  | 403 | Caller is not `ADMIN`, or the target user is not `ARTIST` |
  | 404 | No user with that id |
  | 500 | Unexpected server error |
