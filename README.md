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
| **EmployeeDocumentUtil.java** | Shared document metadata, validation, JPEG upload preparation/compression, path handling, filename matching, and bulk-upload matching for the required document fields. |
| **FileUtil.java** | Reserved file utility boundary for shared file handling logic. |
| **FilterUtil.java** | Reserved filtering utility boundary for reusable search/filter behavior. |
| **ValidationUtil.java** | Reserved validation utility boundary for shared input validation rules. |

---

### Assets and Runtime Data

| Path | Functionality |
| --- | --- |
| **src/main/resources/images/** | Static UI images such as logo, header, login background, and login foreground artwork. These are bundled inside the shaded JAR/EXE. |
| **src/main/resources/resources/Labels.txt** | Optional document label/alias reference bundled with the app for Upload All and bulk document matching. |
| **employees/** or configured storage path | Runtime employee file storage for profile images and uploaded documents. The selected folder is prepared at startup and receives its own `.gitignore` guard so uploaded files are not committed. |
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
   EmployeeDocumentUploadPanel -> dynamic search, single upload, or Upload All filename matching for the configured document fields
   -> configured employee storage root, saved in DB as employees/{employeeCode}/{file} paths
   EmployeeDocumentViewPanel -> searchable saved documents, locked-document checks, single or Upload All upload for missing documents
   -> missing documents uploaded through detail update
   Home bulk upload -> reads KGM_EMPLOYEE_STORAGE_DIR -> scans direct Employee-Code child folders only
   -> exact folder-name-to-EMPLOYEE_CODE match -> skips already saved DB/storage images -> grouped responsive result dialog

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
- Documents: configured document path columns plus the employee profile image path. Database columns use uppercase underscore names; UI labels use readable business names.
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
| 10 | `RESIGN_APPLICATION` | Resign Application |
| 11 | `CLEARANCE_CERTIFICATE` | Clearance Certificate |
| 12 | `SERVICE_CERTIFICATE` | Service Certificate |
| 13 | `PAYMENT_VOUCHER` | Payment Voucher |
| 14 | `TRIAL_CARD` | Trial Card |
| 15 | `MEDICAL_DOC` | Medical |
| 16 | `INTERVIEW_FORMS` | Interview Forms |
| 17 | `COVID_CERTIFICATE` | Covid Certificate |
| 18 | `DISCIPLINARY_I` | Disciplinary I |
| 19 | `DISCIPLINARY_II` | Disciplinary II |
| 20 | `DISCIPLINARY_III` | Disciplinary III |
| 21 | `MISCELLANEOUS_I` | Miscellaneous I |
| 22 | `MISCELLANEOUS_II` | Miscellaneous II |
| 23 | `MISCELLANEOUS_III` | Miscellaneous III |

### Document Upload Status Colors and States

The document upload feature uses color-coding and status text to communicate the state of each document in the upload workflow. All status indicators are implemented in `EmployeeDocumentUploadPanel` with color styling from `TablePaginationHelper`.

#### Document Upload States and Their Colors

| Status | Description | Color | RGB | Hex Code | Use Case |
| --- | --- | --- | --- | --- | --- |
| **Uploaded** | Document file successfully uploaded and saved to database | Green | 38, 128, 64 | #268040 | File is ready, stored in the system, and can be viewed or replaced |
| **Not Uploaded** | Document has not been uploaded yet (optional field) | Gray | 99, 115, 129 | #637381 | Optional document that is pending upload or intentionally skipped |
| **Missing required** | Required document is not uploaded (must be provided) | Red | 180, 60, 45 | #B43C2D | Critical missing document that blocks employee registration or detail completion |
| **Ready to save** / **Pending** | Document has been selected and is queued for database save | Blue | 0, 112, 210 | #0070D2 | Document is in the upload queue and waiting to be persisted to the database |

#### Status Display Locations

Status indicators appear in the following locations within the document upload interface:

- **Document Table Status Column**: Each document row displays its current status in the `Status` column of the `EmployeeDocumentUploadPanel` table.
- **File Info**: When a file is uploaded, the status displays as `Uploaded (X KB)` or `Uploaded (X MB)` to show both state and file size.
- **Required Indicator**: Required documents display an asterisk (`*`) next to their label (e.g., `CNIC Front *`) to distinguish them from optional documents.
- **Search Filtering**: When searching for documents, status keywords like "uploaded", "missing", or "required" are searchable and help locate specific documents.

#### Status Transitions and Workflows

1. **New Document (Not Uploaded → Ready to save)**
   - User selects a file to upload
   - Status changes from `Not Uploaded` to `Uploaded (file size)`
   - Document becomes ready to save when the employee record is submitted

2. **Required Missing Document (Missing required → Uploaded)**
   - System flags required documents as `Missing required` if not provided
   - User must upload a file to clear this status
   - Once uploaded, status changes to `Uploaded (file size)`
   - Employee cannot be registered/updated without clearing this status

3. **Replace Existing Document (Uploaded → Replace)**
   - In the detail view, already-saved documents show as `Locked`
   - Users see a `Replace` button instead of `Upload` for locked documents
   - Clicking `Replace` opens the file chooser to select a new file
   - On successful replacement, status updates to show new file size

4. **Bulk Upload Scenario**
   - User selects multiple files via `Upload All`
   - System matches filenames to document types (e.g., `CNIC_Front.jpg` → `CNIC Front`)
   - Matched documents transition to `Uploaded (file size)`
   - Unmatched files are reported in a summary dialog with discard reasons
   - User receives feedback on upload count and discarded files

#### Color Palette Consistency

The document upload status colors are part of a broader UI color palette defined in `TablePaginationHelper.DOCUMENT_STATUS_COLORS`:

| Color Name | RGB | Hex Code | Used For |
| --- | --- | --- | --- |
| **Green** | 38, 128, 64 | #268040 | Uploaded, Locked, Success states |
| **Blue (Primary)** | 0, 112, 210 | #0070D2 | Ready to save, Pending, Primary actions |
| **Red (Danger)** | 180, 60, 45 | #B43C2D | Missing required, Error, Failed states |
| **Amber** | 245, 158, 11 | #F59E0B | Warning and attention states |
| **Purple** | 139, 92, 246 | #8B5CF6 | Dynamic/unknown status types generated via hash-based color assignment for consistency |
| **Pink** | 236, 72, 153 | #EC4899 | Dynamic/unknown status types generated via hash-based color assignment for consistency |
| **Teal** | 20, 184, 166 | #14B8A6 | Dynamic/unknown status types generated via hash-based color assignment for consistency |
| **Orange** | 249, 115, 22 | #F97316 | Dynamic/unknown status types generated via hash-based color assignment for consistency |

#### UI Component Colors

Additional styling colors used in the document upload interface:

| Component | Color | RGB | Hex Code | Usage |
| --- | --- | --- | --- | --- |
| **Text (Primary)** | Dark Gray | 35, 43, 54 | #232B36 | Document names, labels, field text |
| **Text (Secondary)** | Medium Gray | 99, 115, 129 | #637381 | Descriptive text, hints, disabled text |
| **Background (Page)** | White | 255, 255, 255 | #FFFFFF | Main panel background |
| **Background (Card)** | Light Gray | 248, 250, 252 | #F8FAFC | Summary and control panels |
| **Border** | Light Gray | 220, 226, 232 | #DCE2E8 | Table borders, panel borders |
| **Cell Divider** | Very Light Gray | 232, 236, 240 | #E8ECF0 | Table cell separators |
| **Row Selection** | Light Blue | 229, 242, 255 | #E5F2FF | Highlighted table rows |
| **Field Border** | Medium Gray | 200, 200, 200 | #C8C8C8 | Text field and search box borders |

#### Accessibility Notes

- Color-blind friendly: Status indicators combine colors with clear text labels (e.g., "Uploaded", "Missing required") so users are not reliant on color alone.
- Text contrast: All status text meets WCAG contrast requirements for readability.
- Interactive elements: Action buttons (`Upload`, `Replace`, `View`) respond to both color and text state changes.

---

## Employee Update Rules

- Normal employee detail fields can be updated when the submitted value is real data.
- Empty values, blank strings, `N/A`, `n/a`, `NA`, `NULL`, and `-` are treated as placeholders and are not written as updates.
- `EMPLOYEE_CODE` is the record key and stays locked in the detail screen.
- Document fields are record-safe: if a document path already exists in the database, that document remains marked `Locked`, can be viewed from the detail screen, and cannot be replaced.
- If a document field is empty or a placeholder, the detail screen allows upload. The file is copied under the active employee storage root and the logical database path is saved as `employees/{employeeCode}/{file}` on Update.
- Profile image follows the same safety rule: it can be uploaded only when `EMP_IMG` is empty or a placeholder.
- Registration and detail document upload both support `Upload All` for multiple files. Each selected file must be a real JPG/JPEG image and must match a document label, Employee field name, storage filename, or supported alias from the bundled `src/main/resources/resources/Labels.txt` after normalizing spaces, underscores, punctuation, and case.
- Document/photo upload size is controlled by `AppConfig.documentUploadMaxBytes()`, which reads `kgm.document.upload.max.bytes`, then `KGM_DOCUMENT_UPLOAD_MAX_BYTES`, then the compatible `.env` alias `DOCUMENT_UPLOAD_MAX_BYTES`, then defaults to `409600` bytes (400 KB). Commas and underscores are accepted in the value, for example `614,400` or `1_024_000`.
- If a selected JPG/JPEG is above the configured upload limit, the app dynamically searches for the best JPEG quality that fits before saving. It does not resize, crop, rotate, or scale the image, and it verifies that the compressed width and height match the original image.
- Compression applies to employee photo uploads, Add Employee document uploads, View Employee Details document uploads, `Upload All`, and single upload/replace for missing documents. Home dashboard bulk folder upload follows `BULK_IMPORT_COMPRESSION`: when `true`, oversized JPG/JPEG files are compressed using `KGM_DOCUMENT_UPLOAD_MAX_BYTES`; when `false`, valid JPG/JPEG files are copied as-is. Excel import/export is not part of this flow.
- Home dashboard bulk upload reads the configured `KGM_EMPLOYEE_STORAGE_DIR`; users do not select folders manually. Put one direct child folder per `Employee-Code` inside the employee storage folder. The folder name must exactly match `EMPLOYEE_CODE` in the database. Nested folders are ignored.
- Bulk upload saves time by rejecting unsupported file types, unmatched names, locked/already-saved DB documents, existing storage files with the same saved name, and duplicate matches before compression work starts. Compressed temp files are cleaned after home bulk copies and when pending panel uploads are replaced or cleared.
- If a JPG/JPEG cannot be compressed under the configured limit without keeping the same dimensions, the file is rejected and the user sees the normal upload warning.
- Upload matching accepts database-style names such as `SS_CARD` and user-facing names such as `Social Security Card`; files are saved using the configured storage filename for the matching document type.
- Detail document upload keeps saved DB document records locked. If `Upload All` includes a file matching an already-saved document, the file is skipped and the dialog explains that the document already exists in DB and cannot be replaced.
- After a bulk upload attempt, the user receives a responsive UniversalDialog-style summary grouped by `Employee-Code`, including uploaded images, skipped images, missing employee codes, invalid/unreadable folders, compression status, and errors. Large result content scrolls inside the dialog.
- Startup opens Login immediately. Employee storage, MySQL, and field/document metadata preparation run in the background while the user types credentials. If checks are still running after login, the loader shows percentage-based phases: LAN/storage reconnect, DB/schema preparation, field/document metadata, and ready.
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

- Java 21 JDK or newer. `jpackage` is included only with a full JDK, not a JRE.
- Maven 3.8 or newer.
- MySQL Server 8.0 or newer.
- Git for `update-exe.ps1`.

Official installers:

- Java 21 JDK: `https://adoptium.net/temurin/releases/?version=21`
- Maven: `https://maven.apache.org/download.cgi`
- MySQL Windows Installer: `https://dev.mysql.com/downloads/windows/installer/`

### Clone and First Setup

```powershell
git clone <your-repo-url>
cd KGM_Ex_Employees-main
Copy-Item .env.example .env
```

Edit `.env` and replace every dummy value with the machine's real values.

### Database Configuration

Create a local `.env` file from `.env.example`, then replace the dummy values with the machine's real settings. `.env` is intentionally ignored by Git. Packaged EXE installs also check `config\.env` beside the EXE, so the output folder can keep its own machine-specific configuration between updates.

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
| `FIELD_SETTINGS` | `kgm.field.settings.password` | Field Management password for adding, editing, deleting, or changing required field settings |
| `KGM_EMPLOYEE_STORAGE_ON_SERVER` | `kgm.employee.storage.on.server` | `false` stores employee files in a local folder on this PC/app install; `true` stores employee files in a shared UNC/LAN folder |
| `KGM_EMPLOYEE_STORAGE_DIR` | `kgm.employee.storage.dir` | The only employee storage path. Use `resources/employees` or another local path for local mode, or a UNC path like `\\192.168.2.93\employees` for shared-folder mode. Home dashboard bulk upload scans this same folder. If blank or missing in `.env` while local mode is active, startup writes `resources/employees` and uses that app-relative folder |
| `BULK_IMPORT_COMPRESSION` | `kgm.bulk.import.compression` | `true` compresses oversized JPG/JPEG files during Home bulk import; `false` uploads valid JPG/JPEG files as-is |
| `KGM_DOCUMENT_UPLOAD_MAX_BYTES` / `DOCUMENT_UPLOAD_MAX_BYTES` | `kgm.document.upload.max.bytes` | Maximum prepared JPG/JPEG document/photo upload size in bytes; files above it use the best JPEG quality that fits when compression is enabled |
| `KGM_ENV_FILE` | `kgm.env.file` | Optional absolute path to a specific `.env` file |

Employee documents keep logical database paths such as `employees/EMP001/CNIC_FRONT.jpg`; only the physical storage root changes. Startup, Add Employee, View Employee File, Upload All, bulk folder upload, preview, and report/download flows all resolve through `EmployeeStorageUtil`, so changing the storage mode or path in `.env` moves future reads/writes to the same root without code changes. Existing legacy files under `resources/employees`, root `employees`, and older nested paths such as `employees/EMP001/documents/CNIC_FRONT.jpg` remain readable as fallbacks when a saved DB path points there.

For a protected app-local folder, keep:

```text
KGM_EMPLOYEE_STORAGE_ON_SERVER=false
KGM_EMPLOYEE_STORAGE_DIR=resources/employees
```

For a LAN/shared folder, use a UNC path that the Windows user can access:

```text
KGM_EMPLOYEE_STORAGE_ON_SERVER=true
KGM_EMPLOYEE_STORAGE_DIR=\\192.168.2.93\employees
```

For Home dashboard bulk import, use the same employee storage folder:

```text
KGM_EMPLOYEE_STORAGE_DIR=resources/employees
BULK_IMPORT_COMPRESSION=true
KGM_DOCUMENT_UPLOAD_MAX_BYTES=600000
```

The same storage folder may also point to a LAN/shared folder:

```text
KGM_EMPLOYEE_STORAGE_DIR=D:/EmployeeDocuments
# or
KGM_EMPLOYEE_STORAGE_DIR=\\192.168.2.93\employees
```

Expected source-folder layout:

```text
resources/employees/
  1/
    CNIC_FRONT.jpg
    CNIC_BACK.jpg
  2/
    FINAL_SETTLEMENT.jpg
```

Only folders directly inside `KGM_EMPLOYEE_STORAGE_DIR` are scanned, and each folder name is treated as the exact `Employee-Code`. Files inside nested folders are ignored. If a file is already physically in the employee storage folder but the DB path is missing, bulk import saves the DB path without copying the file again.

### Startup

```powershell
# Compile quickly
mvn -q compile

# Run from Maven
mvn exec:java

# Run the shaded JAR after packaging
mvn package
java -jar target\kgm-ex-employee-management-1.0.0.jar
```

The application initializes the configured database and employee schema automatically on startup.
At startup, the console also prints `MySQL Server Running On` with the configured host and port.
If `.env` is missing, MySQL is not installed, the server is unreachable, or credentials fail, a setup screen appears instead of crashing. It shows MySQL install/setup steps, `.env` values, technical details, automatic retry, and a `Retry Connection` button. When setup becomes valid, the normal login screen opens.

### Build the Shaded JAR

```powershell
mvn package
```

The shaded runnable JAR is generated at:

```text
target\kgm-ex-employee-management-1.0.0.jar
```

### Create or Update the Windows EXE

Use one of these commands from the project root:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-exe.ps1 -OutputDir "D:\KGM-eX-Employees-App" -CleanTarget
```

```powershell
powershell -ExecutionPolicy Bypass -File .\update-exe.ps1 -OutputDir "D:\KGM-eX-Employees-App" -CleanTarget
```

`build-exe.ps1` checks Java, Maven, and `jpackage`, builds the shaded JAR, creates a jpackage app image, and copies the EXE into the output folder.

`update-exe.ps1` does the same work after `git pull`.

Generated EXE:

```text
D:\KGM-eX-Employees-App\KGM Ex Employees.exe
```

The scripts replace only generated app files: `KGM Ex Employees.exe`, `app\`, and `runtime\`. They preserve these folders on every build/update:

```text
config\
employees\
resources\employees\
images\uploads\
logs\
backups\
```

If project `.env` exists and the output does not already have `config\.env`, the script copies it once. Existing `config\.env` is preserved so production credentials and storage paths are not overwritten by updates.

### Field Metadata Backup and DB Drop Behavior

Yes. Field Management changes are dynamic and are saved outside the database after each successful metadata operation. The app writes the DB change first, then refreshes `%APPDATA%/KGM Ex-Employee Management/employee_field_metadata.json` plus a local cache/backup copy.

If the MySQL database is dropped or recreated, startup restores the latest valid AppData metadata backup first and recreates missing custom employee columns. The bundled `employee_field_metadata_default.json` is used only as a fallback when no valid local backup exists. Do not delete the AppData metadata file if the latest field setup must be restored after a DB reset.

Application login is configured in `.env`:

```text
KGM_ADMIN_USER=...
KGM_ADMIN_PASSWORD=...
FIELD_SETTINGS=...
```

If `FIELD_SETTINGS` is not set, Field Management falls back to `KGM_ADMIN_PASSWORD` so existing installs keep working.

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

**Last Updated**: June 2026
