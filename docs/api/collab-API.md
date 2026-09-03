# Collab module API

Base path: `/api`. All request/response bodies are JSON.

Auth: session-token based, same `SessionAuthInterceptor` as `users` — send
`Authorization: Bearer <token>` on protected endpoints. An absent/invalid token
means the request proceeds unauthenticated (no `userId` attribute); each
endpoint below decides for itself whether that's rejected with 401.

No DTOs: request/response bodies are the JPA entities (`Topline`,
`Collaboration`, `Comment`) directly, following the project-wide convention.
Fields the server derives itself (`Topline.artistId`, `Comment.toplineId`,
`Comment.authorId`) are `@JsonProperty(access = READ_ONLY)` — they're returned
in responses but ignored if sent in a request body, so a client can't spoof
who authored something.

**Dependency note:** this module depends on `com.mgwprod.catalog` for
`Beat`/`BeatRepository` (to validate `beatId` on a `Topline` and to resolve
`Beat.producerId` when deciding a `Collaboration`). As of this writing the
`catalog` module (Santiago+Mateo) is not yet merged to `main`, so this branch
ships against a **temporary stub** of `com.mgwprod.catalog` (see
`src/main/java/com/mgwprod/catalog/`, each file marked `TEMPORARY STUB`) —
delete that package once the real `catalog` PR merges.

## POST /api/toplines

Uploads an artist's interpretation of an existing beat. Creates the `Topline`
and, in the same transaction, an associated `Collaboration` in `PENDING`.

- **Auth:** required — caller must be an authenticated user with role `ARTIST`.
- **Request body:**

  | Field | Type | Notes |
  |---|---|---|
  | `beatId` | number | required, must reference an existing beat |
  | `audioUrl` | string | required, non-blank |

- **Response body** (`Topline`):

  | Field | Type |
  |---|---|
  | `id` | number |
  | `artistId` | number — the authenticated caller |
  | `beatId` | number |
  | `audioUrl` | string |
  | `createdAt` | ISO-8601 timestamp |

- **Status codes:**

  | Code | When |
  |---|---|
  | 201 | Topline (and its pending Collaboration) created |
  | 400 | Validation failure (blank `audioUrl`, missing `beatId`) or malformed JSON body |
  | 401 | Not authenticated |
  | 403 | Authenticated caller's role is not `ARTIST` |
  | 404 | `beatId` does not reference an existing beat |
  | 500 | Unexpected server error |

## GET /api/toplines

Lists toplines, optionally filtered.

- **Auth:** not required.
- **Query params:** `beatId` (number, optional), `artistId` (number, optional)
  — if both are omitted, returns all toplines; if `beatId` is present it takes
  precedence over `artistId`.
- **Response body:** array of `Topline` (see shape above).
- **Status codes:** `200` always (empty array if no matches).

## GET /api/toplines/{id}

Fetches a single topline.

- **Auth:** not required.
- **Path params:** `id` — topline id (number).
- **Response body:** `Topline` (see shape above).
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Topline found |
  | 404 | No topline with that id |

## PUT /api/collaborations/{id}

Accepts or rejects a pending collaboration. Only the producer who owns the
beat referenced by the collaboration's topline may decide it.

- **Auth:** required.
- **Path params:** `id` — collaboration id (number).
- **Query params:** `status` — `"ACCEPTED"` \| `"REJECTED"` (required).
- **Response body** (`Collaboration`):

  | Field | Type |
  |---|---|
  | `id` | number |
  | `toplineId` | number |
  | `status` | `"PENDING"` \| `"ACCEPTED"` \| `"REJECTED"` |
  | `decidedAt` | ISO-8601 timestamp \| null |

- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | Decision applied |
  | 400 | `status` missing/invalid, or `id` is not a valid number |
  | 401 | Not authenticated |
  | 403 | Authenticated caller does not own the beat behind this collaboration |
  | 404 | No collaboration with that id (or the topline/beat it references is missing) |
  | 500 | Unexpected server error |

## GET /api/collaborations

Lists collaborations, optionally filtered by status.

- **Auth:** not required.
- **Query params:** `status` — `"PENDING"` \| `"ACCEPTED"` \| `"REJECTED"` (optional).
- **Response body:** array of `Collaboration` (see shape above).
- **Status codes:** `200` always (empty array if no matches).

## POST /api/toplines/{toplineId}/comments

Comments on a topline.

- **Auth:** required.
- **Path params:** `toplineId` — topline id (number).
- **Request body:**

  | Field | Type | Notes |
  |---|---|---|
  | `text` | string | required, non-blank, max 1000 chars |

- **Response body** (`Comment`):

  | Field | Type |
  |---|---|
  | `id` | number |
  | `toplineId` | number |
  | `authorId` | number — the authenticated caller |
  | `text` | string |
  | `createdAt` | ISO-8601 timestamp |

- **Status codes:**

  | Code | When |
  |---|---|
  | 201 | Comment created |
  | 400 | Blank `text`, or malformed JSON body |
  | 401 | Not authenticated |
  | 404 | No topline with `toplineId` |
  | 500 | Unexpected server error |

## GET /api/toplines/{toplineId}/comments

Lists comments on a topline.

- **Auth:** not required.
- **Path params:** `toplineId` — topline id (number).
- **Response body:** array of `Comment` (see shape above).
- **Status codes:**

  | Code | When |
  |---|---|
  | 200 | (empty array if no comments) |
  | 404 | No topline with `toplineId` |
