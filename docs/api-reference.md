# API Reference

Both services expose interactive OpenAPI documentation via Swagger UI:

- **Backend Engine:** `http://localhost:8080/swagger-ui/index.html`
- **Analytics Consumer:** `http://127.0.0.1:8081/swagger-ui/index.html`

## Backend Routing Engine (`http://localhost:8080`)

### Link Management

**Create a Short Link**

```
POST /api/v1/links
```

Provisions a redirection target for a given destination URL. This operation generates a sequential Snowflake ID, encodes it into a Base62 short code, and persists the record to the PostgreSQL transactional database.

> **Rate Limits:** Enforced at 10 burst requests per IP address. Tokens replenish at a rate of $0.1\text{ tokens/second}$ (~6 requests per minute).

*Request Body:*

```json
{
  "originalLink": "https://example.com",
  "expiresInDays": 7
}
```

| Field Name      | Type    | Required | Description                                                    |
|-----------------|---------|----------|----------------------------------------------------------------|
| `originalLink`  | String  | Yes      | Destination target; must evaluate to a structurally valid URL. |
| `expiresInDays` | Integer | No       | Validity duration. Defaults to 7 days if omitted.              |

*Response (201 Created):*

```json
{
  "code": "In52vyTSef",
  "shortUrl": "http://localhost:8080/In52vyTSef",
  "originalLink": "https://example.com"
}
```

*Response (400 Bad Request, Validation Failure):*

```json
{
  "timestamp": "2026-06-29T07:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "must be a valid URL"
}
```

*Response (429 Too Many Requests, Throttled):*

```json
{
  "timestamp": "2026-06-29T07:00:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later."
}
```

### Redirection & Routing

#### Resolve Short Code

```
GET /{code}
```

Resolves a short code to its destination URL. This endpoint uses a Cache-Aside pattern, searching Redis first. If a cache miss occurs, the backend queries PostgreSQL and populates Redis using a TTL equal to the link's remaining lifespan. It asynchronously fires a Kafka event before returning.

*Path parameters:*

| Parameter | Type   | Description                                           |
|-----------|--------|-------------------------------------------------------|
| `code`    | String | The Base62 short code generated during link creation. |

*Response (302 Found):*

```
HTTP/1.1 302 Found
Location: https://example.com
```

*Response (404 Not Found, Invalid Code):*

```json
{
  "timestamp": "2026-06-29T07:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Link Unavailable"
}
```

*Response (410 Gone, Expired Link):*

```json
{
  "timestamp": "2026-06-29T07:00:00Z",
  "status": 410,
  "error": "Gone",
  "message": "Link Expired"
}
```

---

## Analytics Consumer (`http://127.0.0.1:8081`)

All analytical metrics are scoped to a specific short link by its unique `{code}` parameter. If a code has no recorded click history, metrics return counts of `0` and empty array structures.

### Analytics Reporting

#### Get Full Analytics Summary

```
GET /api/v1/analytics/{code}/summary
```

Aggregates total redirect metrics, timeseries distributions, and client technology breakdowns into a unified payload.

*Response (200 OK):*

```json
{
  "total": 42,
  "dates": [
    { "label": "2026-06-29 00:00:00+00", "count": 42 }
  ],
  "countries": [
    { "label": "Kuwait", "count": 38 },
    { "label": "Unknown", "count": 4 }
  ],
  "browsers": [
    { "label": "Chrome 149", "count": 30 },
    { "label": "Safari 17", "count": 12 }
  ],
  "operatingSystems": [
    { "label": "Windows NT 10.0", "count": 25 },
    { "label": "iOS 17", "count": 12 },
    { "label": "Unknown", "count": 5 }
  ],
  "devices": [
    { "label": "Desktop", "count": 30 },
    { "label": "Phone", "count": 12 }
  ]
}
```

#### Get Total Clicks

```
GET /api/v1/analytics/{code}/clicks/total
```

*Response (200 OK):*

```json
{
  "total": 42
}
```

#### Get Clicks Grouped by Date

```
GET /api/v1/analytics/{code}/clicks/by-date
```

Returns redirect frequencies aggregated by UTC calendar days, ordered chronologically.

*Response (200 OK):*

```json
{
  "metrics": [
    { "label": "2026-06-27 00:00:00+00", "count": 15 },
    { "label": "2026-06-28 00:00:00+00", "count": 18 },
    { "label": "2026-06-29 00:00:00+00", "count": 9 }
  ]
}
```

#### Get Clicks Grouped by Country

```
GET /api/v1/analytics/{code}/clicks/by-country
```

*Response (200 OK):*

```json
{
  "metrics": [
    { "label": "Kuwait", "count": 38 },
    { "label": "United States", "count": 3 },
    { "label": "Unknown", "count": 1 }
  ]
}
```

**Note:** IP addresses classified as private (RFC 1918), local loopbacks, or addresses missing from the MaxMind GeoIP database resolve to `"Unknown"`.

#### Get Clicks Grouped by Browser

```
GET /api/v1/analytics/{code}/clicks/by-browser
```

*Response (200 OK):*

```json
{
  "metrics": [
    { "label": "Chrome 149", "count": 30 },
    { "label": "Safari 17.4", "count": 12 }
  ]
}
```

#### Get Clicks Grouped by Operating System

```
GET /api/v1/analytics/{code}/clicks/by-os
```

*Response (200 OK):*

```json
{
  "metrics": [
    { "label": "Windows NT 10.0", "count": 25 },
    { "label": "iOS 17.4", "count": 12 },
    { "label": "Unknown", "count": 5 }
  ]
}
```

#### Get Clicks Grouped by Device

```
GET /api/v1/analytics/{code}/clicks/by-device
```

*Response (200 OK):*

```json
{
  "metrics": [
    { "label": "Desktop", "count": 30 },
    { "label": "Phone", "count": 12 }
  ]
}
```

---

## Global Platform Error Structure

All system exceptions and input validation errors generate a standardized JSON response:

```json
{
  "timestamp": "2026-06-29T07:00:00.000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Link Unavailable"
}
```

| Property Name | Type     | Description                                              |
|---------------|----------|----------------------------------------------------------|
| `timestamp`   | ISO 8601 | ISO 8601 timestamp (UTC) of when the exception occurred. |
| `status`      | Integer  | Standard HTTP status code.                               |
| `error`       | String   | HTTP status reason phrase.                               |
| `message`     | String   | Human-readable explanation of the specific error.        |

### Standardized HTTP Response Matrix

| HTTP Status             | Operational Meaning | Triggering Condition                                            |
|-------------------------|---------------------|-----------------------------------------------------------------|
| `200 OK`                | Success             | Analytical queries successfully processed and returned.         |
| `201 Created`           | Entity Created      | A new short link has been successfully generated and persisted. |
| `302 Found`             | Redirect            | Short code resolved; redirecting the client to the destination. |
| `400 Bad Request`       | Validation Failure  | Invalid input payload (e.g., malformed URL syntax).             |
| `404 Not Found`         | Missing Entity      | The requested short code does not exist in the database.        |
| `410 Gone`              | Resource Expired    | The target short link has passed its expiration window.         |
| `429 Too Many Requests` | Rate Limit Breached | Client IP address has exceeded the API rate limit bounds.       |
| `500 Internal Error`    | Server Fault        | An unexpected error occurred while processing the request.      |