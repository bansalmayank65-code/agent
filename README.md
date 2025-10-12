# Amazon Agentic Workstation

This project is a task orchestration web app with a Flutter frontend and Java Spring Boot backend.

## Features
- 9-step collapsible workflow in Flutter
- Backend REST endpoints for validation, generation, and download
- DTOs, validation, and example task.json

## Structure
- `frontend/` — Flutter app
- `backend/` — Spring Boot app (Maven)

## How to Run
- **Backend:**
  1. Navigate to `backend/`
  2. Run `mvn spring-boot:run`
- **Frontend:**
  1. Navigate to `frontend/`
  2. Run `flutter pub get`
  3. Run `flutter run`

## Integration
- The Flutter app connects to backend endpoints at `http://localhost:8080`

## Example task.json
See `backend/src/main/resources/example-task.json`

---
For details, see comments in code files.
