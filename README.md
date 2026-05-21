# KGM Ex-Employee Management System

## Project Overview

A desktop application for Kohinoor Textile Mills Gujar Khan Ltd. to manage ex-employee records, employee registration, searchable document uploads, employee lookup, detail review, selective document-package downloads, and session-controlled access. The application is built with Java Swing for the UI, MySQL for persistence, JDBC for data access, and Maven for project builds.

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
| **Main.java** | Application entry point. Prints the configured MySQL server location, initializes the database schema, starts on the Swing event thread, and opens `LoginView`. |

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
| **DatabaseInitializer.java** | Creates the configured MySQL database, ensures the `employees` table exists, adds required document columns for existing installs, migrates obvious legacy document paths, and ensures the employee-code search index. |

#### `src/main/resources`

| File | Functionality |
| --- | --- |
| **schema.sql** | Reference MySQL schema for the `employees` table and document path columns. |

---

### Data Access Layer

#### `com.kgm.dao` Package

| File | Functionality |
| --- | --- |
| **EmployeeRegistrationDao.java** | Inserts newly registered employee records with a generated column list, including profile image and the centralized document path fields. |
| **EmployeeRecordDao.java** | Reads employee records, supports indexed lookup/listing/counts, dynamically maps document fields, and updates only meaningful submitted employee data. |

---

### Business Logic Layer

#### `com.kgm.service` Package

| File | Functionality |
| --- | --- |
| **AuthService.java** | Handles login validation. Current credential check is `admin` / `1234`; this is the boundary for future DB or directory-based authentication. |
| **EmployeeService.java** | Reserved business-service boundary for employee workflow rules that should not live directly in UI or DAO classes. |
| **EmployeeReportService.java** | Generates employee download packages, including PDF profile, selected documents, and merged document PDFs. |
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
| **HomeView.java** | Main dashboard for employee search, listing, refresh, import entry point, and record creation. |
| **EmployeeRegistrationView.java** | Registration window for adding a new employee record. Combines employee form entry and document upload tabs before saving to MySQL. |
| **EmployeeDetailView.java** | Detail, update, document review, and download workflow for an existing employee. |

---

### Dialog Components

#### `com.kgm.ui.dialog` Package

| File | Functionality |
| --- | --- |
| **UniversalDialog.java** | Reusable modal dialog for app messages, confirmations, and review details. |

---

### Panel Components

#### `com.kgm.ui.panel` Package

| File | Functionality |
| --- | --- |
| **HeaderPanel.java** | Shared application header. Displays the current page title and session/logout controls. |
| **FooterPanel.java** | Shared application footer. Displays company text and optional trailing action space. |
| **EmployeeTablePanel.java** | Home dashboard employee table built on `UniversalTablePanel`, with paging and detail navigation. |
| **EmployeeRegistrationFormPanel.java** | Employee registration form with profile photo upload and core fields required to create a record. |
| **EmployeeDocumentUploadPanel.java** | Registration document upload panel with search, single/bulk upload, validation, and preview. |
| **EmployeeDocumentViewPanel.java** | Employee document review/update panel for viewing saved documents and uploading missing ones. |
| **EmployeeBasicDetailsPanel.java** | Basic employee detail form used in detail/edit flows. Allows updates to normal employee fields, keeps employee code locked, handles missing profile-image upload, and ignores empty placeholders such as `N/A`. |
| **EmployeeAdditionalDetailsPanel.java** | Additional employee detail panel for employment, payroll, banking, reporting, compliance, benefits, and vaccination fields. Allows normal employee-field updates while ignoring empty placeholders. |
| **ExcelImportButton.java** | Reusable styled button for triggering Excel import workflows. |
| **UniversalTablePanel.java** | Reusable paginated table component for links, actions, status cells, and responsive row-height layout. |

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
| **HomeViewHelper.java** | Styling and layout helper for the Home dashboard. |
| **LoginViewHelper.java** | Styling and component factory methods for the login screen, including background image panel, form layout, placeholder fields, and primary button. |
| **EmployeeRegistrationViewHelper.java** | Layout and style helper for the employee registration window, page header, tabs, action rows, buttons, and scroll behavior. |
| **EmployeeDetailViewHelper.java** | Facade/helper for employee detail layout, tabs, actions, and page scrolling. |
| **EmployeeDetailViewLayoutHelper.java** | Lower-level layout helper for employee detail frame and header structure. |
| **EmployeeRegistrationFormPanelHelper.java** | Styling helper for registration form layout, photo preview, form fields, labels, address areas, and upload controls. |
| **EmployeeBasicDetailsPanelHelper.java** | Styling proxy/helper for basic employee details, reusing form patterns from the registration form helper. |
| **EmployeeAdditionalDetailsPanelHelper.java** | Styling helper for additional details sections, breadcrumbs, grids, field rows, date fields, and return-to-top controls. |
| **EmployeeDocumentUploadPanelHelper.java** | Styling and layout helper for document upload tables and preview windows. |
| **EmployeeDocumentViewPanelHelper.java** | Styling proxy/helper for employee document viewing tables. |
| **TableThemeHelper.java** | Shared table theme constants, table styling, empty states, navigation controls, links, and reusable action panels. |
| **TablePaginationHelper.java** | Legacy/shared table factory and pagination helper for document tables. |
| **UniversalTablePanelHelper.java** | Rendering and layout helper for `UniversalTablePanel`. |
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
| **EmployeeDocumentUtil.java** | Shared document metadata, validation, path handling, filename matching, and bulk-upload matching for the 22 required document fields. |
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
   HomeView -> EmployeeRecordDao -> EmployeeTablePanel -> UniversalTablePanel -> MySQL employees table

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
   EmployeeDocumentUploadPanel -> dynamic search, single upload, or Upload All filename matching for the 22 configured document fields
   -> local employees/{employeeCode}/documents storage
   EmployeeDocumentViewPanel -> searchable saved documents, locked-document checks, single or Upload All upload for missing documents
   -> missing documents uploaded through detail update

8. Download Employee Report Package
   EmployeeDetailView selection dialog -> EmployeeReportService -> EmployeeRecordDao.getFullEmployeeByCode
   -> optional PDF profile + optional all-documents PDF + all or selected saved documents in a selected local folder
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
- Documents: 22 required document path columns plus the employee profile image path. Database columns use uppercase underscore names; UI labels use readable business names.

### Required Document Fields

The application uses `EmployeeDocumentUtil` as the single source of truth for document labels, database columns, storage filenames, search aliases, upload-all matching, detail-view locking, and report/download selection.

| # | Database Column | UI Label |
| ---: | --- | --- |
| 1 | `CNIC_FRONT` | CNIC Front |
| 2 | `CNIC_BACK` | CNIC Back |
| 3 | `EOBI` | EOBI |
| 4 | `SS_CARD` | Social Security Card |
| 5 | `FINAL_SETTLEMENT` | Final Settlement |
| 6 | `APPOINTMENT_LETTER_FRONT` | Appointment Letter Front |
| 7 | `APPOINTMENT_LETTER_BACK` | Appointment Letter Back |
| 8 | `APPLICATION_FRONT` | Application Front |
| 9 | `APPLICATION_BACK` | Application Back |
| 10 | `CLEARANCE_CERTIFICATE` | Clearance Certificate |
| 11 | `SERVICE_CERTIFICATE` | Service Certificate |
| 12 | `PAYMENT_VOUCHER` | Payment Voucher |
| 13 | `TRIAL_CARD` | Trial Card |
| 14 | `MEDICAL_DOC` | Medical |
| 15 | `INTERVIEW_FORMS` | Interview Forms |
| 16 | `COVID_CERTIFICATE` | Covid Certificate |
| 17 | `DISCIPLINARY_I` | Disciplinary I |
| 18 | `DISCIPLINARY_II` | Disciplinary II |
| 19 | `DISCIPLINARY_III` | Disciplinary III |
| 20 | `MISCELLANEOUS_I` | Miscellaneous I |
| 21 | `MISCELLANEOUS_II` | Miscellaneous II |
| 22 | `MISCELLANEOUS_III` | Miscellaneous III |

---

## Employee Update Rules

- Normal employee detail fields can be updated when the submitted value is real data.
- Empty values, blank strings, `N/A`, `n/a`, `NA`, `NULL`, and `-` are treated as placeholders and are not written as updates.
- `EMPLOYEE_CODE` is the record key and stays locked in the detail screen.
- Document fields are record-safe: if a document path already exists in the database, that document remains marked `Locked`, can be viewed from the detail screen, and cannot be replaced.
- If a document field is empty or a placeholder, the detail screen allows upload. The file is copied to `employees/{employeeCode}/documents/` and the database path is saved on Update.
- Profile image follows the same safety rule: it can be uploaded only when `EMP_IMG` is empty or a placeholder.
- Registration and detail document upload both support `Upload All` for multiple files. Each selected file must be JPG/JPEG, must be 400KB or smaller, and must match a document label, Employee field name, or storage filename after normalizing spaces, underscores, punctuation, and case.
- Upload matching accepts database-style names such as `SS_CARD` and user-facing names such as `Social Security Card`; files are saved using the configured storage filename for the matching document type.
- Detail document upload keeps saved DB document records locked. If `Upload All` includes a file matching an already-saved document, the file is skipped and the dialog explains that the document already exists in DB and cannot be replaced.
- After a bulk upload attempt, the user receives a summary showing how many documents are ready to save and which files were discarded with the reason.
- Employee detail downloads first ask what to include: PDF profile, all saved documents, `All Documents (PDF)`, or specific saved document names when `All saved documents` is turned off. Saved records with missing source files are still named in the picker so the user can see their status.
- `All Documents (PDF)` merges only available saved document images, excludes the employee profile photo, starts each document on a new page, preserves image size, splits tall images across pages when needed, and does not add a header or footer.

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
At startup, the console also prints `MySQL Server Running On` with the configured host and port.

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
| Utilities | 6 | Session, document matching, and shared utility boundaries |
| Resources | 1 | SQL schema |
| **Total source/resource files** | **53** | Complete application code and schema |

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
