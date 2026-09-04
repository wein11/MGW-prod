# Catalog module API

Base path: `/api`. All request/response bodies are JSON.

Auth: same session-token mechanism as `users` (see `docs/api/users-API.md`).
Send `Authorization: Bearer <token>` on protected endpoints. Unauthenticated
requests proceed without a `userId` request attribute; each endpoint below
decides for itself whether that's rejected.

Entities are returned directly (no DTOs), so `Beat`/`BeatComment` responses
below mirror the JPA entity fields exactly.

## POST /api/beats

Publishes a new beat. Only users with `role = PRODUCER` may call this.

- **Auth:** required — `Authorization: Bearer <token>`.
- **Request body:**

  | Field | Type | Notes |
  |---|---|---|
  | `title` | string | required |
  | `genre` | string | required |
  | `bpm` | number | required, >= 1 |
  | `key` | string | optional |
  | `audioUrl` | string | required — external link (SoundCloud/Drive/etc.) |

  `producerId` is not part of the request — it's set server-side from the
  authenticated `userId`, never accepted from the client.

- **Response body** (`Beat`):

  | Field | Type |
  |---|---|
  | `id` | number |
  | `producerId` | number |
  | `title` | string |
  | `genre` | string |
  | `bpm` | number |
  | `key` | string \| null |
  | `audioUrl` | string |
  | `createdAt` | ISO-8601 timestamp |

- **Status codes:**

  | Code | When |
  |---|---|
  | 201 | Beat created |
  | 400 | Validation failure (blank title/genre/audioUrl, missing/invalid bpm) or malformed JSON body |
  | 401 | No valid `Authorization: Bearer <token>` was sent (not authenticated) |
  | 403 | Authenticated, but as `ARTIST` — only producers can publish beats |
  | 500 | Unexpected server error |

## GET /api/beats

Lists beats, optionally filtered.

- **Auth:** not required.
- **Query params (all optional):**

  | Param | Type | Notes |
  |---|---|---|
  | `genre` | string | exact match |
  | `bpm` | number | exact match |
  | `producerId` | number | if present, takes priority over `genre`/`bpm` (returns all beats from that producer) |

- **Response body:** array of `Beat` (see shape above).
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Always (empty array if no matches) |
  | 400 | A query param has the wrong type (e.g. `bpm=abc`) |
  | 500 | Unexpected server error |

## GET /api/beats/{id}

Fetches a single beat's detail.

- **Auth:** not required.
- **Path params:** `id` — beat id (number).
- **Response body:** `Beat` (see shape above).
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Beat found |
  | 400 | `id` is not a valid number |
  | 404 | No beat with that id |
  | 500 | Unexpected server error |

## POST /api/beats/{beatId}/comments

Adds a comment to a beat.

- **Auth:** required — `Authorization: Bearer <token>`.
- **Path params:** `beatId` — beat being commented on.
- **Request body:**

  | Field | Type | Notes |
  |---|---|---|
  | `text` | string | required, non-blank |

  `beatId` and `authorId` are not part of the request — `beatId` comes from
  the path, `authorId` is set server-side from the authenticated `userId`.

- **Response body** (`BeatComment`):

  | Field | Type |
  |---|---|
  | `id` | number |
  | `beatId` | number |
  | `authorId` | number |
  | `text` | string |
  | `createdAt` | ISO-8601 timestamp |

- **Status codes:**

  | Code | When |
  |---|---|
  | 201 | Comment created |
  | 400 | Blank `text` or malformed JSON body |
  | 401 | No valid `Authorization: Bearer <token>` was sent (not authenticated) |
  | 404 | No beat with `beatId` |
  | 500 | Unexpected server error |

## GET /api/beats/{beatId}/comments

Lists all comments on a beat.

- **Auth:** not required.
- **Path params:** `beatId` — beat whose comments are listed.
- **Response body:** array of `BeatComment` (see shape above).
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Always (empty array if no comments) |
  | 404 | No beat with `beatId` |
  | 500 | Unexpected server error |
