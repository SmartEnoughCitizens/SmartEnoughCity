# hermes
Hermes is the central backend service written in Spring Boot. It acts as the integration hub for simulations, recommendations, user management, and email workflows, providing a unified API for the frontend. Hermes coordinates all core components while keeping downstream services modular and scalable.

Features
Consolidated endpoints for simulations, recommendations, users, and notifications.
	•	Simulation Orchestration
Connects to internal simulation engines and aggregates results.
	•	Recommendation Integration
Serves personalized recommendations sourced from dedicated services.
	•	User Management
Authentication, authorization, user profiles, and session handling.
	•	Email Workflow Coordination
Triggers transactional emails and integrates with configured mail services.
	•	Spring-Native Architecture
Built using Spring Boot, Spring Web, Spring Security, and optional Spring Data modules.

Tech Stack
	•	Language: Java 17+
	•	Framework: Spring Boot
	•	Build Tool: Maven or Gradle
	•	Database: (Insert your DB, e.g., PostgreSQL/MySQL)
	•	Communication: REST (optional: WebClient, Feign, Kafka, or RabbitMQ)
