# API Documentation

## Endpoints

### Health Check
- **GET** `/health`
- **Response**: `OK`
- **Description**: Basic health check endpoint

### Test Endpoint
- **GET** `/api/test`
- **Response**: 
  ```json
  {
    "message": "Amazon Q Test",
    "status": "SUCCESS"
  }
  ```
- **Description**: Test endpoint for Amazon Q integration

## Authentication
Uses platform-security libraries for authentication when configured.

## Error Handling
Standard platform error responses with appropriate HTTP status codes.
