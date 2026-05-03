<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->
- [x] Verify that the copilot-instructions.md file in the .github directory is created.

- [x] Clarify Project Requirements
	Project: Homestay management platform with Spring Boot + Spring AI + Vue + Element UI
	Requirements: User/Admin management, Homestay/Booking/Routes/Activities CRUD, AI recommendations based on user orders
	Database: H2 in-memory for demo, MySQL ready for production
	AI: OpenAI-compatible format, optional during demo

- [x] Scaffold the Project
	Created Maven project structure with Spring Boot 3.2.2, Spring AI BOM 1.0.0-M2
	Created Vue 3 + Vite frontend with Element Plus UI
	Configured H2 database for demo, MySQL driver included for production

- [x] Customize the Project
	Implemented domain entities (User, Homestay, Booking, Route, Activity)
	Created REST APIs for CRUD operations and booking management
	Implemented AI service with optional ChatModel (fallback when API key unavailable)
	Created Vue demo interface with user input, data display, and AI recommendation button

- [x] Install Required Extensions
	No extensions required - project ready to run with Maven and npm

- [x] Compile the Project
	Backend: Maven build successful with Spring AI milestone repository
	Frontend: npm dependencies installed successfully

- [x] Create and Run Task
	Backend running on http://localhost:8080
	Frontend running on http://localhost:5173

- [x] Launch the Project
	Backend: Spring Boot application started, H2 database initialized
	Frontend: Vite dev server started with hot reload
	Both services running and responding to requests

- [x] Ensure Documentation is Complete
	README.md created with setup instructions
	All checklist items completed and marked
	Project structure documented
