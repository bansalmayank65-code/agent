<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->
- [x] Clarify Project Requirements
	Project: Amazon Agentic Workstation
	Frontend: Flutter
	Backend: Java Spring Boot (Maven)
	Features: Task orchestration web app, 9 collapsible steps, REST endpoints, DTOs, validation, download as zip, example task.json, integration points, comments for clarity.

- [x] Upgrade Java Runtime
	✅ Upgraded Java from version 11 to version 21 LTS
	✅ Updated Spring Boot from 2.7.5 to 3.3.4 (compatible with Java 21)
	✅ Installed Maven 3.9.11 using Scoop package manager
	✅ Set JAVA_HOME to point to Java 21 installation
	✅ Successfully compiled and tested the project with Java 21
	✅ Verified Spring Boot application starts correctly

- [x] Scaffold the Project
	✅ Enhanced Flutter frontend with comprehensive UI structure
	✅ Created modern, responsive design with Amazon branding
	✅ Implemented 9-step workflow with collapsible cards
	✅ Added state management using Provider pattern
	✅ Integrated API service for backend communication
	✅ Added theme support (light/dark modes)
	✅ Created reusable widgets and components
	✅ Added animations and smooth transitions
	✅ Implemented error handling and validation
	✅ Added connection status monitoring
	✅ Created comprehensive project documentation

- [x] Customize the Project
	✅ Created HTML demo page for immediate testing
	✅ Configured Amazon-branded UI with orange/blue theme
	✅ Added comprehensive error handling and validation
	✅ Implemented real-time connection monitoring
	✅ Added responsive design for all screen sizes

- [x] Install Required Extensions
	✅ Database integration extensions and dependencies configured
	✅ JPA/Hibernate ORM with Spring Data repositories
	✅ MySQL connector and H2 in-memory database support
	✅ Spring Boot validation and security components

- [x] Compile the Project
	✅ Successfully compiled Java 17 backend with Spring Boot 3.4.0
	✅ Database schema simplified to 4 tables (login, login_history, task, task_history)
	✅ Removed trainer_profile and user_trainer_link tables completely
	✅ Enhanced TaskService with database logging and validation
	✅ Sample data initialization service working
	✅ Backend server running on port 8080 with database connectivity
	✅ All REST endpoints functional with enhanced validation
	✅ Maven build successful with no errors
	✅ Removed fallback patterns and added strict error handling for missing UI inputs

- [x] Create and Run Task
	✅ Database schema implemented with comprehensive entity relationships
	✅ Repository layer with custom queries for all entities
	✅ Enhanced TaskService with database integration and logging
	✅ PostgreSQL database migration from H2 complete
	✅ Sample users created (admin, demo, test) with encrypted passwords
	✅ Authentication system with login page implemented
	✅ User-trainer relationship table (user_trainer_link) created
	✅ Complete JPA entity, repository, service, and controller for user-trainer relationships
	✅ REST API endpoints for managing user-trainer assignments
	✅ Bulk operations support for assigning multiple users/trainers
	✅ Removed all default fallback patterns to enforce proper UI input validation
	✅ TaskCacheService now throws IllegalArgumentException for missing edge connection fields
	✅ ComputeComplexityService now throws IllegalArgumentException for missing instruction or actions
	✅ EdgeGenerator now throws IllegalArgumentException for unknown field priorities
- [x] Launch the Project
	✅ Spring Boot backend server running on http://localhost:8080
	✅ HTML demo page accessible at http://localhost:3000/demo.html
	✅ All 9 workflow steps functional in demo interface
	✅ API endpoints tested and responding correctly
	✅ Real-time connection status monitoring working
	✅ File download functionality implemented
	✅ Fixed API endpoint mapping by adding @RequestMapping("/api/tasks") to TaskController
	✅ Enhanced UI error handling to show specific error messages instead of generic failures
	✅ Updated Flutter TaskProvider to return detailed error information to UI
	✅ Added test page at http://localhost:3000/test_error_handling.html for API error testing
	✅ Navigation State Management system implemented with complete user journey flows
	✅ Dynamic URL routing for /task/:taskId and /task/:taskId/step/:stepIndex patterns
	✅ TaskWorkflowScreen created with task loading and step management
	✅ Conditional navigation system with Phase 1/Phase 2 logic implementation
	✅ Post-import navigation hooks redirect users to task workflow automatically
	✅ Task history integration allows resuming work on previous tasks
	✅ Flutter build successful with no compilation errors
- [ ] Ensure Documentation is Complete

Work through each checklist item systematically.
Update the copilot-instructions.md file in the .github directory directly as you complete each step.
