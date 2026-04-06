# Software Component Catalog System Backend API

Base URL: `http://localhost:8080`

This document describes the current backend API for the frontend application.

## Authentication

Protected endpoints require the header:

`X-Auth-Token: <token>`

The token is returned by both register and login APIs.
Use the logout API to invalidate it explicitly.

The first user who registers is assigned the role `ADMIN`. Later users are assigned `USER`.

## Common Error Response

Most errors follow this shape:

```json
{
  "timestamp": "2026-04-03T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "fieldName": "error message"
  }
}
```

## Enums

### `Role`

- `ADMIN`
- `USER`

### `ComponentType`

- `CODE`
- `UML`
- `ERD`
- `STRUCTURED_DESIGN`

## Auth APIs

### `POST /api/auth/register`

Creates a new user and immediately returns an auth token.

Request body:

```json
{
  "username": "Admin User",
  "email": "admin@example.com",
  "password": "StrongPass1!",
  "confirmPassword": "StrongPass1!"
}
```

Rules:

- `username`: required, 3 to 100 characters
- `email`: required, valid email
- `password`: required, minimum 8 characters
- password must contain uppercase, lowercase, digit, and special character
- `confirmPassword` must match `password`

Success response:

```json
{
  "userId": 1,
  "username": "Admin User",
  "email": "admin@example.com",
  "role": "ADMIN",
  "token": "..."
}
```

### `POST /api/auth/login`

Authenticates an existing user and returns a new auth token.

Request body:

```json
{
  "email": "admin@example.com",
  "password": "StrongPass1!"
}
```

Success response:

```json
{
  "userId": 1,
  "username": "Admin User",
  "email": "admin@example.com",
  "role": "ADMIN",
  "token": "..."
}
```

### `POST /api/auth/logout`

Invalidates the current auth token.

Headers:

- `X-Auth-Token: <token>`

Success response:

- `200 OK` with an empty body

## User API

### `GET /api/users/me`

Returns the currently authenticated user.

Headers:

- `X-Auth-Token: <token>`

Success response:

```json
{
  "id": 1,
  "username": "Admin User",
  "email": "admin@example.com",
  "role": "ADMIN"
}
```

## Catalogue APIs

### `POST /api/catalogues`

Creates a catalogue owned by the authenticated user.

Headers:

- `X-Auth-Token: <token>`

Request body:

```json
{
  "name": "Design Assets",
  "description": "Reusable design artifacts",
  "keywords": "uml, erd, architecture"
}
```

Success response:

```json
{
  "id": 1,
  "name": "Design Assets",
  "description": "Reusable design artifacts",
  "keywords": "uml, erd, architecture",
  "ownerId": 1,
  "ownerUsername": "Admin User",
  "components": []
}
```

### `GET /api/catalogues`

Returns all catalogues in the system.

Headers:

- `X-Auth-Token: <token>`

Success response:

```json
[
  {
    "id": 1,
    "name": "Design Assets",
    "description": "Reusable design artifacts",
    "keywords": "uml, erd, architecture",
    "ownerId": 1,
    "ownerUsername": "Admin User",
    "components": [
      {
        "id": 3,
        "name": "Order UML",
        "type": "UML"
      }
    ]
  }
]
```

### `GET /api/catalogues/mine`

Returns catalogues owned by the authenticated user.

Headers:

- `X-Auth-Token: <token>`

Response shape is the same as `GET /api/catalogues`.

### `PUT /api/catalogues/{id}`

Updates a catalogue. Only the owner can update it.

Headers:

- `X-Auth-Token: <token>`

Request body:

```json
{
  "name": "Updated Catalogue Name",
  "description": "Updated description",
  "keywords": "updated,keywords"
}
```

Success response:

```json
{
  "id": 1,
  "name": "Updated Catalogue Name",
  "description": "Updated description",
  "keywords": "updated,keywords",
  "ownerId": 1,
  "ownerUsername": "Admin User",
  "components": []
}
```

### `DELETE /api/catalogues/{id}`

Deletes a catalogue. Only the owner can delete it.

Headers:

- `X-Auth-Token: <token>`

Success response:

- HTTP `200 OK`
- empty body

## Component APIs

### `POST /api/components`

Creates a component. Only `ADMIN` users can create components.

Headers:

- `X-Auth-Token: <token>`

Request body:

```json
{
  "name": "Order UML",
  "description": "Class diagram for the order flow",
  "keywords": "uml,order,design",
  "type": "UML",
  "catalogueIds": [1, 2]
}
```

Notes:

- `catalogueIds` is optional
- a component can belong to multiple catalogues

Success response:

```json
{
  "id": 3,
  "name": "Order UML",
  "description": "Class diagram for the order flow",
  "keywords": "uml,order,design",
  "type": "UML",
  "usageCount": 0,
  "searchHitCount": 0,
  "searchedButNotUsedCount": 0,
  "catalogueIds": [1, 2]
}
```

### `PUT /api/components/{id}`

Updates a component. Only `ADMIN` users can update components.

Headers:

- `X-Auth-Token: <token>`

Request body is the same as create.

### `DELETE /api/components/{id}`

Deletes a component. Only `ADMIN` users can delete components.

Headers:

- `X-Auth-Token: <token>`

Success response:

- HTTP `200 OK`
- empty body

### `GET /api/components`

Returns all components.

Headers:

- `X-Auth-Token: <token>`

Success response:

```json
[
  {
    "id": 3,
    "name": "Order UML",
    "description": "Class diagram for the order flow",
    "keywords": "uml,order,design",
    "type": "UML",
    "usageCount": 0,
    "searchHitCount": 0,
    "searchedButNotUsedCount": 0,
    "catalogueIds": [1]
  }
]
```

### `GET /api/components/{id}`

Returns one component by id.

Headers:

- `X-Auth-Token: <token>`

Success response shape is the same as the item returned by `GET /api/components`.

### `GET /api/components/search?keywords=<value>`

Searches components by keyword terms.

Headers:

- `X-Auth-Token: <token>`

Behavior:

- accepts comma-separated or whitespace-separated terms
- matches on component name, keywords, and description
- returns ranked results
- increments `searchHitCount` for every returned component

Example:

`GET /api/components/search?keywords=uml order`

Success response:

```json
[
  {
    "id": 3,
    "name": "Order UML",
    "description": "Class diagram for the order flow",
    "keywords": "uml,order,design",
    "type": "UML",
    "usageCount": 0,
    "searchHitCount": 1,
    "searchedButNotUsedCount": 1,
    "catalogueIds": [1]
  }
]
```

### `POST /api/components/{id}/use`

Records a component usage event.

Headers:

- `X-Auth-Token: <token>`

Behavior:

- increments `usageCount`
- `searchedButNotUsedCount` is returned as `searchHitCount - usageCount`, floored at `0`

Success response:

```json
{
  "id": 3,
  "name": "Order UML",
  "description": "Class diagram for the order flow",
  "keywords": "uml,order,design",
  "type": "UML",
  "usageCount": 1,
  "searchHitCount": 1,
  "searchedButNotUsedCount": 0,
  "catalogueIds": [1]
}
```

## Frontend Integration Notes

- All protected calls must include `X-Auth-Token`
- Store the token returned by register/login on the client
- Catalogue update/delete is owner-based
- Component create/update/delete is admin-only
- Public-style browsing is not enabled yet; list/search/detail still require authentication
- There is no logout endpoint yet
- There is no pagination yet
