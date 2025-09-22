# Authentication Server

A comprehensive authentication server for Android mobile applications built with Java, Spring Boot, Spring Security, and Maven. This server provides Google OAuth verification, JWT token management with refresh tokens, and complete user CRUD operations.

## Features

- **Google OAuth Integration**: Verify Google ID tokens from Android clients
- **JWT Authentication**: Secure access and refresh token management
- **User Management**: Complete CRUD operations for user accounts
- **Spring Security**: Comprehensive security configuration
- **Database Support**: H2 for development, MySQL for production
- **RESTful API**: Clean and well-documented endpoints

## Technology Stack

- **Java 17**
- **Spring Boot 3.1.5**
- **Spring Security**
- **Spring Data JPA**
- **Maven**
- **H2 Database** (Development)
- **MySQL** (Production)
- **JWT (JSON Web Tokens)**
- **Google OAuth 2.0**

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- MySQL (for production)

### Installation

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd authentication-server
   ```

2. **Configure Google OAuth**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select an existing one
   - Enable the Google+ API
   - Create OAuth 2.0 credentials
   - Update `application.yml` with your Google Client ID:
     ```yaml
     app:
       oauth2:
         google:
           clientId: your-google-client-id-here
     ```

3. **Run the application (Development)**
   ```bash
   mvn spring-boot:run
   ```

4. **Access H2 Console (Development)**
   - URL: http://localhost:8080/h2-console
   - JDBC URL: `jdbc:h2:mem:testdb`
   - Username: `sa`
   - Password: `password`

### Production Setup

1. **Configure MySQL Database**
   ```sql
   CREATE DATABASE auth_server_db;
   CREATE USER 'auth_user'@'localhost' IDENTIFIED BY 'secure_password';
   GRANT ALL PRIVILEGES ON auth_server_db.* TO 'auth_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. **Set Environment Variables**
   ```bash
   export DB_USERNAME=auth_user
   export DB_PASSWORD=secure_password
   export JWT_SECRET=your-super-secret-jwt-key-min-256-bits
   export GOOGLE_CLIENT_ID=your-google-client-id
   ```

3. **Run with Production Profile**
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=prod
   ```

## API Endpoints

### Authentication Endpoints

#### Google OAuth Login/Signup
```http
POST /api/auth/google
Content-Type: application/json

{
  "idToken": "google-id-token-from-android-app"
}
```

**Response:**
```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "refresh-token-uuid",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "profilePictureUrl": "https://...",
    "isEmailVerified": true,
    "authProvider": "GOOGLE",
    "createdAt": "2023-01-01T00:00:00",
    "lastLogin": "2023-01-01T00:00:00"
  }
}
```

#### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

#### Logout
```http
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

### User Management Endpoints

#### Get Current User
```http
GET /api/users/me
Authorization: Bearer your-jwt-token
```

#### Get User by ID
```http
GET /api/users/{id}
```

#### Get All Users (Paginated)
```http
GET /api/users?page=0&size=10&sortBy=name&sortDir=asc
```

#### Update Current User
```http
PUT /api/users/me
Authorization: Bearer your-jwt-token
Content-Type: application/json

{
  "name": "Updated Name",
  "profilePictureUrl": "https://new-profile-pic-url"
}
```

#### Delete Current User
```http
DELETE /api/users/me
Authorization: Bearer your-jwt-token
```

## Android Integration

### 1. Add Dependencies to your Android app

```gradle
implementation 'com.google.android.gms:play-services-auth:20.7.0'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
```

### 2. Configure Google Sign-In

```java
GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken("your-google-client-id")
    .requestEmail()
    .build();

GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
```

### 3. Send ID Token to Server

```java
private void sendTokenToServer(String idToken) {
    GoogleAuthRequest request = new GoogleAuthRequest(idToken);
    
    // Use Retrofit or your preferred HTTP client
    apiService.authenticateWithGoogle(request)
        .enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) {
                    AuthResponse authResponse = response.body();
                    // Save tokens and user info
                    saveTokens(authResponse.getAccessToken(), authResponse.getRefreshToken());
                }
            }
            
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Handle error
            }
        });
}
```

## Security Features

- **JWT Tokens**: Stateless authentication with configurable expiration
- **Refresh Tokens**: Secure token renewal mechanism
- **CORS Configuration**: Configurable cross-origin resource sharing
- **SQL Injection Protection**: JPA/Hibernate parameter binding
- **Input Validation**: Bean validation for all request DTOs
- **Secure Headers**: Security headers configuration

## Database Schema

### Users Table
- `id` (Primary Key)
- `name`
- `email` (Unique)
- `google_id` (Unique)
- `profile_picture_url`
- `is_email_verified`
- `auth_provider` (GOOGLE, LOCAL)
- `created_at`
- `updated_at`
- `last_login`

### Refresh Tokens Table
- `id` (Primary Key)
- `token` (Unique)
- `user_id` (Foreign Key)
- `expires_at`
- `created_at`
- `is_revoked`

## Configuration

### JWT Configuration
```yaml
app:
  jwtSecret: your-secret-key
  jwtExpirationInMs: 86400000  # 24 hours
  refreshTokenExpirationInMs: 604800000  # 7 days
```

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_server_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

## Error Handling

The API returns consistent error responses:

```json
{
  "success": false,
  "message": "Error description"
}
```

Common HTTP status codes:
- `200` - Success
- `400` - Bad Request (validation errors)
- `401` - Unauthorized (invalid/expired token)
- `404` - Not Found
- `500` - Internal Server Error

## Development

### Running Tests
```bash
mvn test
```

### Building for Production
```bash
mvn clean package -Pprod
```

### Docker Support (Optional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/authentication-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please create an issue in the repository.
