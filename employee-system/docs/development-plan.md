# Employee System Development Plan

## Project Overview
A Java-based payroll and employee management system using a SQLite database. The application supports three roles: Employee, HR, and Admin. Each role has tailored access and functions to manage employee data, time entries, payroll calculations, and reports.

## Goals
- Build a desktop-style Java application with role-based access
- Use SQLite for data persistence with at least 12 seeded employees
- Provide secure login and authorization for Employee, HR, and Admin users
- Deliver documentation, source control, and a final GitHub submission

## Scope
### Required features
- Login page with username/password and role selection
- Employee view: personal profile, time entry, PTO, payroll preview
- HR view: employee search, profile access, payroll calculations, reports
- Admin view: full employee CRUD, system overview, user management
- Database tables for employees, jobs, departments, pay types, medical plans, addresses, dependents, time entries, and employment history
- Basic security and access controls
- Testing and documentation

## Milestones
1. Project Setup and Repository
   - Create GitHub repository
   - Add `docs/` and initial project documentation
   - Configure Maven project structure and dependencies

2. Data Model and Database
   - Design database schema for employee system entities
   - Implement SQLite schema and seed data with 12 test employees
   - Verify the database connection from Java

3. Authentication and Authorization
   - Create login screen with username/password inputs
   - Implement role-based access for Employee, HR, and Admin
   - Secure the login flow and validate credentials

4. Employee Functionality
   - Show employee profile details
   - Allow employees to view and submit time entries
   - Add PTO tracking or request interface
   - Add simple paycheck calculation or preview

5. HR Functionality
   - Enable HR users to search employees
   - Show demographic and payroll details for selected employees
   - Allow HR to add/edit employee records where appropriate
   - Generate payroll or summary reports for review

6. Admin Functionality
   - Provide full CRUD operations for employee records
   - Manage jobs, departments, and pay types
   - Display application status/version information
   - Add administrative security and configuration controls

7. Testing and Documentation
   - Create test cases for login, role access, and database operations
   - Validate payroll calculations and CRUD operations
   - Document the application setup, data model, and user flows
   - Prepare final submission materials for GitHub

## Feature Breakdown
### Authentication
- Mandatory login for all users
- Role-based navigation and view restrictions
- Example HR account: `HR0001` with secure password policy

### Employee View
- Personal profile access only for the logged-in employee
- Time entry creation and history review
- PTO options and payroll calculation support

### HR View
- Access employee demographic and payroll data
- Search employees by name, ID, or department
- Add/edit employee details within HR permissions
- Generate exportable payroll or employee reports

### Admin View
- Full access to all employee records
- Add, edit, and delete employees
- Manage organizational data such as jobs, departments, and pay types
- View application information and version data

## Notes
- Keep the UI clean and consistent across the three roles
- Focus on correct access control before adding advanced features
- Use `docs/` for the development plan and supplementary documentation
- Keep the database file under `database/` and reference it in the application configuration

## Next Steps
