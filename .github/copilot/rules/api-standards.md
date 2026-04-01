# REST API Development Standards for DSH

## OpenAPI First
- All API changes must start with an update to `/specs/api/openapi/dsh-rest-api.yaml`
- Use OpenAPI 3.0.3 specification format
- Define reusable schemas under `components/schemas`
- Use `$ref` to reference shared schemas instead of duplicating definitions

## URL Design
- Use lowercase, hyphen-separated path segments: `/document-highlights` not `/documentHighlights`
- Use plural nouns for resource collections: `/documents`, `/highlights`
- Nest sub-resources under their parent: `/documents/{documentId}/highlights`
- Version the API via the URL path: `/api/v1/...`
- Keep URLs stateless; avoid verbs in paths

## HTTP Methods
| Method | Usage |
|--------|-------|
| `GET` | Retrieve resources (idempotent, no side effects) |
| `POST` | Create a new resource or trigger an action |
| `PUT` | Replace a resource completely |
| `PATCH` | Partially update a resource |
| `DELETE` | Remove a resource |

## HTTP Status Codes
| Code | Meaning |
|------|---------|
| `200 OK` | Successful GET, PUT, PATCH |
| `201 Created` | Successful POST that creates a resource |
| `202 Accepted` | Async operation accepted |
| `204 No Content` | Successful DELETE or PATCH with no body |
| `400 Bad Request` | Validation failure or malformed request |
| `401 Unauthorized` | Missing or invalid authentication |
| `403 Forbidden` | Authenticated but not authorised |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | State conflict (e.g., duplicate resource) |
| `422 Unprocessable Entity` | Semantic validation failure |
| `500 Internal Server Error` | Unexpected server-side error |

## Request & Response Conventions
- Use `application/json` as the default content type
- Use `multipart/form-data` for file uploads
- Always include a `Content-Type` header in responses with a body
- Use ISO 8601 format for all date/time fields: `2024-01-15T10:30:00Z`
- Use camelCase for JSON property names
- Return paginated collections using `page`, `size`, and `totalElements` fields

## Error Response Format
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for field 'confidence'",
  "path": "/api/v1/documents"
}
```

## Security
- All endpoints (except health checks) require authentication
- Use JWT Bearer tokens: `Authorization: Bearer <token>`
- Validate and sanitise all inputs server-side
- Never expose internal error details in production responses

## Documentation
- Every endpoint must have a `summary` and `description` in the OpenAPI spec
- Include request/response examples in the spec
- Document all query parameters, path parameters, and headers
- Reference the relevant feature spec in endpoint descriptions

## Spring Boot Implementation
- Use `@RestController` with `@RequestMapping("/api/v1/...")`
- Use `ResponseEntity<T>` to control response status and headers
- Delegate business logic to `@Service` classes; keep controllers thin
- Use `@Valid` on request body parameters to trigger Bean Validation
