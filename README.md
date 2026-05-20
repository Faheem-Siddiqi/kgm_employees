# KGM Ex-Employee Management System

Java Swing desktop application for managing ex-employee records, induction data, document uploads, authentication, and MySQL-backed employee lookup.

## Technology Stack

- Java 21
- Swing for desktop UI
- Maven for build and dependency management
- MySQL for persistence
- Apache POI for Excel import support
- JCalendar for date picker components

## Quick Start

1. Install JDK 21 and Maven.
2. Configure MySQL connection values through environment variables or JVM properties:
   - `KGM_DB_HOST`
   - `KGM_DB_PORT`
   - `KGM_DB_NAME`
   - `KGM_DB_USER`
   - `KGM_DB_PASSWORD`
3. Build the project:

```bash
mvn -q -DskipTests compile
```

4. Run the application from `com.kgm.Main`.

Default login in the current code is `admin` / `1234`.

## Architecture

The project follows a simple layered structure:

- `config`: database configuration and connection creation.
- `database`: schema initialization.
- `dao`: database access for employee records.
- `model`: employee and session data objects.
- `service`: application services such as authentication and import boundaries.
- `ui`: Swing windows, panels, dialogs, reusable controls, and styling helpers.
- `util`: shared utility boundaries.

## File Catalog

| File | Functionality |
| --- | --- |
| `.gitignore` | Excludes Maven build output, compiled classes, IDE metadata, and runtime upload folders from future commits. |
| `.vscode/settings.json` | Local VS Code workspace preferences. |
| `pom.xml` | Maven project metadata, Java 21 compiler configuration, and dependency declarations. |
| `images/Header.jpg` | Header image asset used by the Swing UI. |
| `images/LoginBG.png` | Login screen background image. |
| `images/LoginTransparent.png` | Login screen transparent foreground/branding image. |
| `images/Logo.jpg` | Application logo image. |
| `employees/12/documents/CNIC_COPY.jpg` | Sample stored employee CNIC document. |
| `employees/12/documents/EOBI_CARD_COPY.jpg` | Sample stored employee EOBI document. |
| `employees/34/documents/SS_CARD_COPY.jpg` | Sample stored employee social security document. |
| `employees/documents/CNIC_COPY.jpg` | Sample common CNIC document. |
| `employees/documents/EOBI_CARD_COPY.jpg` | Sample common EOBI document. |
| `employees/documents/FINAL_SETTLEMENT.jpg` | Sample common final settlement document. |
| `employees/fa/EMP_IMG.jpg` | Sample employee profile image. |
| `employees/sfsd/documents/INTERVIEW_DOC.jpg` | Sample stored interview document. |
| `src/main/java/com/kgm/Main.java` | Application entry point; initializes the database and opens the login screen on the Swing event thread. |
| `src/main/java/com/kgm/config/DatabaseConfig.java` | Centralizes database settings from JVM properties, environment variables, or defaults. |
| `src/main/java/com/kgm/config/DatabaseConnection.java` | Creates JDBC connections using `DatabaseConfig`. |
| `src/main/java/com/kgm/dao/EmployeeDao.java` | Persists employee induction data and document paths. |
| `src/main/java/com/kgm/dao/EmployeeRepositoryDao.java` | Reads, counts, searches, and updates employee records from MySQL. |
| `src/main/java/com/kgm/database/DatabaseInitializer.java` | Creates the configured database and applies the employee schema at startup. |
| `src/main/java/com/kgm/model/Employee.java` | Employee data model with fields matching the database schema. |
| `src/main/java/com/kgm/model/UserSession.java` | Immutable user session model. |
| `src/main/resources/schema.sql` | MySQL DDL for the `employees` table and employee document columns. |
| `src/main/java/com/kgm/service/AuthService.java` | Login validation service. |
| `src/main/java/com/kgm/service/EmployeeService.java` | Reserved employee business-service boundary. |
| `src/main/java/com/kgm/service/ExcelImportService.java` | Reserved Excel import service boundary. |
| `src/main/java/com/kgm/ui/LoginView.java` | Login window and authentication flow. |
| `src/main/java/com/kgm/ui/HomeView.java` | Main dashboard with header, employee search, table, import, add, and refresh actions. |
| `src/main/java/com/kgm/ui/EmployeeInduction.java` | Employee onboarding window for entering details and uploading documents. |
| `src/main/java/com/kgm/ui/EmployeeDetailView.java` | Employee detail window with tabbed employee and document views. |
| `src/main/java/com/kgm/ui/component/UniversalDatePicker.java` | Reusable single-date picker Swing component. |
| `src/main/java/com/kgm/ui/component/UniversalDateRangePicker.java` | Reusable date-range picker Swing component. |
| `src/main/java/com/kgm/ui/dialog/UniversalDialog.java` | Reusable typed dialog component. |
| `src/main/java/com/kgm/ui/panel/BasicDetailsPanel.java` | Employee basic-details form section. |
| `src/main/java/com/kgm/ui/panel/FormPanel.java` | Full employee induction form with photo upload support. |
| `src/main/java/com/kgm/ui/panel/OtherDetailsPanel.java` | Additional employee detail fields not covered by the basic form. |
| `src/main/java/com/kgm/ui/panel/DocumentPanel.java` | Document upload table used during employee induction. |
| `src/main/java/com/kgm/ui/panel/DocumentViewPanel.java` | Read-only document table used in employee detail view. |
| `src/main/java/com/kgm/ui/panel/EmployeeTablePanel.java` | Paginated employee table with per-row `View` action cells. |
| `src/main/java/com/kgm/ui/panel/ExcelImportButton.java` | Reusable Excel import button component. |
| `src/main/java/com/kgm/ui/panel/HeaderPanel.java` | Shared page header panel. |
| `src/main/java/com/kgm/ui/panel/FooterPanel.java` | Shared page footer panel. |
| `src/main/java/com/kgm/ui/panel/UniversalTablePanel.java` | Reusable paginated table panel with action/link/status rendering support. |
| `src/main/java/com/kgm/ui/styling/BasicDetailsPanelHelper.java` | Styling helper for the basic details form section. |
| `src/main/java/com/kgm/ui/styling/DialogHelper.java` | Convenience methods for warning, info, and error dialogs. |
| `src/main/java/com/kgm/ui/styling/DocumentPanelHelper.java` | Styling and layout helper for document upload panels. |
| `src/main/java/com/kgm/ui/styling/DocumentViewPanelHelper.java` | Styling proxy/helper for read-only document view panels. |
| `src/main/java/com/kgm/ui/styling/EmployeeDetailViewHelper.java` | Layout helper for employee detail view content. |
| `src/main/java/com/kgm/ui/styling/EmployeeDetailViewStyle.java` | Styling constants and tab/window styling for employee detail screens. |
| `src/main/java/com/kgm/ui/styling/EmployeeInductionHelper.java` | Layout helper for induction tabs and action rows. |
| `src/main/java/com/kgm/ui/styling/FormPanelHelper.java` | Styling helper for form fields, photo upload, and grouped form sections. |
| `src/main/java/com/kgm/ui/styling/HomeViewStyle.java` | Styling helper for the home dashboard, bordered filter search bar, action buttons, and body layout. |
| `src/main/java/com/kgm/ui/styling/LoginViewStyle.java` | Styling helper for the login screen and custom login controls. |
| `src/main/java/com/kgm/ui/styling/OtherDetailsPanelHelper.java` | Styling helper for additional employee details. |
| `src/main/java/com/kgm/ui/styling/RoomDetailHelper.java` | Styling helper for room/detail style form layouts. |
| `src/main/java/com/kgm/ui/styling/TableStyleHelper.java` | Shared table colors, renderers, empty state, and table styling. |
| `src/main/java/com/kgm/ui/styling/UniversalDatePickerHelper.java` | Styling helper for the single-date picker. |
| `src/main/java/com/kgm/ui/styling/UniversalDateRangePickerHelper.java` | Styling helper for the date-range picker. |
| `src/main/java/com/kgm/ui/styling/UniversalDialogHelper.java` | Styling helper for universal dialogs. |
| `src/main/java/com/kgm/ui/styling/UniversalTablePagination.java` | Table factory, pagination controls, document table renderers, and sizing utilities. |
| `src/main/java/com/kgm/ui/styling/UniversalTablePanelHelper.java` | Rendering and scroll/pagination helper for `UniversalTablePanel`. |
| `src/main/java/com/kgm/util/FileUtil.java` | Reserved file utility boundary. |
| `src/main/java/com/kgm/util/FilterUtil.java` | Reserved filtering utility boundary. |
| `src/main/java/com/kgm/util/SessionManager.java` | In-memory active user session manager. |
| `src/main/java/com/kgm/util/SessionWatcher.java` | Background session expiry watcher. |
| `src/main/java/com/kgm/util/ValidationUtil.java` | Reserved validation utility boundary. |

## Naming and Project Hygiene

- Java source files use PascalCase class names that match their public class names.
- Packages use lowercase names and singular domain names such as `service`, `model`, and `util`.
- SQL and other non-Java resources live under `src/main/resources`.
- The project uses `DocumentPanel` for upload/edit flows and `DocumentViewPanel` for read-only document viewing.
- Duplicate, unused, or generated source-tree files have been removed from the file catalog.
- Compiled `.class` files and Maven output belong in `target/`, not under `src/main/java`.
- Runtime uploads and employee document storage should be treated as generated data.
