# Car Rental System (JavaFX)

A Car Rental application built with **Java** and **JavaFX**.
It uses a relational database (SQL scripts included in the repository) to manage authentication, cars, rentals, and payments.

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Run the Application](#run-the-application)
- [Screens & Assets](#screens--assets)
- [UML](#uml)
- [Troubleshooting](#troubleshooting)
- [Notes / Improvements](#notes--improvements)

## Features
- User authentication (login)
- Cars data management (cars table)
- Rental workflow (rentals table)
- Payments (payments + card details tables)

> Note: Exact screens/features depend on the JavaFX classes in the repo (e.g. `Login.java`) and how they are wired.

## Tech Stack
- **Java**
- **JavaFX** (UI)
- **SQL Database** (schema scripts included)
- **JDBC** (via `DBConnection.java`)

## Project Structure
The repository is mostly stored at the root level:

- `README.md` — project documentation
- `DBConnection.java` — database connectivity (JDBC connection helper)
- `Login.java` — login screen / authentication UI (JavaFX)
- `javafx_base.xml` — JavaFX resource/configuration file (UI base)
- `project-programming-3_*.sql` — database schema/scripts
- `UML.png` — UML diagram
- `logo.png`, `background.png` — UI assets
- `*.class` — compiled Java class files (typically should not be committed)

## Prerequisites
- **JDK** installed (recommended: Java 11+)
- **JavaFX SDK** installed (if your JDK does not bundle JavaFX)
- A SQL database server (MySQL/MariaDB or similar) matching your SQL scripts
- A Java IDE (recommended): IntelliJ IDEA / Eclipse / NetBeans

## Database Setup
This repository contains SQL scripts to create the database structure.

### 1) Create a database
Create an empty database (example name: `car_rental`).

### 2) Run the SQL scripts
Run scripts in this order (recommended):
1. `project-programming-3_authentication.sql`
2. `project-programming-3_cars.sql`
3. `project-programming-3_rentals.sql`
4. `project-programming-3_payments.sql`
5. `project-programming-3_payment_card_details.sql`

If any script fails due to missing tables/foreign keys, run the script that defines the referenced table first.

## Configuration
Update the database credentials and connection URL in:

- `DBConnection.java`

You usually need to configure:
- DB host / port
- database name
- username / password
- JDBC driver (depending on database)

## Run the Application

### Option A (recommended): Run from an IDE
1. Open the project in IntelliJ / Eclipse.
2. Configure JavaFX:
   - Add JavaFX SDK to your project libraries.
   - Add VM options (example):
     - `--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml`
3. Run the application entry class.
   - Often this is `Login.java` or a main launcher class (if present).

### Option B: Run from command line
This requires JavaFX SDK configured on your machine. Example:

```bash
javac --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml *.java
java  --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml Login
```

Paths and class names may differ depending on your setup.

## Screens & Assets
- `logo.png` and `background.png` are UI images used by the JavaFX interface.

## UML
See: `UML.png` for the class diagram / design overview.

## Troubleshooting

### Login fails / database errors
- Confirm the database server is running
- Confirm credentials in `DBConnection.java`
- Confirm tables exist (execute SQL scripts)
- Ensure JDBC driver is available on the classpath

### JavaFX runtime components are missing
If you see errors like “JavaFX runtime components are missing”:
- Install JavaFX SDK
- Add module path VM arguments:
  - `--module-path .../javafx/lib --add-modules javafx.controls,javafx.fxml`

## Notes / Improvements
Recommended improvements for maintaining this repo:
- Remove committed compiled files (`*.class`) and add a `.gitignore`
- Add Maven (`pom.xml`) or Gradle (`build.gradle`) build configuration
- Move sources into `src/main/java` (standard layout)
- Add a `docs/` folder with install + usage documentation