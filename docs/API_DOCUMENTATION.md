# API Documentation

## wrenchy.in — Public REST API · Core Services (v1)

| Field | Value |
|---|---|
| Document Type | REST API Documentation (OpenAPI / Swagger style, hand-written) |
| Companion To | SRS v1.0, System Design v1.0, Database Design v1.0 |
| Version | 1.0 |
| Status | Draft for review |
| Coverage | Auth · Booking (Job) · Parts |
| Author | [ADD: Author Name] |
| Last Updated | [ADD: YYYY-MM-DD] |

---

## Table of Contents

1. Conventions
2. Authentication
3. Standard Error Format and Status Codes
4. Pagination, Sorting, and Filtering
5. Synchronous vs. Asynchronous Endpoints
6. Auth Service (`/api/v1/auth/*`)
7. Booking Service (`/api/v1/jobs/*`, `/public`)
8. Parts Service (`/api/v1/parts/*`)
9. Auto-Generated Spec and Source-of-Truth Note
10. Appendix: Shared Schemas

---

## 1. Conventions

### 1.1 Base URL

| Environment | Base URL |
|---|---|
| Production | `https://api.wrenchy.in` |
| Local development | `http://localhost:8080` (gateway port) |

All paths below are relative to the base URL.

### 1.2 Versioning

API version is part of the URL prefix: `/api/v1/...`. Breaking changes increment the prefix to `/api/v2`. Non-breaking additions (new optional fields, new endpoints) ship under the same version.

**[FACT]** The webhook path (`/api/v1/payments/webhook/razorpay`) and the public booking path (`/public`) are exceptions and are documented in their respective service sections.

### 1.3 Content Type and Encoding

- `Content-Type: application/json; charset=utf-8` for all request bodies (except photo upload, which uses `multipart/form-data`).
- `Accept: application/json` for all responses.
- All string fields are UTF-8.
- All date-time fields use ISO 8601 with explicit time zone, normalised to UTC by the server (`2026-06-01T18:23:01.234Z`).
- Money fields are JSON numbers with two decimal places (e.g., `250.00`).
- Phone numbers are 10-digit strings, no country code, no leading `+`.
- UUIDs are lowercase, hyphenated strings (`8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c`).

### 1.4 Required Headers

| Header | When | Example |
|---|---|---|
| `Authorization` | All authenticated endpoints | `Bearer eyJhbGciOiJIUzI1...` |
| `Content-Type` | All endpoints with a JSON body | `application/json` |
| `Idempotency-Key` | Optional, only on `POST /public` and `POST /api/v1/jobs/{id}/parts` | Any UUID v4. |
| `X-Request-Id` | Optional client-supplied trace id; server generates if absent | `8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c` |

---

## 2. Authentication

### 2.1 Scheme

All authenticated endpoints expect a JWT access token in the `Authorization` header.

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI...
```

JWTs are HMAC-SHA256 signed by `auth-service`. Validation happens at the API gateway (per FR-AUTH-09); downstream services trust the gateway-injected `X-User-Id` and `X-User-Role` headers.

### 2.2 Token Lifecycle

| Token | Lifetime | Storage Recommendation |
|---|---|---|
| Access token | 30 minutes | In-memory (JavaScript) or short-lived secure storage. |
| Refresh token | 30 days | HTTP-only cookie or secure storage. |

Tokens are issued by `POST /api/v1/auth/otp/verify` and rotated by `POST /api/v1/auth/refresh`.

### 2.3 Roles

| Role | Meaning |
|---|---|
| `CUSTOMER` | An end-user who booked a service. Can read own jobs, file own complaints. |
| `TECHNICIAN` | A field worker. Can read assigned jobs, advance status, log parts. |
| `ADMIN` | Shop staff. Can assign technicians, manage inventory, resolve complaints. |
| `OWNER` | Super-administrator. Full access. |

### 2.4 Whitelisted (No-Auth) Paths

| Path | Why |
|---|---|
| `POST /api/v1/auth/otp/send` | Pre-authentication. |
| `POST /api/v1/auth/otp/verify` | Pre-authentication. |
| `POST /api/v1/auth/refresh` | Refresh token in body, not Authorization header. |
| `POST /public` | Public booking — frictionless entry. |
| `POST /api/v1/payments/webhook/razorpay` | HMAC-verified, not JWT. |
| `GET /actuator/health` | Liveness probe. |

All other paths require a valid JWT.

---

## 3. Standard Error Format and Status Codes

### 3.1 Error Response Schema

Every non-2xx response uses this exact envelope:

```json
{
  "timestamp": "2026-06-01T18:23:01.234Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_PHONE_FORMAT",
  "message": "Phone number must be exactly 10 digits.",
  "path": "/api/v1/auth/otp/send",
  "requestId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "details": {
    "field": "phone",
    "value": "98765"
  }
}
```

| Field | Type | Notes |
|---|---|---|
| `timestamp` | string | ISO 8601 UTC. |
| `status` | integer | HTTP status code, repeated for convenience. |
| `error` | string | HTTP reason phrase. |
| `code` | string | Machine-readable error code in SCREAMING_SNAKE_CASE. The contract for clients. |
| `message` | string | Human-readable explanation. May change between versions; do not parse. |
| `path` | string | Originally-requested path. |
| `requestId` | string | Same as the response `X-Request-Id` header. Use this in support tickets. |
| `details` | object | Optional. Field-level context for validation errors. |

### 3.2 Status Codes Used

| Code | Meaning | When |
|---|---|---|
| `200 OK` | Read or update succeeded. | |
| `201 Created` | New resource created. `Location` header points to it. | |
| `204 No Content` | Action succeeded, no body. | Logout, delete. |
| `400 Bad Request` | Malformed request or schema validation failed. | |
| `401 Unauthorized` | JWT missing, expired, or invalid. | |
| `403 Forbidden` | Authenticated but wrong role for this resource. | |
| `404 Not Found` | Resource does not exist or the caller cannot see it. | |
| `409 Conflict` | State-machine violation, duplicate resource, or insufficient stock. | |
| `422 Unprocessable Entity` | Syntactically valid but semantically rejected. | |
| `429 Too Many Requests` | Rate limit hit. `Retry-After` header indicates wait. | |
| `500 Internal Server Error` | Unhandled server failure. Body still uses the standard envelope. | |
| `503 Service Unavailable` | A downstream dependency is unhealthy. | |

### 3.3 Common Error Codes

| Code | Status | Meaning |
|---|---|---|
| `INVALID_PHONE_FORMAT` | 400 | Phone is not exactly 10 digits. |
| `INVALID_OTP` | 401 | OTP wrong or expired. |
| `OTP_TOO_MANY_ATTEMPTS` | 429 | More than 3 OTPs requested in 15 minutes. |
| `TOKEN_EXPIRED` | 401 | JWT past `exp`. |
| `TOKEN_REVOKED` | 401 | Refresh token in revocation list. |
| `INSUFFICIENT_ROLE` | 403 | Caller's role does not permit the action. |
| `JOB_NOT_FOUND` | 404 | Job id unknown or not visible to caller. |
| `INVALID_STATE_TRANSITION` | 409 | The proposed `from → to` is not in the state machine. |
| `INSUFFICIENT_STOCK` | 409 | Requested quantity exceeds current stock. |
| `SKU_NOT_FOUND` | 404 | Part does not exist. |
| `VALIDATION_FAILED` | 422 | One or more fields failed semantic validation; `details` lists them. |
| `RATE_LIMITED` | 429 | Generic rate limit. |

---

## 4. Pagination, Sorting, and Filtering

### 4.1 Pagination

List endpoints accept:

| Parameter | Default | Max | Meaning |
|---|---|---|---|
| `page` | 0 | — | Zero-indexed page number. |
| `size` | 20 | 100 | Items per page. |

Response envelope:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "isFirst": true,
  "isLast": false
}
```

### 4.2 Sorting

`sort=<field>,<direction>`. Multiple sorts permitted by repeating the parameter.

Example: `?sort=createdAt,desc&sort=status,asc`

### 4.3 Filtering

Each list endpoint documents its supported filter parameters. Filters are AND-combined.

---

## 5. Synchronous vs. Asynchronous Endpoints

Each endpoint below is tagged in its header:

- **Sync** — request completes when the response returns.
- **Sync + async fan-out** — request returns 2xx immediately; one or more Kafka events drive downstream work (notifications, BI projections). Documented per endpoint.

**[FACT]** Asynchronous tails never delay the request response. If a Kafka publish fails, the inbound request still succeeds and the failure is logged for replay.

---

## 6. Auth Service

**Base path:** `/api/v1/auth`

---

### 6.1 `POST /api/v1/auth/otp/send`

**Purpose.** Issue an OTP to a phone number.
**Auth.** None (whitelisted).
**Kind.** Sync.

#### Request body

```json
{
  "phone": "9876543210",
  "purpose": "LOGIN"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `phone` | string | yes | 10-digit Indian mobile. |
| `purpose` | string | yes | One of `LOGIN`, `RECOVERY`. |

#### Success — `200 OK`

```json
{
  "sent": true,
  "expiresInSeconds": 300
}
```

#### Errors

| Status | Code | Meaning |
|---|---|---|
| 400 | `INVALID_PHONE_FORMAT` | Phone is not 10 digits. |
| 429 | `OTP_TOO_MANY_ATTEMPTS` | Per-phone rate limit (3 per 15 min) or per-IP limit. |
| 503 | `OTP_VENDOR_UNAVAILABLE` | 2Factor.in returned 5xx. |

**[INFERENCE]** The endpoint deliberately returns `sent: true` even if the phone is not registered, to avoid disclosing which numbers are users.

---

### 6.2 `POST /api/v1/auth/otp/verify`

**Purpose.** Exchange a phone + OTP for an access token and refresh token.
**Auth.** None (whitelisted).
**Kind.** Sync.

#### Request body

```json
{
  "phone": "9876543210",
  "otp": "473819",
  "purpose": "LOGIN"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `phone` | string | yes | 10-digit. |
| `otp` | string | yes | 6 digits. |
| `purpose` | string | yes | Must match the `purpose` sent at issuance. |

#### Success — `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "rt_8f3a1b2c4d5e6f7a8b9c0d1e2f3a4b5c",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": {
    "id": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
    "phone": "9876543210",
    "role": "CUSTOMER",
    "isActive": true,
    "createdAt": "2026-05-12T09:14:01Z"
  }
}
```

#### Errors

| Status | Code |
|---|---|
| 400 | `INVALID_PHONE_FORMAT`, `VALIDATION_FAILED` |
| 401 | `INVALID_OTP` (wrong code or expired) |
| 429 | `RATE_LIMITED` |

---

### 6.3 `POST /api/v1/auth/refresh`

**Purpose.** Trade a valid refresh token for a fresh access token and a rotated refresh token.
**Auth.** None (whitelisted — refresh token is in body).
**Kind.** Sync.

#### Request body

```json
{
  "refreshToken": "rt_8f3a1b2c4d5e6f7a8b9c0d1e2f3a4b5c"
}
```

#### Success — `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "rt_9a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

**[INFERENCE]** The old refresh token is invalidated server-side as part of the rotation. Clients must replace their stored token.

#### Errors

| Status | Code |
|---|---|
| 401 | `TOKEN_EXPIRED`, `TOKEN_REVOKED` |
| 400 | `VALIDATION_FAILED` |

---

### 6.4 `POST /api/v1/auth/logout`

**Purpose.** Revoke a refresh token (and add it to the Redis blacklist).
**Auth.** Required. Any role.
**Kind.** Sync.

#### Request body

```json
{
  "refreshToken": "rt_8f3a1b2c4d5e6f7a8b9c0d1e2f3a4b5c"
}
```

#### Success — `204 No Content`

(No body.)

#### Errors

| Status | Code |
|---|---|
| 401 | `TOKEN_EXPIRED`, missing JWT |

---

### 6.5 `GET /api/v1/auth/me`

**Purpose.** Return the currently authenticated user.
**Auth.** Required. Any role.
**Kind.** Sync.

#### Success — `200 OK`

```json
{
  "id": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "phone": "9876543210",
  "role": "CUSTOMER",
  "isActive": true,
  "createdAt": "2026-05-12T09:14:01Z",
  "lastLoginAt": "2026-06-01T18:22:14Z"
}
```

#### Errors

| Status | Code |
|---|---|
| 401 | JWT missing or invalid |

---

## 7. Booking Service

**Base paths:** `/public` (anonymous) and `/api/v1/jobs/*` (authenticated).

The booking service is the largest surface. Endpoints are grouped by purpose.

---

### 7.1 `POST /public`

**Purpose.** Anonymous booking — the public entry point.
**Auth.** None (whitelisted).
**Kind.** Sync + async fan-out.
**Async effects.** Publishes `job.created` to Kafka → `notification-service` dispatches admin SMS / WhatsApp.

#### Request body

```json
{
  "name": "Anita Verma",
  "phone": "9876543210",
  "appliance": "AC",
  "applianceBrand": "Voltas",
  "problem": "Cooling stopped working since yesterday evening.",
  "address": {
    "line1": "House 12, Civil Lines",
    "landmark": "Opposite SBI",
    "city": "Banda",
    "pincode": "210001"
  },
  "scheduledAt": "2026-06-02T10:00:00+05:30"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | yes | 2–80 chars. |
| `phone` | string | yes | 10 digits. |
| `appliance` | string | yes | One of `AC`, `FRIDGE`, `WASHING_MACHINE`, `RO`, `WATER_HEATER`, `OTHER`. |
| `applianceBrand` | string | no | |
| `problem` | string | yes | 5–500 chars. |
| `address.line1` | string | yes | |
| `address.landmark` | string | no | |
| `address.city` | string | no | Defaults to `Banda`. |
| `address.pincode` | string | yes | 6 digits. |
| `scheduledAt` | string | no | ISO 8601. If absent, treated as "as soon as possible". |

#### Success — `201 Created`

```json
{
  "id": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "referenceCode": "WR9X4K",
  "status": "REQUESTED",
  "createdAt": "2026-06-01T18:23:01.234Z"
}
```

`Location: /api/v1/jobs/8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c`

#### Errors

| Status | Code |
|---|---|
| 400 | `INVALID_PHONE_FORMAT`, `VALIDATION_FAILED` |
| 429 | `RATE_LIMITED` (5 per phone per hour; 100 per IP per hour) |

---

### 7.2 `POST /api/v1/jobs`

**Purpose.** Admin-created booking with explicit customer reference.
**Auth.** Required. `ADMIN` or `OWNER`.
**Kind.** Sync + async fan-out.

#### Request body

Same shape as `POST /public`, plus the optional fields below:

```json
{
  "customerId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "name": "Anita Verma",
  "phone": "9876543210",
  "appliance": "AC",
  "problem": "AC not cooling",
  "address": { ... },
  "scheduledAt": "2026-06-02T10:00:00+05:30",
  "internalNote": "Repeat customer — second time this season."
}
```

#### Success — `201 Created`

```json
{
  "id": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "referenceCode": "WR9X4K",
  "status": "REQUESTED",
  "customerId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "customerPhone": "9876543210",
  "createdAt": "2026-06-01T18:23:01.234Z"
}
```

#### Errors

| Status | Code |
|---|---|
| 401 | JWT missing |
| 403 | `INSUFFICIENT_ROLE` |
| 400 | `VALIDATION_FAILED` |
| 404 | `CUSTOMER_NOT_FOUND` (if `customerId` supplied and unknown) |

---

### 7.3 `GET /api/v1/jobs`

**Purpose.** List jobs with filtering and pagination.
**Auth.** Required. `ADMIN` / `OWNER` see all. `TECHNICIAN` is scoped server-side to their own assignments. `CUSTOMER` is scoped to their own phone.
**Kind.** Sync.

#### Query parameters

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `status` | string | no | One of the 8 status values. Repeat for OR. |
| `customerPhone` | string | no | Admin-only filter. |
| `technicianId` | string | no | Admin-only filter. |
| `from` | string | no | ISO 8601. `createdAt >=`. |
| `to` | string | no | ISO 8601. `createdAt <`. |
| `page` | int | no | Default 0. |
| `size` | int | no | Default 20, max 100. |
| `sort` | string | no | Default `createdAt,desc`. |

#### Success — `200 OK`

```json
{
  "content": [
    {
      "id": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
      "referenceCode": "WR9X4K",
      "status": "ASSIGNED",
      "customerPhone": "9876543210",
      "customerName": "Anita Verma",
      "appliance": "AC",
      "problem": "AC not cooling",
      "primaryTechId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "scheduledAt": "2026-06-02T10:00:00+05:30",
      "totalAmount": 0.00,
      "isPaid": false,
      "createdAt": "2026-06-01T18:23:01.234Z",
      "updatedAt": "2026-06-01T18:45:12.001Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "isFirst": true,
  "isLast": false
}
```

#### Errors

| Status | Code |
|---|---|
| 401 | JWT missing |
| 400 | `VALIDATION_FAILED` |

---

### 7.4 `GET /api/v1/jobs/my`

**Purpose.** Convenience endpoint for technicians — their own jobs only.
**Auth.** Required. `TECHNICIAN`.
**Kind.** Sync.

#### Success — `200 OK`

Same envelope as `GET /api/v1/jobs`, scoped to `primary_tech_id = me OR assistant_tech_id = me`.

---

### 7.5 `GET /api/v1/jobs/{id}`

**Purpose.** Fetch a single job with full embedded detail.
**Auth.** Required. `ADMIN`, `OWNER`, assigned `TECHNICIAN`, or owning `CUSTOMER`.
**Kind.** Sync.

#### Path parameters

| Parameter | Type | Notes |
|---|---|---|
| `id` | UUID | The job UUID, **or** the 6-char `referenceCode`. |

#### Success — `200 OK`

```json
{
  "id": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "referenceCode": "WR9X4K",
  "status": "IN_PROGRESS",
  "customerId": "9c0d1e2f-3a4b-5c6d-7e8f-9a0b1c2d3e4f",
  "customerPhone": "9876543210",
  "customerNameSnapshot": "Anita Verma",
  "appliance": "AC",
  "applianceBrand": "Voltas",
  "problem": "Cooling stopped working since yesterday.",
  "address": {
    "line1": "House 12, Civil Lines",
    "landmark": "Opposite SBI",
    "city": "Banda",
    "pincode": "210001"
  },
  "scheduledAt": "2026-06-02T10:00:00+05:30",
  "primaryTech": {
    "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "name": "Ramesh Kumar",
    "primaryPhone": "9123456789",
    "trustScore": 84
  },
  "assistantTech": null,
  "serviceCharge": 0.00,
  "totalAmount": 0.00,
  "isPaid": false,
  "isGuaranteeService": false,
  "parentJobId": null,
  "guaranteeExpiresAt": null,
  "partsUsed": [
    {
      "id": "f1a2b3c4-d5e6-7a8b-9c0d-1e2f3a4b5c6d",
      "sku": "VOLTAS-AC-CAPACITOR-25MFD",
      "name": "Voltas AC Running Capacitor 25 MFD",
      "quantity": 1,
      "unitPriceAtUse": 220.00,
      "createdAt": "2026-06-02T10:42:00Z"
    }
  ],
  "statusHistory": [
    { "fromStatus": null, "toStatus": "REQUESTED", "actorRole": "SYSTEM", "createdAt": "2026-06-01T18:23:01Z" },
    { "fromStatus": "REQUESTED", "toStatus": "ASSIGNED", "actorRole": "ADMIN", "createdAt": "2026-06-01T18:45:12Z" },
    { "fromStatus": "ASSIGNED", "toStatus": "IN_TRANSIT", "actorRole": "TECHNICIAN", "createdAt": "2026-06-02T09:30:00Z" },
    { "fromStatus": "IN_TRANSIT", "toStatus": "AT_CUSTOMER", "actorRole": "TECHNICIAN", "createdAt": "2026-06-02T10:05:00Z" },
    { "fromStatus": "AT_CUSTOMER", "toStatus": "IN_PROGRESS", "actorRole": "TECHNICIAN", "createdAt": "2026-06-02T10:10:00Z" }
  ],
  "photos": [
    {
      "id": "...",
      "kind": "BEFORE",
      "cloudinarySecureUrl": "https://res.cloudinary.com/wrenchy/image/upload/...",
      "uploadedAt": "2026-06-02T10:08:00Z"
    }
  ],
  "createdAt": "2026-06-01T18:23:01Z",
  "updatedAt": "2026-06-02T10:42:00Z"
}
```

#### Errors

| Status | Code |
|---|---|
| 401 | JWT missing |
| 403 | `INSUFFICIENT_ROLE` |
| 404 | `JOB_NOT_FOUND` |

---

### 7.6 `PUT /api/v1/jobs/{id}/status`

**Purpose.** Advance a job through the state machine.
**Auth.** Required.
 - `TECHNICIAN` may transition between `ASSIGNED → IN_TRANSIT → AT_CUSTOMER → IN_PROGRESS → PARTS_NEEDED → COMPLETED`.
 - `ADMIN` / `OWNER` may set any transition, including `CANCELLED`.
**Kind.** Sync + async fan-out.
**Async effects.** Publishes `job.status.changed`; on `COMPLETED` also computes `guarantee_expires_at` and `total_amount`.

#### Request body

```json
{
  "status": "IN_TRANSIT",
  "reason": "Left shop with parts."
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `status` | string | yes | Target status. |
| `reason` | string | no | Required for `CANCELLED`. |

#### Success — `200 OK`

Returns the updated job in the same shape as `GET /api/v1/jobs/{id}`.

#### Errors

| Status | Code |
|---|---|
| 401 | JWT missing |
| 403 | `INSUFFICIENT_ROLE` for the proposed transition |
| 404 | `JOB_NOT_FOUND` |
| 409 | `INVALID_STATE_TRANSITION` — proposed `from → to` not allowed. |

---

### 7.7 `POST /api/v1/jobs/{id}/assign`

**Purpose.** Manually assign a primary technician (and optional assistant).
**Auth.** Required. `ADMIN` or `OWNER`.
**Kind.** Sync + async fan-out.
**Async effects.** Publishes `job.assigned` → notification-service sends the technician their assignment via SMS / WhatsApp.

#### Request body

```json
{
  "primaryTechId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "assistantTechId": null
}
```

#### Success — `200 OK`

Returns the updated job. Job status transitions from `REQUESTED` → `ASSIGNED` as part of the same operation.

#### Errors

| Status | Code |
|---|---|
| 404 | `JOB_NOT_FOUND`, `TECHNICIAN_NOT_FOUND` |
| 409 | `INVALID_STATE_TRANSITION`, `TECHNICIAN_INACTIVE` |

---

### 7.8 `POST /api/v1/jobs/{id}/auto-assign`

**Purpose.** Let the system pick the best technician by trust score, skills, and service area.
**Auth.** Required. `ADMIN` or `OWNER`.
**Kind.** Sync + async fan-out.
**Async effects.** Same as `assign`.

#### Request body

(No body.)

#### Success — `200 OK`

```json
{
  "selectedTechnicianId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "selectedTechnicianName": "Ramesh Kumar",
  "selectionReason": "Highest trust score (84) among active technicians serving pincode 210001 with AC skill, least-recently-assigned tiebreak.",
  "job": { ...full job object as in GET /jobs/{id}... }
}
```

#### Errors

| Status | Code |
|---|---|
| 404 | `JOB_NOT_FOUND` |
| 409 | `NO_TECHNICIAN_AVAILABLE` — no active tech matches skill + area. |

---

### 7.9 `POST /api/v1/jobs/{id}/parts`

**Purpose.** Record a spare part consumed during the job. Synchronously calls `parts-service` to decrement stock.
**Auth.** Required. Assigned `TECHNICIAN`, `ADMIN`, or `OWNER`.
**Kind.** Sync (two-service transaction). Optionally idempotent via `Idempotency-Key` header.
**Async effects.** If post-decrement stock falls at or below threshold, `parts-service` emits `inventory.reorder-alert`.

#### Request body

```json
{
  "sku": "VOLTAS-AC-CAPACITOR-25MFD",
  "quantity": 1
}
```

#### Success — `201 Created`

```json
{
  "id": "f1a2b3c4-d5e6-7a8b-9c0d-1e2f3a4b5c6d",
  "jobId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "sku": "VOLTAS-AC-CAPACITOR-25MFD",
  "name": "Voltas AC Running Capacitor 25 MFD",
  "quantity": 1,
  "unitPriceAtUse": 220.00,
  "stockRemaining": 4,
  "createdAt": "2026-06-02T10:42:00Z"
}
```

#### Errors

| Status | Code |
|---|---|
| 404 | `JOB_NOT_FOUND`, `SKU_NOT_FOUND` |
| 409 | `INSUFFICIENT_STOCK` (with `available` in `details`), `JOB_NOT_IN_PROGRESS` |
| 403 | `INSUFFICIENT_ROLE` (only assigned tech may log) |

---

### 7.10 `POST /api/v1/jobs/{id}/complaint`

**Purpose.** File a complaint against a completed job.
**Auth.** Required. Owning `CUSTOMER`, or `ADMIN`.
**Kind.** Sync + async fan-out.
**Async effects.** Notifies admin; if category is `RECURRENCE` and within guarantee window, creates a linked re-service job (status `REQUESTED`, `is_guarantee_service = true`, `service_charge = 0`); decrements technician trust score.

#### Request body

```json
{
  "category": "RECURRENCE",
  "description": "AC stopped cooling again on 2026-06-09, six days after the original repair."
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `category` | string | yes | One of `RECURRENCE`, `BEHAVIOR`, `PRICING`, `PUNCTUALITY`, `OTHER`. |
| `description` | string | yes | 10–1000 chars. |

#### Success — `201 Created`

```json
{
  "id": "c1a2b3c4-d5e6-7a8b-9c0d-1e2f3a4b5c6d",
  "jobId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "category": "RECURRENCE",
  "description": "AC stopped cooling again ...",
  "status": "OPEN",
  "triggeredGuaranteeJobId": "9a8b7c6d-5e4f-3a2b-1c0d-1e2f3a4b5c6d",
  "createdAt": "2026-06-09T11:14:00Z"
}
```

#### Errors

| Status | Code |
|---|---|
| 404 | `JOB_NOT_FOUND` |
| 409 | `JOB_NOT_COMPLETED` (cannot complain about a job that is not completed) |
| 422 | `VALIDATION_FAILED` |

---

### 7.11 `POST /api/v1/jobs/{id}/photos`

**Purpose.** Upload a before- or after-repair photograph.
**Auth.** Required. Assigned `TECHNICIAN` or `ADMIN`.
**Kind.** Sync (proxies to Cloudinary).
**Content-Type.** `multipart/form-data`.

#### Form fields

| Field | Type | Notes |
|---|---|---|
| `kind` | string | `BEFORE` or `AFTER`. |
| `file` | binary | JPEG or PNG, ≤ 5 MB. |

#### Success — `201 Created`

```json
{
  "id": "p1a2b3c4-d5e6-7a8b-9c0d-1e2f3a4b5c6d",
  "kind": "BEFORE",
  "cloudinarySecureUrl": "https://res.cloudinary.com/wrenchy/image/upload/v.../before.jpg",
  "uploadedAt": "2026-06-02T10:08:00Z"
}
```

#### Errors

| Status | Code |
|---|---|
| 413 | `PAYLOAD_TOO_LARGE` |
| 415 | `UNSUPPORTED_MEDIA_TYPE` |
| 422 | `VALIDATION_FAILED` |

---

### 7.12 `GET /api/v1/jobs/{id}/invoice`

**Purpose.** Fetch invoice metadata and a signed PDF URL.
**Auth.** Required. Owning `CUSTOMER`, `ADMIN`, or `OWNER`.
**Kind.** Sync.

#### Success — `200 OK`

```json
{
  "invoiceNumber": "WR-2026-001234",
  "jobId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
  "totalAmount": 470.00,
  "gstin": null,
  "pdfUrl": "https://res.cloudinary.com/wrenchy/raw/upload/v.../invoice.pdf",
  "generatedAt": "2026-06-02T11:15:00Z"
}
```

#### Errors

| Status | Code |
|---|---|
| 404 | `JOB_NOT_FOUND`, `INVOICE_NOT_GENERATED` (job not yet completed + paid) |

---

## 8. Parts Service

**Base path:** `/api/v1/parts/*`

---

### 8.1 `GET /api/v1/parts`

**Purpose.** Search and list the spare-parts catalog.
**Auth.** Required. Any role.
**Kind.** Sync.

#### Query parameters

| Parameter | Type | Required | Notes |
|---|---|---|---|
| `q` | string | no | Fuzzy search on name and SKU (`pg_trgm` backed). |
| `category` | int | no | Category id. |
| `brand` | string | no | Exact match. |
| `modelCompatibility` | string | no | "Find parts compatible with model X". |
| `inStockOnly` | bool | no | Default `false`. |
| `page` | int | no | Default 0. |
| `size` | int | no | Default 20, max 100. |
| `sort` | string | no | Default `name,asc`. |

#### Success — `200 OK`

```json
{
  "content": [
    {
      "sku": "VOLTAS-AC-CAPACITOR-25MFD",
      "name": "Voltas AC Running Capacitor 25 MFD",
      "categoryId": 14,
      "brand": "Voltas",
      "modelCompatibility": ["183DZX", "183DZZ", "183IZI"],
      "unitPrice": 220.00,
      "currentStock": 4,
      "reorderThreshold": 2,
      "isActive": true,
      "createdAt": "2026-04-01T11:00:00Z",
      "updatedAt": "2026-06-02T10:42:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 312,
  "totalPages": 16,
  "isFirst": true,
  "isLast": false
}
```

---

### 8.2 `GET /api/v1/parts/{sku}`

**Purpose.** Fetch a single part by SKU.
**Auth.** Required. Any role.
**Kind.** Sync.

#### Success — `200 OK`

Same shape as a single element in `GET /api/v1/parts.content[]`.

#### Errors

| Status | Code |
|---|---|
| 404 | `SKU_NOT_FOUND` |

---

### 8.3 `POST /api/v1/parts`

**Purpose.** Create a new SKU.
**Auth.** Required. `ADMIN` or `OWNER`.
**Kind.** Sync.

#### Request body

```json
{
  "sku": "VOLTAS-AC-CAPACITOR-25MFD",
  "name": "Voltas AC Running Capacitor 25 MFD",
  "categoryId": 14,
  "brand": "Voltas",
  "modelCompatibility": ["183DZX", "183DZZ"],
  "unitPrice": 220.00,
  "currentStock": 6,
  "reorderThreshold": 2,
  "supplierId": 3
}
```

#### Success — `201 Created`

Returns the created part.

#### Errors

| Status | Code |
|---|---|
| 409 | `SKU_ALREADY_EXISTS` |
| 422 | `VALIDATION_FAILED` |

---

### 8.4 `PUT /api/v1/parts/{sku}`

**Purpose.** Update mutable fields of a part (name, price, threshold, model compatibility, active flag).
**Auth.** Required. `ADMIN` or `OWNER`.
**Kind.** Sync.

#### Request body

```json
{
  "name": "Voltas AC Running Capacitor 25 MFD (revised)",
  "unitPrice": 240.00,
  "reorderThreshold": 3,
  "isActive": true
}
```

All fields optional; only fields present are updated.

#### Success — `200 OK`

Returns the updated part.

#### Errors

| Status | Code |
|---|---|
| 404 | `SKU_NOT_FOUND` |
| 422 | `VALIDATION_FAILED` |

---

### 8.5 `POST /api/v1/parts/{sku}/restock`

**Purpose.** Increase stock for a SKU and log a stock movement.
**Auth.** Required. `ADMIN` or `OWNER`.
**Kind.** Sync.

#### Request body

```json
{
  "quantity": 12,
  "note": "Purchase order PO-2026-021",
  "supplierId": 3
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `quantity` | int | yes | Positive. |
| `note` | string | no | Persisted on the stock movement row. |
| `supplierId` | int | no | |

#### Success — `200 OK`

```json
{
  "sku": "VOLTAS-AC-CAPACITOR-25MFD",
  "newStock": 16,
  "stockMovementId": 84211,
  "createdAt": "2026-06-02T08:00:00Z"
}
```

#### Errors

| Status | Code |
|---|---|
| 404 | `SKU_NOT_FOUND` |
| 422 | `VALIDATION_FAILED` |

---

### 8.6 `POST /api/v1/parts/{sku}/consume`

**Purpose.** Internal endpoint called by `job-service` (via Feign) when a technician logs a part. Atomically decrements stock and writes a `CONSUME` stock movement row.

**[ASSUMPTION]** This endpoint is reachable through the gateway today but is intended for service-to-service use only. A future iteration will move it to an internal-only port or scope it via a service-token role.

**Auth.** Required. Calling principal must have role `TECHNICIAN`, `ADMIN`, or `OWNER`.
**Kind.** Sync + async fan-out.
**Async effects.** If post-decrement stock falls at or below threshold, emits `inventory.reorder-alert`.

#### Request body

```json
{
  "quantity": 1,
  "referenceType": "JOB",
  "referenceId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c"
}
```

#### Success — `200 OK`

```json
{
  "sku": "VOLTAS-AC-CAPACITOR-25MFD",
  "previousStock": 5,
  "newStock": 4,
  "unitPriceAtConsumption": 220.00,
  "stockMovementId": 84212,
  "thresholdReached": false
}
```

#### Errors

| Status | Code |
|---|---|
| 404 | `SKU_NOT_FOUND` |
| 409 | `INSUFFICIENT_STOCK` |
| 422 | `VALIDATION_FAILED` |

---

### 8.7 `GET /api/v1/parts/{sku}/movements`

**Purpose.** Stock-movement ledger for a SKU.
**Auth.** Required. `ADMIN` or `OWNER`.
**Kind.** Sync.

#### Query parameters

| Parameter | Type | Notes |
|---|---|---|
| `from` | string | ISO 8601; default = 30 days ago. |
| `to` | string | ISO 8601; default = now. |
| `page` | int | Default 0. |
| `size` | int | Default 20, max 100. |

#### Success — `200 OK`

```json
{
  "content": [
    {
      "id": 84212,
      "sku": "VOLTAS-AC-CAPACITOR-25MFD",
      "movementType": "CONSUME",
      "quantityDelta": -1,
      "stockAfter": 4,
      "referenceType": "JOB",
      "referenceId": "8f3a1b2c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
      "note": null,
      "performedByUserId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "createdAt": "2026-06-02T10:42:00Z"
    },
    {
      "id": 84211,
      "sku": "VOLTAS-AC-CAPACITOR-25MFD",
      "movementType": "RESTOCK",
      "quantityDelta": 12,
      "stockAfter": 5,
      "referenceType": "PURCHASE_ORDER",
      "referenceId": null,
      "note": "Purchase order PO-2026-021",
      "performedByUserId": "owner-id",
      "createdAt": "2026-06-02T08:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 47,
  "totalPages": 3
}
```

---

### 8.8 `GET /api/v1/parts/reorder-alerts`

**Purpose.** List SKUs currently at or below reorder threshold.
**Auth.** Required. `ADMIN` or `OWNER`.
**Kind.** Sync.

#### Success — `200 OK`

```json
{
  "content": [
    {
      "sku": "LG-FRIDGE-COMPRESSOR-1HP",
      "name": "LG Fridge Compressor 1 HP",
      "currentStock": 1,
      "reorderThreshold": 2,
      "deficit": 1,
      "supplierId": 5,
      "supplierName": "Sharma Refrigeration Suppliers"
    }
  ],
  "totalElements": 4
}
```

This endpoint is backed by the partial index `parts_reorder_idx WHERE current_stock <= reorder_threshold AND is_active = TRUE` for O(matches) read.

---

## 9. Auto-Generated Spec and Source-of-Truth Note

### 9.1 Auto-Generated OpenAPI Spec

Each service includes `springdoc-openapi-starter-webmvc-ui` (or `-webflux-ui` for the gateway). At runtime, the following endpoints are exposed on each service's internal port:

| Path | Content |
|---|---|
| `/v3/api-docs` | The complete OpenAPI 3.1 JSON document. |
| `/swagger-ui.html` | Interactive Swagger UI. |

In production, these paths are only reachable inside the VM loopback (not exposed via the gateway). **[INFERENCE]** A future iteration will expose a sanitised, aggregated spec at `https://api.wrenchy.in/v3/api-docs` for partner integrations.

### 9.2 Hand-Written vs. Generated — Source of Truth

The springdoc-generated specification is derived from controller signatures, `@Operation`, `@ApiResponse`, and DTO annotations. **It is the authoritative source for endpoint paths, parameter names, and types.** This hand-written document exists to provide a single, consolidated, prose-quality reference — particularly for explaining business semantics that annotations cannot.

**Discipline rule:** any divergence between this document and the generated spec must be reconciled within the same PR that introduces it. CI runs a script that diffs the generated spec against a snapshot in `docs/openapi/snapshot.json`; PRs that change the surface without updating the snapshot fail the build.

### 9.3 Postman / Insomnia Collection

A Postman v2.1 collection is published at `docs/wrenchy-api.postman_collection.json` (file to be added). It mirrors this document and is regenerated from the springdoc spec via `openapi-to-postmanv2`.

---

## 10. Appendix: Shared Schemas

### 10.1 `Address`

```json
{
  "line1": "string, 1..200 chars",
  "landmark": "string, 0..120 chars",
  "city": "string, defaults to 'Banda'",
  "pincode": "string, exactly 6 digits"
}
```

### 10.2 `PageEnvelope<T>`

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "isFirst": true,
  "isLast": true
}
```

### 10.3 `ErrorEnvelope`

(Section 3.1.)

### 10.4 `JobSummary`

A trimmed projection used in list endpoints — `id`, `referenceCode`, `status`, `customerPhone`, `customerName`, `appliance`, `problem`, `primaryTechId`, `scheduledAt`, `totalAmount`, `isPaid`, `createdAt`, `updatedAt`.

### 10.5 `JobDetail`

The full embedded projection returned by `GET /api/v1/jobs/{id}`, including `partsUsed[]`, `statusHistory[]`, and `photos[]`.

### 10.6 Status Codes Quick Index

| Code | Meaning |
|---|---|
| 200 | OK |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict (state machine, duplicate, insufficient stock) |
| 413 | Payload Too Large |
| 415 | Unsupported Media Type |
| 422 | Unprocessable Entity (semantic validation) |
| 429 | Too Many Requests |
| 500 | Internal Server Error |
| 503 | Service Unavailable |

### Document Change Log

| Version | Date | Author | Change Summary |
|---|---|---|---|
| 1.0 | [ADD: YYYY-MM-DD] | [ADD: Author] | Initial draft covering Auth, Booking, and Parts. |

---

*End of document.*
