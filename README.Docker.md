# Docker Deployment Guide

This guide explains how to build and run the Authentication Server using Docker.

## Prerequisites

- Docker installed (version 20.10 or higher)
- Docker Compose installed (version 2.0 or higher)

## Quick Start

### Development Mode (H2 Database)

The simplest way to run the application with an in-memory H2 database:

```bash
# Build and start the container
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the container
docker-compose down
```

The application will be available at:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

### Production Mode (MySQL Database)

For production deployment with a persistent MySQL database:

1. Create a `.env` file with your configuration:
```bash
JWT_SECRET=your-secure-random-string-here
GOOGLE_CLIENT_ID=your-google-client-id
MYSQL_ROOT_PASSWORD=your-root-password
MYSQL_DATABASE=authdb
MYSQL_USER=authuser
MYSQL_PASSWORD=your-db-password
```

2. Start the services:
```bash
docker-compose -f docker-compose.prod.yml up -d
```

3. Check the status:
```bash
docker-compose -f docker-compose.prod.yml ps
docker-compose -f docker-compose.prod.yml logs -f auth-server
```

## Building the Docker Image

### Build Manually

```bash
# Build the image
docker build -t authentication-server:latest .

# Run the container
docker run -p 8080:8080 authentication-server:latest
```

### Build with Custom Tag

```bash
docker build -t authentication-server:v1.0.0 .
```

## Docker Commands Cheat Sheet

### Container Management

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Restart services
docker-compose restart

# View running containers
docker-compose ps

# View logs
docker-compose logs -f auth-server

# Execute command in container
docker-compose exec auth-server sh

# Remove all containers and volumes
docker-compose down -v
```

### Image Management

```bash
# List images
docker images

# Remove image
docker rmi authentication-server:latest

# Rebuild image (no cache)
docker-compose build --no-cache

# Pull latest base images
docker-compose pull
```

## Environment Variables

The application supports the following environment variables:

### Database Configuration
- `SPRING_DATASOURCE_URL` - Database connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `SPRING_JPA_HIBERNATE_DDL_AUTO` - Hibernate DDL auto mode (create-drop, update, validate)

### JWT Configuration
- `APP_JWT_SECRET` - Secret key for JWT signing (minimum 256 bits)
- `APP_JWT_EXPIRATION_IN_MS` - Access token expiration (default: 24 hours)
- `APP_REFRESH_TOKEN_EXPIRATION_IN_MS` - Refresh token expiration (default: 7 days)

### OAuth2 Configuration
- `APP_OAUTH2_GOOGLE_CLIENT_ID` - Google OAuth2 client ID

### Logging
- `LOGGING_LEVEL_COM_EXAMPLE_AUTHSERVER` - Application log level (DEBUG, INFO, WARN, ERROR)

## Health Check

The container includes a health check endpoint:

```bash
# Check container health
docker inspect --format='{{.State.Health.Status}}' authentication-server

# Manual health check
curl http://localhost:8080/actuator/health
```

## Troubleshooting

### Container won't start

1. Check logs:
```bash
docker-compose logs auth-server
```

2. Check if port 8080 is already in use:
```bash
# Windows
netstat -ano | findstr :8080

# Linux/Mac
lsof -i :8080
```

### Database connection issues

1. Verify MySQL is running:
```bash
docker-compose ps mysql
```

2. Check MySQL logs:
```bash
docker-compose logs mysql
```

3. Test connection:
```bash
docker-compose exec mysql mysql -u authuser -p authdb
```

### Out of memory errors

Increase memory limits in docker-compose.yml:

```yaml
services:
  auth-server:
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
```

## Performance Optimization

### JVM Memory Settings

The Dockerfile includes optimal JVM settings for containers:
- `-XX:+UseContainerSupport` - Enables container-aware JVM
- `-XX:MaxRAMPercentage=75.0` - Uses 75% of container memory
- `-Djava.security.egd=file:/dev/./urandom` - Faster startup

### Multi-stage Build

The Dockerfile uses a multi-stage build to:
- Reduce final image size (JRE instead of JDK)
- Separate build and runtime dependencies
- Improve security (no build tools in production image)

## Security Best Practices

1. **Never commit `.env` files** - Keep secrets out of version control
2. **Change default passwords** - Update JWT_SECRET and database passwords
3. **Use non-root user** - The container runs as user `spring:spring`
4. **Keep images updated** - Regularly update base images
5. **Enable HTTPS** - Use a reverse proxy (nginx/traefik) with SSL

## Production Deployment

For production deployment, consider:

1. **Use a reverse proxy** (nginx, Traefik) for SSL termination
2. **Set up monitoring** (Prometheus, Grafana)
3. **Configure backup** for MySQL volumes
4. **Use Docker secrets** for sensitive data
5. **Set up log aggregation** (ELK stack, Loki)

### Example with nginx

```yaml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - auth-server
```

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Build Docker image
  run: docker build -t ${{ secrets.REGISTRY }}/auth-server:${{ github.sha }} .

- name: Push to registry
  run: docker push ${{ secrets.REGISTRY }}/auth-server:${{ github.sha }}
```

## Support

For issues and questions:
- Check application logs: `docker-compose logs -f auth-server`
- Review Swagger documentation: http://localhost:8080/swagger-ui.html
- Verify environment configuration


