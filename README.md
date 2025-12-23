# Nested

A Reddit-style community platform built with Spring Boot and MongoDB.

## Features

- User authentication (register, login, JWT-based sessions)
- Communities (create, subscribe, moderate)
- Posts (text, link, image, video types)
- Nested comments with voting
- User profiles and karma system
- Post search functionality

## Tech Stack

- Java 17
- Spring Boot 4.0
- Spring Security
- MongoDB Atlas
- Thymeleaf
- JWT Authentication

## Running the Application

```bash
./gradlew bootRun
```

The server starts at `http://localhost:8080`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login
- `POST /api/auth/logout` - Logout

### Posts
- `GET /api/posts` - Get posts feed
- `GET /api/posts/hot` - Get hot posts
- `GET /api/posts/new` - Get new posts
- `POST /api/posts` - Create post

### Communities
- `GET /api/subs` - Get popular communities
- `GET /api/subs/{name}` - Get community by name
- `POST /api/subs` - Create community
- `POST /api/subs/{id}/subscribe` - Subscribe

### Comments
- `GET /api/comments/post/{postId}` - Get comments for post
- `POST /api/comments` - Create comment

### Voting
- `POST /api/votes` - Vote on post or comment

## Configuration

Set MongoDB connection in `application.properties`:

```properties
spring.data.mongodb.uri=your-mongodb-uri
spring.data.mongodb.database=nested_server
```
