## Course Service API Requests (with Pagination)

All requests are expected to go through the API Gateway with a valid JWT:

```bash
GATEWAY_BASE_URL=http://localhost:8085
AUTH_TOKEN="Bearer <jwt>"
```

### List Published Courses (paginated)

`GET /courses?page=0&size=10`

```bash
curl -X GET "$GATEWAY_BASE_URL/courses?page=0&size=10" \
  -H "Authorization: $AUTH_TOKEN"
```

Sample response:

```json
{
  "items": [
    {
      "id": "course-id-1",
      "title": "Intro to Java",
      "description": "Core Java fundamentals",
      "instructorId": "instructor-123",
      "thumbnailImageUrl": "https://cdn.example.com/thumbnails/intro-to-java.png",
      "lectureContents": [
        {
          "title": "Lesson 1 - Java Basics",
          "contentType": "VIDEO",
          "contentUrl": "https://cdn.example.com/content/lesson-1.mp4",
          "description": "Introduction to Java syntax",
          "durationSeconds": 720
        }
      ],
      "published": true,
      "createdAt": "2026-04-06T10:00:00Z",
      "modifiedAt": "2026-04-06T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

### List Instructor Courses (paginated)

`GET /instructor/courses?page=0&size=10`

```bash
curl -X GET "$GATEWAY_BASE_URL/instructor/courses?page=0&size=10" \
  -H "Authorization: $AUTH_TOKEN"
```

### List Current Student Enrollments (paginated)

`GET /student/enrollments?page=0&size=10`

```bash
curl -X GET "$GATEWAY_BASE_URL/student/enrollments?page=0&size=10" \
  -H "Authorization: $AUTH_TOKEN"
```

Sample response:

```json
{
  "items": [
    {
      "id": "enrollment-id-1",
      "courseId": "course-id-1",
      "studentId": "student-123",
      "enrolledAt": "2026-04-06T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

### List Students for a Course (paginated)

`GET /courses/{courseId}/students?page=0&size=10`

```bash
COURSE_ID="course-id-1"

curl -X GET "$GATEWAY_BASE_URL/courses/$COURSE_ID/students?page=0&size=10" \
  -H "Authorization: $AUTH_TOKEN"
```

Sample response:

```json
{
  "items": [
    {
      "studentId": "student-123",
      "enrolledAt": "2026-04-06T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

