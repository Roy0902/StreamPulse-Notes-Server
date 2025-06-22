# Valkey Cache Setup for StreamPulse Notes Server

This document explains how to set up and use Valkey (Redis-compatible) cache for OTP storage in the StreamPulse Notes Server.

## What is Valkey?

Valkey is a Redis-compatible, high-performance in-memory data store designed for caching and real-time applications. It's used in this project to cache OTP codes for faster verification.

## Features Implemented

1. **OTP Caching**: Store OTP codes in Valkey with automatic expiration
2. **Cache-First Verification**: Check cache before database for faster OTP verification
3. **Automatic Cleanup**: Scheduled cleanup every 12 minutes
4. **Fallback to Database**: If cache fails, fall back to database verification

## Setup Instructions

### 1. Using Docker Compose (Recommended)

```bash
# Start Valkey container
docker-compose up -d valkey

# Check if Valkey is running
docker-compose ps

# View logs
docker-compose logs valkey
```

### 2. Manual Installation

If you prefer to install Valkey manually:

```bash
# For Ubuntu/Debian
sudo apt update
sudo apt install valkey

# For macOS with Homebrew
brew install valkey

# Start Valkey service
sudo systemctl start valkey
sudo systemctl enable valkey
```

### 3. Configuration

The application is configured to connect to Valkey at:
- **Host**: localhost
- **Port**: 6379
- **Database**: 0

Configuration is in `application.properties`:
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.database=0
otp.cache.ttl=600
otp.cache.cleanup-interval=720000
```

## How It Works

### OTP Storage Flow
1. User requests OTP for email verification
2. System generates 6-digit OTP
3. OTP is stored in Valkey cache with 10-minute TTL
4. OTP is also stored in database for persistence
5. Email is sent with OTP

### OTP Verification Flow
1. User submits OTP for verification
2. System checks Valkey cache first
3. If found in cache and matches → verification successful
4. If not in cache → check database as fallback
5. OTP is removed from cache after successful verification

### Cleanup Process
- **Cache Cleanup**: Every 12 minutes (automatic via Valkey TTL)
- **Database Cleanup**: Every hour (scheduled task)

## Monitoring

### Check Cache Status
```bash
# Connect to Valkey CLI
docker exec -it streampulse-valkey valkey-cli

# View all keys
KEYS *

# View OTP keys
KEYS otp:*

# Check TTL for specific OTP
TTL otp:user@example.com:EMAIL_VERIFICATION
```

### Application Logs
The application logs cache operations:
- OTP storage in cache
- OTP retrieval from cache
- Cache cleanup operations
- Fallback to database when cache fails

## Performance Benefits

1. **Faster OTP Verification**: Cache lookups are ~100x faster than database queries
2. **Reduced Database Load**: Fewer database queries for OTP operations
3. **Better User Experience**: Faster response times for OTP verification
4. **Automatic Expiration**: Valkey handles OTP expiration automatically

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Ensure Valkey is running: `docker-compose ps`
   - Check port 6379 is available: `netstat -an | grep 6379`

2. **Cache Not Working**
   - Check application logs for Redis connection errors
   - Verify Valkey configuration in `application.properties`

3. **OTP Verification Fails**
   - Check if OTP exists in cache: `docker exec -it streampulse-valkey valkey-cli KEYS otp:*`
   - Verify TTL hasn't expired: `docker exec -it streampulse-valkey valkey-cli TTL <key>`

### Health Check
```bash
# Test Valkey connection
docker exec -it streampulse-valkey valkey-cli ping

# Should return: PONG
```

## Development

### Adding New Cache Operations
1. Extend `OtpCacheService` interface
2. Implement in `OtpCacheServiceImpl`
3. Update services to use cache operations

### Testing Cache
```bash
# Test OTP storage
curl -X POST http://localhost:8080/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}'

# Check cache
docker exec -it streampulse-valkey valkey-cli KEYS otp:*
```

## Production Considerations

1. **Persistence**: Valkey data is persisted to disk with AOF (Append Only File)
2. **Backup**: Regular backups of Valkey data directory
3. **Monitoring**: Monitor cache hit rates and memory usage
4. **Scaling**: Consider Valkey clustering for high availability 