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
- **Configuration Layer**: Environment-backed app settings, database settings, and connection creation.
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
| **AppConfig.java** | Loads `.env`, environment variables, and JVM properties for application-wide settings such as admin login and employee storage. |
| **DatabaseConfig.java** | Centralizes database host, port, name, username, and password through `AppConfig`. |
| **DatabaseConnection.java** | Creates JDBC connections for both server-level database creation and application-level database access. |

#### `com.kgm.database` Package

| File | Functionality |
| --- | --- |
| **DatabaseInitializer.java** | Creates the configured MySQL database, ensures the `employees` table exists, adds required document columns for existing installs, migrates obvious legacy document paths, and ensures employee-code/search/reporting indexes. |

#### `src/main/resources`

| File | Functionality |
| --- | --- |
| **schema.sql** | Reference MySQL schema for the `employees` table, document path columns, and employee field metadata. |
| **employee_field_metadata_default.json** | Bundled factory-default field metadata snapshot used only when a fresh/reset database has no valid external AppData metadata backup. |

---

### Data Access Layer

#### `com.kgm.dao` Package

| File | Functionality |
| --- | --- |
| **EmployeeRegistrationDao.java** | Inserts newly registered employee records with a generated column list, including profile image and the centralized document path fields. |
| **EmployeeRecordDao.java** | Reads employee records, supports indexed lookup/listing/counts, dynamically maps document fields, and updates only meaningful submitted employee data. |
| **EmployeeFieldDefinitionDao.java** | Manages editable employee field metadata, custom DB columns, Core field flags, date/dropdown behavior, document-field flags, dropdown options, and category heading renames. |

---

### Business Logic Layer

#### `com.kgm.service` Package

| File | Functionality |
| --- | --- |
| **AuthService.java** | Handles login validation using the configured admin username and password from env settings. |
| **EmployeeService.java** | Reserved business-service boundary for employee workflow rules that should not live directly in UI or DAO classes. |
| **EmployeeReportService.java** | Generates employee download packages, including PDF profile, selected documents, and merged document PDFs. |
| **ExcelImportService.java** | Parses employee Excel workbooks, supports standard/legacy import modes, validates required Basic fields, CNIC/date rules, maps known headers to DB fields, and rejects unknown headers with a sample-download prompt. |
| **ExcelSampleGenerator.java** | Generates the employee import sample workbook with all non-document fields, sample rows, dynamic dropdown valid values, and date/CNIC rules. Document fields are excluded. |

---

### Data Models

#### `com.kgm.model` Package

| File | Functionality |
| --- | --- |
| **Employee.java** | Entity model for employee personal, employment, payroll, contact, compliance, benefit, vaccination, document, and profile-image data. |
| **EmployeeFieldDefinition.java** | Metadata model for DB-backed fields, including display label, category heading, Core/detail/document usage, custom/built-in origin, date/dropdown behavior, variable-option behavior, dropdown options, and sort order. |
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
| **FieldManagementView.java** | Field-management window for filtering Built-in/Custom fields, adding custom DB fields, password-gated built-in edits, editing labels/headings/date/dropdown flags, editable dropdown options, variable-option behavior, document fields, and categories with `UniversalTablePanel`. |

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
| **EmployeeRegistrationFormPanel.java** | Employee registration form with profile photo upload and metadata-driven Basic built-in fields required to create a record. |
| **EmployeeDocumentUploadPanel.java** | Registration document upload panel with search, single/bulk upload, validation, and preview. |
| **EmployeeDocumentViewPanel.java** | Employee document review/update panel for viewing saved documents and uploading missing ones. |
| **DocumentImagePreviewPanel.java** | Image preview panel that scales document images for viewing while preserving original aspect ratio. |
| **EmployeeBasicDetailsPanel.java** | Metadata-driven Basic employee detail form used in detail/edit flows. Allows built-in field updates, keeps employee ID locked, handles missing profile-image upload, and ignores empty placeholders such as `N/A`. |
| **EmployeeAdditionalDetailsPanel.java** | Additional employee detail panel for employment, payroll, banking, reporting, compliance, benefits, and vaccination fields. Allows normal employee-field updates while ignoring empty placeholders. |
| **ExcelImportButton.java** | Reusable styled button for triggering Excel import and sample-download workflows. |
| **FileUploadCard.java** | Reusable upload/download card component used by employee photo, document, Excel, and report-package file actions. |
| **NativeFileDialog.java** | Shared native AWT file dialog helper for uploads, multi-file uploads, save targets, and report package folder targets. |
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
| **AppWindowStateHelper.java** | Keeps application screens full-size/maximized while still allowing users to minimize them. Login, native file dialogs, and modal dialogs are not routed through this helper. |
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
| **EmployeeBasicFieldUtil.java** | Single source of truth for Core employee field order, required-field rules, date/dropdown defaults, and labels used by Add Employee, Basic Detail, and Excel import/sample generation. |
| **EmployeeAdditionalFieldDefaults.java** | Seeds the 43 ERP/detail custom fields with database columns, Field Management categories, date/dropdown behavior, and Excel sample values. |
| **EmployeeFieldDefinitionCache.java** | Keeps field metadata in memory for the current app session so screens reuse one loaded catalog. It is refreshed only at startup warm-up or after Field Management changes. |
| **EmployeeFieldMetadataStore.java** | Reads/writes validated metadata JSON snapshots for external AppData backup, backup-copy recovery, bundled factory defaults, and local cache refresh. |
| **EmployeeDocumentUtil.java** | Shared document metadata, validation, path handling, filename matching, and bulk-upload matching for the 22 required document fields. |
| **FileUtil.java** | Reserved file utility boundary for shared file handling logic. |
| **FilterUtil.java** | Reserved filtering utility boundary for reusable search/filter behavior. |
| **ValidationUtil.java** | Reserved validation utility boundary for shared input validation rules. |

---

### Assets and Runtime Data

| Path | Functionality |
| --- | --- |
| **images/** | Static UI images such as logo, header, login background, and login foreground artwork. |
| **resources/employees/** | Runtime employee file storage for profile images and uploaded documents. The folder is created when needed and ignored by Git except for `.gitkeep`. |
| **%APPDATA%/KGM Ex-Employee Management/employee_field_metadata.json** | Latest user-change metadata backup. It is written after the DB update succeeds and is used first after a database drop/reset. |
| **%LOCALAPPDATA%/KGM Ex-Employee Management/cache/employee_field_metadata.cache.json** | Metadata cache for fast reload only. It is ignored whenever its schema version or DB checksum is not fresh. |
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
   EmployeeTablePanel Action cell -> EmployeeDetailView loading shell -> EmployeeRecordDao.getFullEmployeeByCode
   -> field metadata preload -> detail tabs render when data is ready

6. Update Employee Details
   EmployeeDetailView -> EmployeeBasicDetailsPanel / EmployeeAdditionalDetailsPanel
   -> EmployeeRecordDao.updateEmployeeDynamic -> MySQL

7. Document Handling
   EmployeeDocumentUploadPanel -> dynamic search, single upload, or Upload All filename matching for the 22 configured document fields
   -> configured local employee storage, saved in DB as employees/{employeeCode}/documents paths
   EmployeeDocumentViewPanel -> searchable saved documents, locked-document checks, single or Upload All upload for missing documents
   -> missing documents uploaded through detail update

8. Download Employee Report Package
   EmployeeDetailView selection dialog -> EmployeeReportService -> EmployeeRecordDao.getFullEmployeeByCode
   -> native save dialog -> optional PDF profile + optional all-documents PDF + all or selected saved documents in a selected local folder

9. Field and Category Management
   HomeView -> FieldManagementView -> EmployeeFieldDefinitionDao -> employees / employee_field_metadata
   -> dynamic Core/detail categories, document labels, custom fields, date-picker behavior, and dropdown options
```

---

## Database Structure

The primary table is defined in `src/main/resources/schema.sql` and initialized at startup by `DatabaseInitializer`.

| Table | Purpose |
| --- | --- |
| **employees** | Stores employee identity, employment, organization, payroll, banking, contact, reporting, compliance, benefits, vaccination, document paths, and profile image path. |
| **employee_field_metadata** | Stores editable field labels, category headings, Core/detail/document flags, custom/built-in origin, date/dropdown flags, variable-option flags, dropdown options, and sort order for dynamic UI/report/Excel behavior. |

The app keeps `employees` as the canonical employee row for Excel import/export, reports, and updates, but it no longer treats every screen as a `SELECT *` workflow. `EmployeeRecordDao` exposes header, Basic tab, Others tab, and Documents tab projections so each screen fetches only the columns it needs. Physical mini tables are kept for data that truly has a separate lifecycle, such as `employee_field_metadata`; this avoids unnecessary joins and complex migrations while still improving runtime performance.

Startup also ensures indexes for employee-code lookup and dashboard/reporting group fields (`DEPARTMENT`, `SECTION`, `GRADE`, `DESIGNATION`, and `RESIGN_REASON`) so home statistics and navigation filters stay responsive as the employee table grows.

Field metadata has layered durability. The database remains the active source while the app is running. Every Field Management add/edit/delete, required flag, category, dropdown, label, order, visibility, date, or text-area change writes the DB first, then atomically refreshes `%APPDATA%/KGM Ex-Employee Management/employee_field_metadata.json` and the local cache with schema version, timestamp, checksum, and a backup copy. On startup, the cache is used only when its DB checksum is fresh. If the metadata table is empty after a new setup, reset, or database drop, the initializer restores the latest valid AppData JSON first and recreates missing custom employee columns; it falls back to bundled `employee_field_metadata_default.json` only when the external backup is missing, corrupt, or from an unsupported schema version.

During a normal app session, field metadata is loaded into `EmployeeFieldDefinitionCache` once and reused by Add Employee, Employee Detail, document requirements, Excel import/export, reports, and missing-data checks. Field Management changes invalidate this in-memory cache after the DB and AppData JSON backup are saved, so the next screen refresh gets the latest metadata without every screen repeatedly querying DB or rereading JSON.

Key schema areas:

- Basic built-in employee fields: Employee ID, Name, Father Name, CNIC, Phone, Email, Department, Designation, Section, Grade, Shift, Date of Birth, Gender, Resign Reason, Date of Joining, Date of Resignation, and Permanent Address.
- Core identity: employee code, name, family details, gender, DOB, CNIC/NID.
- Employment: department, designation, grade, section, joining date, resignation date, status, shift.
- Organization: division, branch, reporting fields.
- Payroll and banking: salary, pay categories, bank account, SS/EOBI/tax/PF fields.
- Contact: phone, addresses, email, emergency number.
- Compliance and benefits: clearance, verification, wellness, vaccination.
- Documents: 22 required document path columns plus the employee profile image path. Database columns use uppercase underscore names; UI labels use readable business names.
- Field metadata: category headings, labels, Core/detail/document behavior, custom origin, date/dropdown behavior, variable-option behavior, dropdown options, and custom fields are stored separately so Field Management changes appear throughout forms, detail views, document panels, reports, and Excel import.

### Required Basic Employee Fields

`EmployeeBasicFieldUtil` defines the default Core field order used by Add Employee, the first tab of Employee Detail, and the Excel import sample:

`Employee ID`, `Name`, `Father Name`, `CNIC`, `Phone`, `Email`, `Department`, `Designation`, `Section`, `Grade`, `Shift`, `Date of Birth`, `Gender`, `Resign Reason`, `Date of Joining`, `Date of Resignation`, `Permanent Address`.

These fields are seeded as Core and placed in the `Fundamentals` category in `employee_field_metadata`. Add Employee and the first tab of Employee Detail render fields whose category heading is `Fundamentals`, with the Core fields ordered first. A field created or moved into `Fundamentals` is promoted to built-in/protected behavior and appears in those forms. Existing records receive `N/A` for new non-date fields; new date fields stay empty so the picker displays `Choose Date`. Fields currently in `Fundamentals` are required for standard Excel import and cannot be deleted as custom fields.

Field origin is categorized like this:

- **Built-in**: Core fields plus document fields.
- **Documents**: fields marked `document_field = 1`; they are handled by document upload/view panels and excluded from Excel.
- **Custom**: every non-document field that is not Core, including older payroll/HR/banking columns and user-created fields.
- **System internal**: `ID` stays protected and is not part of forms or Excel import.

Seeded custom detail fields are also created/kept during startup so Field Management, Employee Detail, Excel import, and Excel export share the same database columns and categories:

- **Organization / Structure**: `DEPT_CODE`.
- **Payroll / Allowances**: `PAY_SHEET`, `H_RENT`, `H_MAINTENANCE`, `EXTRA_DUTY_ALLOWANCE_DATE`.
- **Personal / HR Details**: `CITY_VILLAGE`, `DISTRICT`, `REFERENCE`, `RELATIVE_DETAIL`, `REFEMP_NAME`, `REFEMP_DESIG`, `REFEMP_DEPT`, `CNIC_EXP_DATE`, `CNIC_FAMILY_NO`, `CNIC_ISSUANCE_DATE`.
- **Employment Details**: `REST_DAY`, `STAFF`, `PRE_WORKEXP`, `CARDNO`, `CHEST_CARD_STATUS`, `REHIRING_STATUS`, `TAILOR_CATEGORY`, `VAC_ID`.
- **Benefits / Housing**: `COLONY_RESIDENT`, `COMPANY_CAR`, `PERSONAL_HOUSE_RENT`, `COLONY_HOUSE_NUMBER`.
- **Compliance / Status**: `SS`, `DED_UNION`.
- **Reporting**: `REPORT_TO_EMP_ID`, `REPORT_TO_UNT`.
- **IT Access**: `USER_ID`, `IT_EQUIPMENT`, `IT_EMAIL`, `IT_INTERNET`, `INTERNET_JUSTIFY`, `IT_SERVICE_ALERT`.
- **Alternate Saturday**: `ALT_SAT_TEAM`, `ALT_SAT_START_DATE`, `ALT_SAT_END_DATE`, `ALT_SAT_NEXT_YEAR`, `ALT_SAT_SHUFFLE`, `ALT_SAT_UNLOCK_NEXT_YEAR`.

Dropdown behavior is metadata-driven through `dropdown_field`, `variable_option_field`, and `dropdown_options`. `Gender` and `Resign Reason` are seeded as dropdowns. Any future non-document field can be marked as a dropdown in Field Management, where each option can be edited or removed. Fixed dropdowns only allow listed options, while variable dropdowns allow typing with prefix suggestion from existing options. Existing old values that are no longer in the option list still display for saved records, but they are not added back to the saved option list.

### Excel Import

The Home dashboard Excel action supports importing `.xlsx` / `.xls` files and downloading a generated `.xlsx` sample. The sample is rebuilt from current Field Management metadata every time it is downloaded, includes all non-document fields, excludes document/image columns, includes a Valid Values sheet with dynamic dropdown options, and states the date format `M/d/yyyy HH:mm:ss`.

Standard import requires every field currently in the `Fundamentals` category. Other non-document fields are optional and may appear in any order. CNIC must contain exactly 13 digits, and Date of Joining must be before Date of Resignation. Unknown, renamed, document, or unsupported extra headers are rejected so users should download the current sample when field settings change.

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
- If a document field is empty or a placeholder, the detail screen allows upload. The file is copied under `KGM_EMPLOYEE_STORAGE_DIR` and the logical database path is saved as `employees/{employeeCode}/documents/{file}` on Update.
- Profile image follows the same safety rule: it can be uploaded only when `EMP_IMG` is empty or a placeholder.
- Registration and detail document upload both support `Upload All` for multiple files. Each selected file must be JPG/JPEG, must be 400KB or smaller, and must match a document label, Employee field name, or storage filename after normalizing spaces, underscores, punctuation, and case.
- Upload matching accepts database-style names such as `SS_CARD` and user-facing names such as `Social Security Card`; files are saved using the configured storage filename for the matching document type.
- Detail document upload keeps saved DB document records locked. If `Upload All` includes a file matching an already-saved document, the file is skipped and the dialog explains that the document already exists in DB and cannot be replaced.
- After a bulk upload attempt, the user receives a summary showing how many documents are ready to save and which files were discarded with the reason.
- Employee detail downloads first ask what to include: PDF profile, all saved documents, `All Documents (PDF)`, or specific saved document names when `All saved documents` is turned off. Saved records with missing source files are still named in the picker so the user can see their status.
- `All Documents (PDF)` merges only available saved document images, excludes the employee profile photo, starts each document on a new page, preserves image size, splits tall images across pages when needed, and does not add a header or footer.
- Employee detail navigation paints an immediate shell before database queries run, then uses the shared `LoadingOverlay` instead of skeleton placeholders. The header loads first, then the Basic, Others, and Documents tabs lazy-load their own field projections only when opened. Dashboard, Download Profile, and Update actions stay fixed outside the tab scroll area.
- Dashboard navigation paints `HomeView` first and loads employee rows inside the table area instead of blocking the whole screen; startup still shows Login immediately while database initialization runs behind `LoadingOverlay`. Home uses a single employee-summary query for the table, prepares table rows off the Swing UI thread, keeps navigation usable while data loads, and lazy-loads analytics afterward. Required-field/document missing counts use one conditional aggregate query instead of one query per field.
- Long operations use the shared `LoadingOverlay`, which blocks the active window with progress text while DB/file work runs in a background worker. This covers employee-detail saves, registration saves, tab loading, Excel import/export/sample saving, and report package generation.
- File uploads, multi-file document uploads, Excel save/import actions, and employee report package downloads use the shared `FileUploadCard` / `NativeFileDialog` path so the app avoids old Swing file chooser surfaces.

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

Create a local `.env` file from `.env.example`, then replace the dummy values with the machine's real settings. `.env` is intentionally ignored by Git.

Settings can be passed through JVM properties, OS environment variables, or `.env`. JVM properties win first, then OS environment variables, then `.env`, then safe defaults.

| Environment Variable | JVM Property | Purpose |
| --- | --- | --- |
| `KGM_DB_HOST` | `kgm.db.host` | MySQL host |
| `KGM_DB_PORT` | `kgm.db.port` | MySQL port |
| `KGM_DB_NAME` | `kgm.db.name` | Database name |
| `KGM_DB_USER` | `kgm.db.user` | Database username |
| `KGM_DB_PASSWORD` | `kgm.db.password` | Database password |
| `KGM_ADMIN_USER` | `kgm.admin.user` | Application admin username |
| `KGM_ADMIN_PASSWORD` | `kgm.admin.password` | Application admin password |
| `KGM_EMPLOYEE_STORAGE_DIR` | `kgm.employee.storage.dir` | Local folder for employee photos and documents |

### Startup

```bash
# Compile
mvn -q -DskipTests compile

# Run from an IDE using:
com.kgm.Main
```

The application initializes the configured database and employee schema automatically on startup.
At startup, the console also prints `MySQL Server Running On` with the configured host and port.

### Field Metadata Backup and DB Drop Behavior

Yes. Field Management changes are dynamic and are saved outside the database after each successful metadata operation. The app writes the DB change first, then refreshes `%APPDATA%/KGM Ex-Employee Management/employee_field_metadata.json` plus a local cache/backup copy.

If the MySQL database is dropped or recreated, startup restores the latest valid AppData metadata backup first and recreates missing custom employee columns. The bundled `employee_field_metadata_default.json` is used only as a fallback when no valid local backup exists. Do not delete the AppData metadata file if the latest field setup must be restored after a DB reset.

Application login is configured in `.env`:

```text
KGM_ADMIN_USER=...
KGM_ADMIN_PASSWORD=...
```

---

## File Count Summary

| Layer | Count | Purpose |
| --- | ---: | --- |
| Java source files | 83 | Application code under `src/main/java` |
| Bundled resources | 2 | SQL schema and field metadata defaults under `src/main/resources` |
| **Total source/resource files** | **85** | Generated outputs and runtime employee files are excluded |

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
2. **Excel Import**: Add focused automated tests for workbook parsing, header rejection, duplicate employee IDs, and date/CNIC validation.
3. **Service Boundaries**: Move validation and employee workflow rules out of UI classes into `EmployeeService` and `ValidationUtil`.
4. **Database Defaults**: Keep credentials externalized through environment variables or JVM properties for production use.
5. **Document Storage**: Extend the configurable local employee storage with managed retention, validation, and cleanup policies.
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
        |-- FieldManagementView
        `-- EmployeeRegistrationView

FieldManagementView
|-- EmployeeFieldDefinitionDao
|-- UniversalTablePanel
|-- EmployeeDocumentUtil
`-- DialogHelper

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
