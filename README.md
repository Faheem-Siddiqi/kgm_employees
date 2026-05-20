# KGM Ex-Employee Management System

## Project Overview

A desktop application for Kohinoor Textile Mills Gujar Khan Ltd. to manage ex-employee records, employee registration, document uploads, employee lookup, detail review, and session-controlled access. The application is built with Java Swing for the UI, MySQL for persistence, JDBC for data access, and Maven for project builds.

---

## Project Architecture

The project follows a layered desktop-application architecture:

- **UI Layer**: Swing windows, panels, dialogs, and reusable controls.
- **UI Styling Layer**: Centralized helper classes for layout, colors, tables, forms, and reusable visual patterns.
- **Service Layer**: Authentication and future business-service boundaries.
- **Data Access Layer**: DAO classes for employee registration, lookup, listing, and updates.
- **Model Layer**: Entity and session data models.
- **Configuration Layer**: Database settings and connection creation.
- **Database Layer**: Schema initialization and SQL resources.
- **Utility Layer**: Session and shared utility helpers.

---

## Complete File Structure and Descriptions

### Core Application Entry Point

#### `com.kgm` Package

| File | Functionality |
| --- | --- |
| **Main.java** | Application entry point. Starts on the Swing event thread, initializes the database schema, and opens `LoginView`. |

---

### Configuration and Database Layer

#### `com.kgm.config` Package

| File | Functionality |
| --- | --- |
| **DatabaseConfig.java** | Centralizes database host, port, name, username, and password. Reads JVM properties first, then environment variables, then defaults. |
| **DatabaseConnection.java** | Creates JDBC connections for both server-level database creation and application-level database access. |

#### `com.kgm.database` Package

| File | Functionality |
| --- | --- |
| **DatabaseInitializer.java** | Creates the configured MySQL database and ensures the `employees` table exists on startup. |

#### `src/main/resources`

| File | Functionality |
| --- | --- |
| **schema.sql** | Reference MySQL schema for the `employees` table and document path columns. |

---

### Data Access Layer

#### `com.kgm.dao` Package

| File | Functionality |
| --- | --- |
| **EmployeeRegistrationDao.java** | Inserts newly registered employee records, including profile image and document path fields. |
| **EmployeeRecordDao.java** | Reads employee lists, counts total employees, searches by employee code, loads full employee details, and performs dynamic employee updates. |

---

### Business Logic Layer

#### `com.kgm.service` Package

| File | Functionality |
| --- | --- |
| **AuthService.java** | Handles login validation. Current credential check is `admin` / `1234`; this is the boundary for future DB or directory-based authentication. |
| **EmployeeService.java** | Reserved business-service boundary for employee workflow rules that should not live directly in UI or DAO classes. |
| **EmployeeReportService.java** | Generates employee report packages from database records. Creates a professional PDF with page numbering and copies available employee documents into an employee-named folder. |
| **ExcelImportService.java** | Reserved service boundary for parsing and importing employee records from Excel files. |

---

### Data Models

#### `com.kgm.model` Package

| File | Functionality |
| --- | --- |
| **Employee.java** | Entity model for employee personal, employment, payroll, contact, compliance, benefit, vaccination, document, and profile-image data. |
| **UserSession.java** | Immutable model for active user session data: username, login timestamp, and expiry duration. |

---

### User Interface Layer

#### Main View Windows (`com.kgm.ui` Package)

| File | Functionality |
| --- | --- |
| **LoginView.java** | Login window. Collects username/password, validates through `AuthService`, starts the session, and opens `HomeView`. |
| **HomeView.java** | Main dashboard after login. Shows header, constrained employee-code filter/search bar, employee table, Excel import action, add record action, refresh action, and footer. |
| **EmployeeRegistrationView.java** | Registration window for adding a new employee record. Combines employee form entry and document upload tabs before saving to MySQL. |
| **EmployeeDetailView.java** | Detail and edit window for an existing employee. Loads full employee data, shows tabbed basic/additional/document sections, supports dynamic updates, and provides a `Download Report` action for employee document packages. |

---

### Dialog Components

#### `com.kgm.ui.dialog` Package

| File | Functionality |
| --- | --- |
| **UniversalDialog.java** | Reusable modal dialog component for information, warning, error, success, and confirmation-style interactions. |

---

### Panel Components

#### `com.kgm.ui.panel` Package

| File | Functionality |
| --- | --- |
| **HeaderPanel.java** | Shared application header. Displays the current page title and session/logout controls. |
| **FooterPanel.java** | Shared application footer. Displays company text and optional trailing action space. |
| **EmployeeTablePanel.java** | Paginated employee table. Displays employee summary rows and a per-row `View` action cell that opens `EmployeeDetailView`. |
| **EmployeeRegistrationFormPanel.java** | Employee registration form with profile photo upload and core fields required to create a record. |
| **EmployeeDocumentUploadPanel.java** | Document upload table for registration. Supports search, clear, upload/replace, view, file-size validation, and document path extraction. |
| **EmployeeDocumentViewPanel.java** | Read-only document panel for employee detail view. Shows document availability and view/download-style actions. |
| **EmployeeBasicDetailsPanel.java** | Basic employee detail form used in detail/edit flows. Handles profile image, identity, contact, and core fields while ignoring empty database placeholders such as `N/A` for missing images. |
| **EmployeeAdditionalDetailsPanel.java** | Additional employee detail panel for employment, payroll, banking, reporting, compliance, benefits, and vaccination fields. |
| **ExcelImportButton.java** | Reusable styled button for triggering Excel import workflows. |
| **UniversalTablePanel.java** | Generic paginated table container with action-column, link-column, status-column, and horizontal scrolling support. |

---

### Reusable UI Components

#### `com.kgm.ui.component` Package

| File | Functionality |
| --- | --- |
| **UniversalDatePicker.java** | Reusable single-date picker built around JCalendar. Supports formatted display and popup date selection. |
| **UniversalDateRangePicker.java** | Reusable date-range picker for selecting start and end dates with clear/apply controls. |

---

### UI Theme and Styling Layer

#### `com.kgm.ui.styling` Package

| File | Functionality |
| --- | --- |
| **HomeViewHelper.java** | Styling and layout helper for `HomeView`, including the bordered filter card, constrained-width search bar, dashboard buttons, and body layout. |
| **LoginViewHelper.java** | Styling and component factory methods for the login screen, including background image panel, form layout, placeholder fields, and primary button. |
| **EmployeeRegistrationViewHelper.java** | Layout and style helper for the employee registration window, page header, tabs, action rows, buttons, and scroll behavior. |
| **EmployeeDetailViewHelper.java** | Facade/helper for employee detail layout, tabs, page scroll behavior, update action styling, and detail header setup. |
| **EmployeeDetailViewLayoutHelper.java** | Lower-level layout helper for employee detail frame setup, back/header area, and employee summary styling. |
| **EmployeeRegistrationFormPanelHelper.java** | Styling helper for registration form layout, photo preview, form fields, labels, address areas, and upload controls. |
| **EmployeeBasicDetailsPanelHelper.java** | Styling proxy/helper for basic employee details, reusing form patterns from the registration form helper. |
| **EmployeeAdditionalDetailsPanelHelper.java** | Styling helper for additional details sections, breadcrumbs, grids, field rows, date fields, and return-to-top controls. |
| **EmployeeDocumentUploadPanelHelper.java** | Styling and layout helper for document upload table, search bar, action links, upload state, and preview windows. |
| **EmployeeDocumentViewPanelHelper.java** | Styling proxy/helper for read-only employee document viewing, reusing document upload table visuals. |
| **TableThemeHelper.java** | Shared table theme constants, table styling, empty states, navigation controls, links, and reusable action panels. |
| **TablePaginationHelper.java** | Table factory and pagination helper for employee/document tables, renderers, scroll panes, and pagination buttons. |
| **UniversalTablePanelHelper.java** | Rendering helper for `UniversalTablePanel`, including action labels, status cells, pagination layout, clipped text cells, and scroll pane styling. |
| **UniversalDatePickerHelper.java** | Styling helper for the single-date picker, popup calendar, calendar icon, and rounded field border. |
| **UniversalDateRangePickerHelper.java** | Styling helper for date-range picker layout, action buttons, calendar colors, display state, and icons. |
| **UniversalDialogHelper.java** | Styling helper for `UniversalDialog`, including accent colors, content layout, buttons, and message sections. |
| **DialogHelper.java** | Convenience wrapper for showing success, info, warning, error, and confirmation dialogs consistently. |

---

### Utility Layer

#### `com.kgm.util` Package

| File | Functionality |
| --- | --- |
| **SessionManager.java** | Stores and clears the current `UserSession`, checks expiry, and exposes remaining session time. |
| **SessionWatcher.java** | Monitors active session expiry and closes/redirects windows when the session is no longer valid. |
| **FileUtil.java** | Reserved file utility boundary for shared file handling logic. |
| **FilterUtil.java** | Reserved filtering utility boundary for reusable search/filter behavior. |
| **ValidationUtil.java** | Reserved validation utility boundary for shared input validation rules. |

---

### Assets and Runtime Data

| Path | Functionality |
| --- | --- |
| **images/** | Static UI images such as logo, header, login background, and login foreground artwork. |
| **employees/** | Runtime/sample employee file storage for profile images and uploaded documents. |
| **target/** | Maven build output. Generated and ignored by Git. |

---

## Data Flow Architecture

```text
1. User Login
   LoginView -> AuthService -> SessionManager -> SessionWatcher -> HomeView

2. Home Dashboard
   HomeView -> EmployeeRecordDao -> EmployeeTablePanel -> MySQL employees table

3. Employee Search
   HomeView search bar -> EmployeeRecordDao.getEmployeeByCode -> EmployeeTablePanel.showSingleEmployee

4. Add Employee Record
   HomeView -> EmployeeRegistrationView
   EmployeeRegistrationFormPanel + EmployeeDocumentUploadPanel -> EmployeeRegistrationDao -> MySQL

5. View Employee Details
   EmployeeTablePanel Action cell -> EmployeeDetailView -> EmployeeRecordDao.getFullEmployeeByCode

6. Update Employee Details
   EmployeeDetailView -> EmployeeBasicDetailsPanel / EmployeeAdditionalDetailsPanel
   -> EmployeeRecordDao.updateEmployeeDynamic -> MySQL

7. Document Handling
   EmployeeDocumentUploadPanel -> local employees/{employeeCode}/documents storage
   EmployeeDocumentViewPanel -> document availability and preview/download actions

8. Download Employee Report Package
   EmployeeDetailView -> EmployeeReportService -> EmployeeRecordDao.getFullEmployeeByCode
   -> generated PDF + copied employee documents in a selected local folder
```

---

## Database Structure

The primary table is defined in `src/main/resources/schema.sql` and initialized at startup by `DatabaseInitializer`.

| Table | Purpose |
| --- | --- |
| **employees** | Stores employee identity, employment, organization, payroll, banking, contact, reporting, compliance, benefits, vaccination, document paths, and profile image path. |

Key schema areas:

- Core identity: employee code, name, family details, gender, DOB, CNIC/NID.
- Employment: department, designation, grade, joining date, resignation date, status, shift.
- Organization: division, branch, reporting fields.
- Payroll and banking: salary, pay categories, bank account, SS/EOBI/tax/PF fields.
- Contact: phone, addresses, email, emergency number.
- Compliance and benefits: clearance, verification, wellness, vaccination.
- Documents: CNIC, EOBI, social security, settlement, clearance, appointment, service, retirement, disciplinary, and profile image paths.

---

## Technology Stack

| Component | Technology |
| --- | --- |
| Frontend | Java Swing (`JFrame`, `JPanel`, `JTable`, `JDialog`) |
| Backend Logic | Java 21 |
| Database | MySQL |
| Data Access | JDBC |
| Date/Time UI | JCalendar 1.4 |
| Excel Operations | Apache POI 5.2.5 |
| Logging Dependencies | SLF4J 2.0.13, Log4j 2.21.1 |
| Build Tool | Maven |
| Testing | JUnit 5 |

---

## Running the Application

### Prerequisites

- Java 21 or newer
- Maven 3.8 or newer
- MySQL 8.0 or newer

### Database Configuration

Database values can be passed through environment variables or JVM properties.

| Environment Variable | JVM Property | Purpose |
| --- | --- | --- |
| `KGM_DB_HOST` | `kgm.db.host` | MySQL host |
| `KGM_DB_PORT` | `kgm.db.port` | MySQL port |
| `KGM_DB_NAME` | `kgm.db.name` | Database name |
| `KGM_DB_USER` | `kgm.db.user` | Database username |
| `KGM_DB_PASSWORD` | `kgm.db.password` | Database password |

### Startup

```bash
# Compile
mvn -q -DskipTests compile

# Run from an IDE using:
com.kgm.Main
```

The application initializes the configured database and employee schema automatically on startup.

Default application login:

```text
Username: admin
Password: 1234
```

---

## File Count Summary

| Layer | Count | Purpose |
| --- | ---: | --- |
| Core entry point | 1 | Application bootstrap |
| Configuration | 2 | Database configuration and connection creation |
| Database initializer | 1 | Startup schema/database creation |
| DAO layer | 2 | Employee persistence, lookup, listing, and updates |
| Service layer | 4 | Authentication, employee report packaging, and future service boundaries |
| Models | 2 | Employee and user session data |
| UI views | 4 | Main application windows |
| UI dialogs | 1 | Reusable modal dialog |
| UI components | 2 | Reusable date controls |
| UI panels | 10 | Forms, tables, document panels, header/footer |
| UI styling helpers | 17 | Visual design, table styling, layout helpers |
| Utilities | 5 | Session and shared utility boundaries |
| Resources | 1 | SQL schema |
| **Total source/resource files** | **52** | Complete application code and schema |

---

## Naming Conventions

- **Views**: `*View.java` for top-level Swing windows, for example `LoginView`, `HomeView`, `EmployeeRegistrationView`.
- **Panels**: `*Panel.java` for reusable UI containers, with domain-specific names such as `EmployeeTablePanel` and `EmployeeDocumentUploadPanel`.
- **Dialogs**: `*Dialog.java` for modal dialog classes.
- **Components**: reusable controls use descriptive names such as `UniversalDatePicker`.
- **Styling/Layout Helpers**: `*Helper.java` for static Swing styling and layout utilities.
- **DAOs**: `*Dao.java` for JDBC/database access classes.
- **Services**: `*Service.java` for business logic boundaries.
- **Models**: domain nouns such as `Employee` and `UserSession`.
- **Resources**: SQL and non-Java files live in `src/main/resources`.

---

## Key Design Patterns

1. **DAO Pattern**: `EmployeeRegistrationDao` and `EmployeeRecordDao` isolate database access.
2. **Service Pattern**: `AuthService`, `EmployeeReportService`, `EmployeeService`, and `ExcelImportService` define business boundaries.
3. **Component Pattern**: Date pickers, dialogs, table panels, and document panels are reusable Swing components.
4. **Helper/Factory Pattern**: Styling helpers create consistent Swing components and layouts.
5. **Observer Pattern**: Swing listeners handle button clicks, document changes, table actions, and tab changes.
6. **Session Control Pattern**: `SessionManager` and `SessionWatcher` coordinate login state and expiry.

---

## Known Issues and Future Improvements

1. **Authentication**: Current credentials are hardcoded. Move authentication to database, LDAP, or another managed identity provider.
2. **Excel Import**: `ExcelImportService` is currently a service boundary and should be implemented behind `ExcelImportButton`.
3. **Service Boundaries**: Move validation and employee workflow rules out of UI classes into `EmployeeService` and `ValidationUtil`.
4. **Database Defaults**: Keep credentials externalized through environment variables or JVM properties for production use.
5. **Document Storage**: Replace local `employees/` file storage with managed storage, validation, and cleanup policies.
6. **Testing**: Add unit and integration tests for DAOs, services, session expiry, and form validation.
7. **Error Handling**: Improve user-facing messages and centralize exception handling for database/file operations.

---

## File Dependencies Quick Reference

```text
Main.java
|-- DatabaseInitializer
`-- LoginView
    |-- AuthService
    |-- SessionManager
    |-- SessionWatcher
    `-- HomeView
        |-- EmployeeRecordDao
        |-- EmployeeTablePanel
        |-- ExcelImportButton
        `-- EmployeeRegistrationView

EmployeeRegistrationView
|-- EmployeeRegistrationFormPanel
|-- EmployeeDocumentUploadPanel
|-- EmployeeRegistrationDao
`-- DialogHelper

EmployeeDetailView
|-- EmployeeRecordDao
|-- EmployeeReportService
|-- EmployeeBasicDetailsPanel
|-- EmployeeAdditionalDetailsPanel
|-- EmployeeDocumentViewPanel
`-- DialogHelper

All UI Views and Panels
|-- Styling helper classes
|-- UniversalDialog / DialogHelper
`-- Session-aware HeaderPanel and FooterPanel
```

---

## Contact and Support

**Company**: Kohinoor Textile Mills Gujar Khan Ltd.  
**Application**: KGM Ex-Employee Management System  
**Version**: 1.0.0

---

## License

Internal use only - Kohinoor Textile Mills Gujar Khan Ltd.

---

**Last Updated**: May 2026
