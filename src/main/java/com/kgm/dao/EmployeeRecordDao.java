package com.kgm.dao;

import com.kgm.config.DatabaseConnection;
import com.kgm.model.Employee;
import com.kgm.model.EmployeeFieldDefinition;
import com.kgm.util.EmployeeBasicFieldUtil;
import com.kgm.util.EmployeeDocumentUtil;
import com.kgm.util.EmployeeFieldDefinitionCache;
import java.sql.*;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EmployeeRecordDao implements AutoCloseable {
    private static final int QUERY_TIMEOUT_SECONDS = 15;
    private static final int FETCH_SIZE = 500;

    private final Connection con;

    public EmployeeRecordDao() {
        try {
            this.con = DatabaseConnection.getConnection();
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to connect to MySQL database.", exception);
        }
    }

    @Override
    public void close() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    // ==============================
    // 🔹 SAFE VALUE HANDLER
    // ==============================
    private String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        return value;
    }

    private void applyReadQuerySettings(Statement statement) throws SQLException {
        statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
        statement.setFetchSize(FETCH_SIZE);
    }

    // ==============================
    // 🔹 PAGINATED LIST (LIMIT FIXED = 2500)
    // ==============================
    public List<Employee> getEmployees(int offset) {
        return getEmployees(offset, 2500);
    }

    public List<Employee> getEmployees(int offset, int limit) {
        List<Employee> list = new ArrayList<>();
        String sql = """
                    SELECT
                        ID,
                        EMPLOYEE_CODE,
                        EMP_NAME,
                        FATHER_NAME,
                        NID,
                        EMP_CONTNO,
                        PERSONAL_EMAIL,
                        DEPARTMENT,
                        DESIGNATION,
                        SECTION,
                        GRADE,
                        GENDER,
                        RESIGN_REASON,
                        JOINING_DATE,
                        RESIGN_DATE
                    FROM employees
                    ORDER BY ID DESC
                    LIMIT ? OFFSET ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            applyReadQuerySettings(ps);
            ps.setInt(1, Math.max(1, limit));
            ps.setInt(2, Math.max(0, offset));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                Employee e = new Employee();
                e.setID(rs.getInt("ID"));
                e.setEMPLOYEE_CODE(safe(rs.getString("EMPLOYEE_CODE")));
                e.setEMP_NAME(safe(rs.getString("EMP_NAME")));
                e.setFATHER_NAME(safe(rs.getString("FATHER_NAME")));
                e.setNID(safe(rs.getString("NID")));
                e.setEMP_CONTNO(safe(rs.getString("EMP_CONTNO")));
                e.setPERSONAL_EMAIL(safe(rs.getString("PERSONAL_EMAIL")));
                e.setDEPARTMENT(safe(rs.getString("DEPARTMENT")));
                e.setDESIGNATION(safe(rs.getString("DESIGNATION")));
                e.setSECTION(safe(rs.getString("SECTION")));
                e.setGRADE(safe(rs.getString("GRADE")));
                e.setGENDER(safe(rs.getString("GENDER")));
                e.setRESIGN_REASON(safe(rs.getString("RESIGN_REASON")));
                e.setJOINING_DATE(safe(rs.getString("JOINING_DATE")));
                e.setRESIGN_DATE(safe(rs.getString("RESIGN_DATE")));
                list.add(e);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }

    public List<Employee> getEmployeeSummaries() {
        List<Employee> list = new ArrayList<>();
        String sql = """
                    SELECT
                        ID,
                        EMPLOYEE_CODE,
                        EMP_NAME,
                        FATHER_NAME,
                        NID,
                        EMP_CONTNO,
                        PERSONAL_EMAIL,
                        DEPARTMENT,
                        DESIGNATION,
                        SECTION,
                        GRADE,
                        GENDER,
                        RESIGN_REASON,
                        JOINING_DATE,
                        RESIGN_DATE
                    FROM employees
                    ORDER BY ID DESC
                """;

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            applyReadQuerySettings(statement);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(summaryEmployee(rs));
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load employee summaries.", ex);
        }
        return list;
    }

    private Employee summaryEmployee(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setID(rs.getInt("ID"));
        e.setEMPLOYEE_CODE(safe(rs.getString("EMPLOYEE_CODE")));
        e.setEMP_NAME(safe(rs.getString("EMP_NAME")));
        e.setFATHER_NAME(safe(rs.getString("FATHER_NAME")));
        e.setNID(safe(rs.getString("NID")));
        e.setEMP_CONTNO(safe(rs.getString("EMP_CONTNO")));
        e.setPERSONAL_EMAIL(safe(rs.getString("PERSONAL_EMAIL")));
        e.setDEPARTMENT(safe(rs.getString("DEPARTMENT")));
        e.setDESIGNATION(safe(rs.getString("DESIGNATION")));
        e.setSECTION(safe(rs.getString("SECTION")));
        e.setGRADE(safe(rs.getString("GRADE")));
        e.setGENDER(safe(rs.getString("GENDER")));
        e.setRESIGN_REASON(safe(rs.getString("RESIGN_REASON")));
        e.setJOINING_DATE(safe(rs.getString("JOINING_DATE")));
        e.setRESIGN_DATE(safe(rs.getString("RESIGN_DATE")));
        return e;
    }

    // ==============================
    // 🔹 TOTAL COUNT
    // ==============================
    public int countEmployees() {
        String sql = "SELECT COUNT(*) FROM employees";

        try (Statement st = con.createStatement()) {
            applyReadQuerySettings(st);
            try (ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    public DashboardStats dashboardStats() {
        int total = countEmployees();
        Map<String, List<CountStat>> sectionsByDepartment = countByParentChild("DEPARTMENT", "SECTION");
        Map<String, List<CountStat>> departmentsByGrade = countByParentChild("GRADE", "DEPARTMENT");
        Map<String, List<CountStat>> departmentsByDesignation = countByParentChild("DESIGNATION", "DEPARTMENT");
        List<CountStat> departments = countByColumn("DEPARTMENT");
        List<ContributionStat> grades = contributions(countByColumn("GRADE"), departmentsByGrade);
        List<CountStat> designations = countByColumn("DESIGNATION");
        List<CountStat> exitTrends = exitTrends();
        List<EmployeeFieldDefinition> requiredDefinitions = requiredDefinitions();
        List<EmployeeFieldDefinition> requiredDocuments = new ArrayList<>();
        List<EmployeeFieldDefinition> requiredFields = new ArrayList<>();
        for (EmployeeFieldDefinition definition : requiredDefinitions) {
            if (definition.documentField()) {
                requiredDocuments.add(definition);
            } else {
                requiredFields.add(definition);
            }
        }
        DashboardMissingStats missingStats = missingDashboardStats(requiredDocuments, requiredFields, total);

        return new DashboardStats(
                total,
                departments,
                sectionsByDepartment,
                grades,
                designations,
                departmentsByDesignation,
                exitTrends,
                missingDocumentsFromRequirements(missingStats.documents()),
                missingStats.employeesMissingDocuments(),
                missingStats.documents(),
                missingStats.fields(),
                missingStats.employeesMissingFields(),
                totalMissing(missingStats.documents()),
                totalMissing(missingStats.fields()),
                missingStats.employeesMissingAnyData()
        );
    }

    private List<CountStat> countByColumn(String column) {
        List<CountStat> stats = new ArrayList<>();
        String sql = "SELECT " + normalizedValueSql(column) + " AS label, COUNT(*) AS total "
                + "FROM employees GROUP BY label ORDER BY total DESC, label ASC";
        try (Statement statement = con.createStatement()) {
            applyReadQuerySettings(statement);
            try (ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    stats.add(new CountStat(rs.getString("label"), rs.getInt("total")));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return stats;
    }

    private Map<String, List<CountStat>> countByParentChild(String parentColumn, String childColumn) {
        Map<String, List<CountStat>> grouped = new LinkedHashMap<>();
        String sql = "SELECT " + normalizedValueSql(parentColumn) + " AS parent_label, "
                + normalizedValueSql(childColumn) + " AS child_label, COUNT(*) AS total "
                + "FROM employees GROUP BY parent_label, child_label "
                + "ORDER BY parent_label ASC, total DESC, child_label ASC";
        try (Statement statement = con.createStatement()) {
            applyReadQuerySettings(statement);
            try (ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    String parent = rs.getString("parent_label");
                    grouped.computeIfAbsent(parent, ignored -> new ArrayList<>())
                            .add(new CountStat(rs.getString("child_label"), rs.getInt("total")));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return grouped;
    }

    private List<ContributionStat> contributions(
            List<CountStat> totals,
            Map<String, List<CountStat>> contributionMap
    ) {
        List<ContributionStat> contributions = new ArrayList<>();
        for (CountStat total : totals) {
            contributions.add(new ContributionStat(
                    total.label(),
                    total.count(),
                    contributionMap.getOrDefault(total.label(), List.of())
            ));
        }
        contributions.sort(Comparator
                .comparingInt(ContributionStat::count)
                .reversed()
                .thenComparing(ContributionStat::label, String.CASE_INSENSITIVE_ORDER));
        return contributions;
    }

    private List<CountStat> exitTrends() {
        Map<String, Integer> totals = new LinkedHashMap<>();
        totals.put("Layoff", 0);
        totals.put("Resignation", 0);
        totals.put("Others", 0);
        for (CountStat reason : countByColumn("RESIGN_REASON")) {
            String bucket = exitBucket(reason.label());
            totals.put(bucket, totals.getOrDefault(bucket, 0) + reason.count());
        }

        List<CountStat> stats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            stats.add(new CountStat(entry.getKey(), entry.getValue()));
        }
        return stats;
    }

    private String exitBucket(String reason) {
        String text = reason == null ? "" : reason.toLowerCase();
        if (text.contains("lay")) {
            return "Layoff";
        }
        if (text.contains("resign")
                || text.contains("retire")
                || text.contains("left")
                || text.contains("quit")) {
            return "Resignation";
        }
        return "Others";
    }

    private DashboardMissingStats missingDashboardStats(
            List<EmployeeFieldDefinition> documentDefinitions,
            List<EmployeeFieldDefinition> fieldDefinitions,
            int totalEmployees
    ) {
        List<EmployeeFieldDefinition> safeDocumentDefinitions = documentDefinitions == null ? List.of() : documentDefinitions;
        List<EmployeeFieldDefinition> safeFieldDefinitions = fieldDefinitions == null ? List.of() : fieldDefinitions;
        List<MissingMetric> metrics = new ArrayList<>();
        List<MissingRequirementStat> missingDocuments = new ArrayList<>();
        List<MissingRequirementStat> missingFields = new ArrayList<>();
        Set<String> existingColumns;
        try {
            existingColumns = employeeColumns();
        } catch (SQLException exception) {
            exception.printStackTrace();
            existingColumns = Set.of();
        }

        int aliasIndex = 0;
        boolean missingDocumentColumn = false;
        for (EmployeeFieldDefinition definition : safeDocumentDefinitions) {
            if (existingColumns.contains(definition.columnName().toUpperCase(Locale.ROOT))) {
                String alias = "m" + aliasIndex++;
                metrics.add(new MissingMetric(definition, alias));
            } else {
                missingDocumentColumn = true;
                missingDocuments.add(missingRequirement(definition, totalEmployees));
            }
        }

        boolean missingFieldColumn = false;
        for (EmployeeFieldDefinition definition : safeFieldDefinitions) {
            if (existingColumns.contains(definition.columnName().toUpperCase(Locale.ROOT))) {
                String alias = "m" + aliasIndex++;
                metrics.add(new MissingMetric(definition, alias));
            } else {
                missingFieldColumn = true;
                missingFields.add(missingRequirement(definition, totalEmployees));
            }
        }

        List<String> selectExpressions = new ArrayList<>();
        List<String> documentConditions = new ArrayList<>();
        List<String> fieldConditions = new ArrayList<>();
        for (MissingMetric metric : metrics) {
            String condition = missingValueCondition(quoteIdentifier(metric.definition().columnName()));
            selectExpressions.add("SUM(CASE WHEN " + condition + " THEN 1 ELSE 0 END) AS " + metric.alias());
            if (metric.definition().documentField()) {
                documentConditions.add(condition);
            } else {
                fieldConditions.add(condition);
            }
        }

        boolean hasDocumentConditions = !documentConditions.isEmpty();
        boolean hasFieldConditions = !fieldConditions.isEmpty();
        if (hasDocumentConditions) {
            selectExpressions.add("SUM(CASE WHEN (" + String.join(" OR ", documentConditions)
                    + ") THEN 1 ELSE 0 END) AS any_documents");
        }
        if (hasFieldConditions) {
            selectExpressions.add("SUM(CASE WHEN (" + String.join(" OR ", fieldConditions)
                    + ") THEN 1 ELSE 0 END) AS any_fields");
        }
        if (hasDocumentConditions || hasFieldConditions) {
            List<String> allConditions = new ArrayList<>(documentConditions);
            allConditions.addAll(fieldConditions);
            selectExpressions.add("SUM(CASE WHEN (" + String.join(" OR ", allConditions)
                    + ") THEN 1 ELSE 0 END) AS any_data");
        }

        int employeesMissingDocuments = missingDocumentColumn && !safeDocumentDefinitions.isEmpty()
                ? totalEmployees
                : 0;
        int employeesMissingFields = missingFieldColumn && !safeFieldDefinitions.isEmpty()
                ? totalEmployees
                : 0;
        int employeesMissingAnyData = (missingDocumentColumn || missingFieldColumn)
                && (!safeDocumentDefinitions.isEmpty() || !safeFieldDefinitions.isEmpty())
                ? totalEmployees
                : 0;

        if (!selectExpressions.isEmpty()) {
            String sql = "SELECT " + String.join(", ", selectExpressions) + " FROM employees";
            try (Statement statement = con.createStatement()) {
                applyReadQuerySettings(statement);
                try (ResultSet rs = statement.executeQuery(sql)) {
                    if (rs.next()) {
                        for (MissingMetric metric : metrics) {
                            MissingRequirementStat stat = missingRequirement(metric.definition(), rs.getInt(metric.alias()));
                            if (metric.definition().documentField()) {
                                missingDocuments.add(stat);
                            } else {
                                missingFields.add(stat);
                            }
                        }
                        if (!missingDocumentColumn && hasDocumentConditions) {
                            employeesMissingDocuments = rs.getInt("any_documents");
                        }
                        if (!missingFieldColumn && hasFieldConditions) {
                            employeesMissingFields = rs.getInt("any_fields");
                        }
                        if (!missingDocumentColumn && !missingFieldColumn
                                && (hasDocumentConditions || hasFieldConditions)) {
                            employeesMissingAnyData = rs.getInt("any_data");
                        }
                    }
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }

        sortMissingStats(missingDocuments);
        sortMissingStats(missingFields);
        return new DashboardMissingStats(
                missingDocuments,
                missingFields,
                employeesMissingDocuments,
                employeesMissingFields,
                employeesMissingAnyData
        );
    }

    private MissingRequirementStat missingRequirement(EmployeeFieldDefinition definition, int missingCount) {
        return new MissingRequirementStat(
                definition.label(),
                definition.columnName(),
                missingCount,
                definition.documentField()
        );
    }

    private void sortMissingStats(List<MissingRequirementStat> stats) {
        stats.sort(Comparator
                .comparingInt(MissingRequirementStat::missingCount)
                .reversed()
                .thenComparing(MissingRequirementStat::label, String.CASE_INSENSITIVE_ORDER));
    }

    private List<EmployeeFieldDefinition> requiredDefinitions() {
        List<EmployeeFieldDefinition> required = new ArrayList<>();
        try {
            for (EmployeeFieldDefinition definition : EmployeeFieldDefinitionCache.fields()) {
                if ("ID".equalsIgnoreCase(definition.columnName())) {
                    continue;
                }
                if (definition.requiredField()) {
                    required.add(definition);
                }
            }
        } catch (RuntimeException exception) {
            exception.printStackTrace();
        }
        required.sort(Comparator
                .comparing(EmployeeFieldDefinition::heading, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(EmployeeFieldDefinition::sortOrder)
                .thenComparing(EmployeeFieldDefinition::label, String.CASE_INSENSITIVE_ORDER));
        return required;
    }

    private List<EmployeeFieldDefinition> requiredDefinitions(boolean documentField) {
        List<EmployeeFieldDefinition> required = new ArrayList<>();
        try {
            for (EmployeeFieldDefinition definition : EmployeeFieldDefinitionCache.fields()) {
                if ("ID".equalsIgnoreCase(definition.columnName())) {
                    continue;
                }
                if (definition.documentField() == documentField && definition.requiredField()) {
                    required.add(definition);
                }
            }
        } catch (RuntimeException exception) {
            exception.printStackTrace();
        }
        required.sort(Comparator
                .comparing(EmployeeFieldDefinition::heading, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(EmployeeFieldDefinition::sortOrder)
                .thenComparing(EmployeeFieldDefinition::label, String.CASE_INSENSITIVE_ORDER));
        return required;
    }

    private List<MissingDocumentStat> missingDocumentsFromRequirements(List<MissingRequirementStat> requirements) {
        List<MissingDocumentStat> documents = new ArrayList<>();
        for (MissingRequirementStat requirement : requirements) {
            documents.add(new MissingDocumentStat(
                    requirement.label(),
                    requirement.column(),
                    requirement.missingCount()
            ));
        }
        return documents;
    }

    private int totalMissing(List<MissingRequirementStat> stats) {
        int total = 0;
        for (MissingRequirementStat stat : stats) {
            total += stat.missingCount();
        }
        return total;
    }

    private String normalizedValueSql(String column) {
        String quoted = quoteIdentifier(column);
        return "CASE WHEN " + missingValueCondition(quoted)
                + " THEN 'Unassigned' ELSE TRIM(" + quoted + ") END";
    }

    private String missingValueCondition(String quotedColumn) {
        return quotedColumn + " IS NULL OR TRIM(" + quotedColumn + ") = '' OR UPPER(TRIM(" + quotedColumn
                + ")) IN ('N/A', 'NA', 'NULL') OR TRIM(" + quotedColumn + ") = '-'";
    }

    public List<MissingEmployeeRow> missingRequiredDataRows() {
        List<MissingEmployeeRow> rows = new ArrayList<>();
        List<EmployeeFieldDefinition> required = new ArrayList<>();
        required.addAll(requiredDefinitions(false));
        required.addAll(requiredDefinitions(true));
        if (required.isEmpty()) {
            return rows;
        }

        try (Statement statement = con.createStatement()) {
            applyReadQuerySettings(statement);
            try (ResultSet rs = statement.executeQuery("SELECT * FROM employees ORDER BY ID DESC")) {
                ResultSetMetaData metaData = rs.getMetaData();
                Map<String, Integer> resultColumns = resultColumns(metaData);
                while (rs.next()) {
                    List<String> missing = new ArrayList<>();
                    for (EmployeeFieldDefinition definition : required) {
                        Integer index = resultColumns.get(definition.columnName().toUpperCase(Locale.ROOT));
                        String value = index == null ? null : rs.getString(index);
                        if (isMissingValue(value)) {
                            missing.add(definition.label());
                        }
                    }
                    if (missing.isEmpty()) {
                        continue;
                    }
                    rows.add(new MissingEmployeeRow(
                            safe(valueFor(rs, resultColumns, "EMPLOYEE_CODE")),
                            safe(valueFor(rs, resultColumns, "EMP_NAME")),
                            String.join(", ", missing),
                            safe(valueFor(rs, resultColumns, "DESIGNATION")),
                            safe(valueFor(rs, resultColumns, "GRADE")),
                            safe(valueFor(rs, resultColumns, "DEPARTMENT")),
                            safe(valueFor(rs, resultColumns, "SECTION")),
                            safe(valueFor(rs, resultColumns, "JOINING_DATE")),
                            safe(valueFor(rs, resultColumns, "RESIGN_DATE")),
                            safe(valueFor(rs, resultColumns, "EMP_CONTNO"))
                    ));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return rows;
    }

    private boolean isMissingValue(String value) {
        if (value == null) {
            return true;
        }
        String text = value.trim();
        return text.isEmpty()
                || text.equalsIgnoreCase("N/A")
                || text.equalsIgnoreCase("NA")
                || text.equalsIgnoreCase("NULL")
                || text.equals("-");
    }

    private Map<String, Integer> resultColumns(ResultSetMetaData metaData) throws SQLException {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            String label = metaData.getColumnLabel(index);
            if (label == null || label.isBlank()) {
                label = metaData.getColumnName(index);
            }
            if (label != null && !label.isBlank()) {
                columns.put(label.toUpperCase(Locale.ROOT), index);
            }
        }
        return columns;
    }

    private String valueFor(ResultSet rs, Map<String, Integer> resultColumns, String column) throws SQLException {
        Integer index = resultColumns.get(column.toUpperCase(Locale.ROOT));
        return index == null ? "" : rs.getString(index);
    }

    // ==============================
    // 🔹 SEARCH BY EMPLOYEE CODE (INDEXED)
    // ==============================
    public Employee getEmployeeByCode(String empCode) {
        String sql = """
                    SELECT
                        ID,
                        EMPLOYEE_CODE,
                        EMP_NAME,
                        FATHER_NAME,
                        NID,
                        EMP_CONTNO,
                        PERSONAL_EMAIL,
                        DEPARTMENT,
                        DESIGNATION,
                        SECTION,
                        GRADE,
                        GENDER,
                        RESIGN_REASON,
                        JOINING_DATE,
                        RESIGN_DATE
                    FROM employees
                    WHERE EMPLOYEE_CODE = ?
                    LIMIT 1
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            applyReadQuerySettings(ps);
            ps.setString(1, empCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                Employee e = new Employee();
                e.setID(rs.getInt("ID"));
                e.setEMPLOYEE_CODE(safe(rs.getString("EMPLOYEE_CODE")));
                e.setEMP_NAME(safe(rs.getString("EMP_NAME")));
                e.setFATHER_NAME(safe(rs.getString("FATHER_NAME")));
                e.setNID(safe(rs.getString("NID")));
                e.setEMP_CONTNO(safe(rs.getString("EMP_CONTNO")));
                e.setPERSONAL_EMAIL(safe(rs.getString("PERSONAL_EMAIL")));
                e.setDEPARTMENT(rs.getString("DEPARTMENT"));
                e.setDESIGNATION(safe(rs.getString("DESIGNATION")));
                e.setSECTION(safe(rs.getString("SECTION")));
                e.setGRADE(safe(rs.getString("GRADE")));
                e.setGENDER(safe(rs.getString("GENDER")));
                e.setRESIGN_REASON(safe(rs.getString("RESIGN_REASON")));
                e.setJOINING_DATE(safe(rs.getString("JOINING_DATE")));
                e.setRESIGN_DATE(safe(rs.getString("RESIGN_DATE")));
                return e;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    // ==============================
    // 🔹 PROJECTED EMPLOYEE LOADS (TAB-SPECIFIC FIELDS)
    // ==============================
    public Employee getEmployeeHeaderByCode(String empCode) {
        return getEmployeeSectionByCode(empCode, List.of("ID", "EMPLOYEE_CODE", "EMP_NAME"));
    }

    public Employee getEmployeeSectionByCode(String empCode, List<String> requestedColumns) {
        if (empCode == null || empCode.isBlank()) {
            return null;
        }

        try {
            List<String> columns = normalizedProjectionColumns(requestedColumns);
            if (columns.isEmpty()) {
                columns = List.of("EMPLOYEE_CODE");
            }

            String sql = "SELECT " + quotedColumnList(columns)
                    + " FROM employees WHERE EMPLOYEE_CODE = ? LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                applyReadQuerySettings(ps);
                ps.setString(1, empCode.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapProjectedEmployee(rs);
                    }
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public Employee getEmployeeDocumentsByCode(String empCode) {
        List<String> columns = new ArrayList<>();
        columns.add("ID");
        columns.add("EMPLOYEE_CODE");
        columns.add("EMP_NAME");
        columns.add("EMP_IMG");
        for (EmployeeDocumentUtil.DocumentType documentType : EmployeeDocumentUtil.documentTypes()) {
            columns.add(documentType.employeeFieldName());
        }
        return getEmployeeSectionByCode(empCode, columns);
    }

    // ==============================
    // 🔹 FULL EMPLOYEE BY CODE (ALL FIELDS)
    // ==============================
    public Employee getFullEmployeeByCode(String empCode) {
        String sql = "SELECT * FROM employees WHERE EMPLOYEE_CODE = ? LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            applyReadQuerySettings(ps);
            ps.setString(1, empCode);
            try (ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                Employee e = new Employee();

                // ================= BASIC =================
                e.setID(rs.getInt("ID"));
                e.setUNT_CODE(safe(rs.getString("UNT_CODE")));
                e.setEMPLOYEE_CODE(safe(rs.getString("EMPLOYEE_CODE")));
                e.setEMP_NAME(safe(rs.getString("EMP_NAME")));
                e.setFATHER_NAME(safe(rs.getString("FATHER_NAME")));
                e.setMOTHER_NAME(safe(rs.getString("MOTHER_NAME")));
                e.setGENDER(safe(rs.getString("GENDER")));
                e.setDOB(safe(rs.getString("DOB")));
                e.setCITY_OF_BIRTH(safe(rs.getString("CITY_OF_BIRTH")));
                e.setNATIONALITY(safe(rs.getString("NATIONALITY")));
                e.setRELIGION(safe(rs.getString("RELIGION")));
                e.setBLOOD_GROUP(safe(rs.getString("BLOOD_GROUP")));
                e.setM_STATUS(safe(rs.getString("M_STATUS")));
                e.setNID(safe(rs.getString("NID")));

                // ================= EMPLOYMENT =================
                e.setDEPARTMENT(safe(rs.getString("DEPARTMENT")));
                e.setDESIG_CODE(safe(rs.getString("DESIG_CODE")));
                e.setDESIGNATION(safe(rs.getString("DESIGNATION")));
                e.setSECTION(safe(rs.getString("SECTION")));
                e.setGRADE(safe(rs.getString("GRADE")));
                e.setJOINING_DATE(safe(rs.getString("JOINING_DATE")));
                e.setCONFIRMING_ON(safe(rs.getString("CONFIRMING_ON")));
                e.setEMP_STATUS(safe(rs.getString("EMP_STATUS")));
                e.setSHIFT(safe(rs.getString("SHIFT")));
                e.setPROB_PERIOD(safe(rs.getString("PROB_PERIOD")));
                e.setEXP_IN_KTML(safe(rs.getString("EXP_IN_KTML")));
                e.setAPPLICATION_DATE(safe(rs.getString("APPLICATION_DATE")));
                e.setRESIGN_REASON(safe(rs.getString("RESIGN_REASON")));
                e.setRESIGN_DATE(safe(rs.getString("RESIGN_DATE")));

                // ================= ORGANIZATION =================
                e.setORG_ID(safe(rs.getString("ORG_ID")));
                e.setDIVISION(safe(rs.getString("DIVISION")));
                e.setBRANCH_CODE(safe(rs.getString("BRANCH_CODE")));
                e.setBRANCH_NAME(safe(rs.getString("BRANCH_NAME")));
                e.setDESCR(safe(rs.getString("DESCR")));

                // ================= PAYROLL =================
                e.setGROSS_SALARY(safe(rs.getString("GROSS_SALARY")));
                e.setPAY_CATEGORY(safe(rs.getString("PAY_CATEGORY")));
                e.setBASIC(safe(rs.getString("BASIC")));
                e.setCOLA1(safe(rs.getString("COLA1")));
                e.setCOLA2(safe(rs.getString("COLA2")));
                e.setCOLA3(safe(rs.getString("COLA3")));
                e.setCOLA4(safe(rs.getString("COLA4")));
                e.setCOLA5(safe(rs.getString("COLA5")));
                e.setCOLA6_7(safe(rs.getString("COLA6_7")));
                e.setCOLA8(safe(rs.getString("COLA8")));
                e.setCOLA9(safe(rs.getString("COLA9")));
                e.setCOLA10(safe(rs.getString("COLA10")));
                e.setCOLA11(safe(rs.getString("COLA11")));

                e.setPB_SPECIAL1_2(safe(rs.getString("PB_SPECIAL1_2")));
                e.setPB_SPECIAL3(safe(rs.getString("PB_SPECIAL3")));
                e.setPB_SPECIAL4(safe(rs.getString("PB_SPECIAL4")));
                e.setSPECIAL(safe(rs.getString("SPECIAL")));

                e.setOTHER1(safe(rs.getString("OTHER1")));
                e.setOTHER2(safe(rs.getString("OTHER2")));
                e.setOTHER3(safe(rs.getString("OTHER3")));

                e.setMEDICAL(safe(rs.getString("MEDICAL")));
                e.setCONVEYANCE(safe(rs.getString("CONVEYANCE")));
                e.setUTILITY(safe(rs.getString("UTILITY")));
                e.setENTERTAINMENT(safe(rs.getString("ENTERTAINMENT")));

                e.setPAY_GROUP(safe(rs.getString("PAY_GROUP")));
                e.setPAY_GROUP_DESC(safe(rs.getString("PAY_GROUP_DESC")));
                e.setPAY_AT_JOINING(safe(rs.getString("PAY_AT_JOINING")));
                e.setEXTRA_DUTY(safe(rs.getString("EXTRA_DUTY")));
                e.setPAYROLL_FLAG(safe(rs.getString("PAYROLL_FLAG")));

                // ================= BANKING =================
                e.setBANK_NAME(safe(rs.getString("BANK_NAME")));
                e.setBANK_AC_NO(safe(rs.getString("BANK_AC_NO")));
                e.setSS_NO(safe(rs.getString("SS_NO")));
                e.setEOBI_NO(safe(rs.getString("EOBI_NO")));
                e.setTAX_NO(safe(rs.getString("TAX_NO")));
                e.setPFUND_DEDUCTION(safe(rs.getString("PFUND_DEDUCTION")));
                e.setPF_INTEREST(safe(rs.getString("PF_INTEREST")));
                e.setPFUND_CODE(safe(rs.getString("PFUND_CODE")));
                e.setCLIPPER_PFUND_CODE(safe(rs.getString("CLIPPER_PFUND_CODE")));
                e.setEFU(safe(rs.getString("EFU")));
                e.setEFU_NO(safe(rs.getString("EFU_NO")));
                e.setEOBI_STATUS(safe(rs.getString("EOBI_STATUS")));

                // ================= CONTACT =================
                e.setEMP_CONTNO(safe(rs.getString("EMP_CONTNO")));
                e.setCURRENT_ADR(safe(rs.getString("CURRENT_ADR")));
                e.setPERMANENT_ADR(safe(rs.getString("PERMANENT_ADR")));
                e.setPERSONAL_EMAIL(safe(rs.getString("PERSONAL_EMAIL")));
                e.setOFFICIAL_EMAIL(safe(rs.getString("OFFICIAL_EMAIL")));
                e.setEMERGENCY_NO(safe(rs.getString("EMERGENCY_NO")));

                // ================= REPORTING =================
                e.setREP_UNT(safe(rs.getString("REP_UNT")));
                e.setREP_EMP_ID(safe(rs.getString("REP_EMP_ID")));
                e.setREP_EMP_DESIG_CODE(safe(rs.getString("REP_EMP_DESIG_CODE")));
                e.setREP_EMP_DEPT_CODE(safe(rs.getString("REP_EMP_DEPT_CODE")));
                e.setREP_EMP_TYPE(safe(rs.getString("REP_EMP_TYPE")));

                // ================= COMPLIANCE =================
                e.setFLAG(safe(rs.getString("FLAG")));
                e.setCLEARANCE_STATUS(safe(rs.getString("CLEARANCE_STATUS")));
                e.setHOD_CHECK(safe(rs.getString("HOD_CHECK")));
                e.setSEC_HEAD_CHK(safe(rs.getString("SEC_HEAD_CHK")));
                e.setNIC_VERIFY(safe(rs.getString("NIC_VERIFY")));
                e.setNIC_VERIFY_DATE(safe(rs.getString("NIC_VERIFY_DATE")));
                e.setATT_CATEG(safe(rs.getString("ATT_CATEG")));
                e.setDIS_CERTIFICATE(safe(rs.getString("DIS_CERTIFICATE")));

                // ================= BENEFITS =================
                e.setWELLNESS_CLUB(safe(rs.getString("WELLNESS_CLUB")));
                e.setWELLNESS_CARD_ISSUE(safe(rs.getString("WELLNESS_CARD_ISSUE")));
                e.setWELLNESS_CARD_NO(safe(rs.getString("WELLNESS_CARD_NO")));
                e.setWELLNESS_CLUB_VALID_DATE(safe(rs.getString("WELLNESS_CLUB_VALID_DATE")));

                // ================= VACCINATION =================
                e.setFIRST_DOSE(safe(rs.getString("FIRST_DOSE")));
                e.setSECOND_DOSE(safe(rs.getString("SECOND_DOSE")));
                e.setFIRST_VACC_DATE(safe(rs.getString("FIRST_VACC_DATE")));
                e.setSECOND_VACC_DATE(safe(rs.getString("SECOND_VACC_DATE")));

                // ================= DOCUMENTS =================
                for (int index = 0; index < EmployeeDocumentUtil.documentCount(); index++) {
                    String column = EmployeeDocumentUtil.documentType(index).employeeFieldName();
                    EmployeeDocumentUtil.setDocumentPath(e, index, safe(rs.getString(column)));
                }
                e.setEMP_IMG(safe(rs.getString("EMP_IMG")));
                loadDynamicFields(e, rs);

                return e;
            }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }



    // ==============================
// 🔹 GENERIC OBJECT-BASED UPDATE
// ==============================
public void updateEmployeeDynamic(Employee emp) throws Exception {

    StringBuilder sql = new StringBuilder("UPDATE employees SET ");
    List<Object> values = new ArrayList<>();
    Set<String> writtenColumns = new HashSet<>();

    // 🔥 reflect all fields of Employee class
    java.lang.reflect.Field[] fields = Employee.class.getDeclaredFields();

    for (java.lang.reflect.Field field : fields) {

        field.setAccessible(true);
        String column = field.getName();

        // Skip ID, primary key, and the dynamic field map itself.
        if (column.equalsIgnoreCase("ID")
                || column.equalsIgnoreCase("EMPLOYEE_CODE")
                || column.equalsIgnoreCase("dynamicFields")) {
            continue;
        }

        Object value = field.get(emp);
        if (isWritableValue(value)) {
            addUpdateValue(sql, values, writtenColumns, column, value.toString().trim());
        }
    }

    for (Map.Entry<String, String> dynamicField : emp.getDynamicFields().entrySet()) {
        String column = dynamicField.getKey();
        if (column.equalsIgnoreCase("ID") || column.equalsIgnoreCase("EMPLOYEE_CODE")) {
            continue;
        }

        String value = dynamicField.getValue();
        if (isWritableValue(value)) {
            addUpdateValue(sql, values, writtenColumns, column, value.trim());
        }
    }

    // remove last comma
    if (values.isEmpty()) {
        return; // nothing to update
    }

    sql.setLength(sql.length() - 2);

    sql.append(" WHERE EMPLOYEE_CODE = ?");
    values.add(emp.getEMPLOYEE_CODE());

    try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
        ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

        for (int i = 0; i < values.size(); i++) {
            ps.setObject(i + 1, values.get(i));
        }

        ps.executeUpdate();
    }
}

    private List<String> normalizedProjectionColumns(List<String> requestedColumns) throws SQLException {
        Set<String> existingColumns = employeeColumns();
        Set<String> normalizedColumns = new LinkedHashSet<>();
        normalizedColumns.add("EMPLOYEE_CODE");
        normalizedColumns.add("EMP_NAME");

        if (requestedColumns != null) {
            for (String requestedColumn : requestedColumns) {
                String normalizedColumn = normalizedColumnName(requestedColumn);
                if (!normalizedColumn.isBlank() && existingColumns.contains(normalizedColumn)) {
                    normalizedColumns.add(normalizedColumn);
                }
            }
        }

        normalizedColumns.removeIf(column -> !existingColumns.contains(column));
        return new ArrayList<>(normalizedColumns);
    }

    private Set<String> employeeColumns() throws SQLException {
        Set<String> columns = new HashSet<>();
        DatabaseMetaData metaData = con.getMetaData();
        try (ResultSet rs = metaData.getColumns(con.getCatalog(), null, "employees", null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toUpperCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private String normalizedColumnName(String columnName) {
        if (columnName == null) {
            return "";
        }
        String normalized = columnName.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z0-9_]+") ? normalized : "";
    }

    private String quotedColumnList(List<String> columns) {
        List<String> quoted = new ArrayList<>();
        for (String column : columns) {
            quoted.add(quoteIdentifier(column));
        }
        return String.join(", ", quoted);
    }

    private Employee mapProjectedEmployee(ResultSet rs) throws SQLException {
        Employee employee = new Employee();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            String column = metaData.getColumnLabel(index);
            if (column == null || column.isBlank()) {
                column = metaData.getColumnName(index);
            }
            if (column == null || column.isBlank()) {
                continue;
            }
            if ("ID".equalsIgnoreCase(column)) {
                employee.setID(rs.getInt(index));
                continue;
            }
            EmployeeBasicFieldUtil.writeValue(employee, column, safe(rs.getString(index)));
        }
        return employee;
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private void addUpdateValue(
            StringBuilder sql,
            List<Object> values,
            Set<String> writtenColumns,
            String column,
            String value
    ) {
        String normalizedColumn = column == null ? "" : column.trim().toUpperCase();
        if (normalizedColumn.isEmpty() || writtenColumns.contains(normalizedColumn)) {
            return;
        }
        writtenColumns.add(normalizedColumn);
        sql.append(quoteIdentifier(normalizedColumn)).append(" = ?, ");
        values.add(value);
    }

    private void loadDynamicFields(Employee employee, ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int index = 1; index <= metaData.getColumnCount(); index++) {
            String column = metaData.getColumnLabel(index);
            if (column == null || column.isBlank()) {
                column = metaData.getColumnName(index);
            }
            if (column == null || column.isBlank()) {
                continue;
            }
            employee.setDynamicField(column, safe(rs.getString(index)));
        }
    }

    private boolean isWritableValue(Object value) {
        if (value == null) {
            return false;
        }

        String text = value.toString().trim();
        return !text.isEmpty()
                && !text.equalsIgnoreCase("N/A")
                && !text.equalsIgnoreCase("NA")
                && !text.equalsIgnoreCase("NULL")
                && !text.equals("-");
    }

    public record CountStat(String label, int count) {
    }

    public record ContributionStat(String label, int count, List<CountStat> contributions) {
    }

    public record MissingDocumentStat(String label, String column, int missingCount) {
    }

    public record MissingRequirementStat(String label, String column, int missingCount, boolean documentField) {
    }

    private record MissingMetric(EmployeeFieldDefinition definition, String alias) {
    }

    private record DashboardMissingStats(
            List<MissingRequirementStat> documents,
            List<MissingRequirementStat> fields,
            int employeesMissingDocuments,
            int employeesMissingFields,
            int employeesMissingAnyData
    ) {
    }

    public record MissingEmployeeRow(
            String employeeCode,
            String name,
            String missingItems,
            String designation,
            String grade,
            String department,
            String section,
            String joiningDate,
            String resignationDate,
            String phoneNumber
    ) {
    }

    public record DashboardStats(
            int totalEmployees,
            List<CountStat> employeesByDepartment,
            Map<String, List<CountStat>> sectionsByDepartment,
            List<ContributionStat> employeesByGrade,
            List<CountStat> employeesByDesignation,
            Map<String, List<CountStat>> departmentsByDesignation,
            List<CountStat> exitTrends,
            List<MissingDocumentStat> missingDocuments,
            int employeesMissingRequiredDocuments,
            List<MissingRequirementStat> missingRequiredDocuments,
            List<MissingRequirementStat> missingRequiredFields,
            int employeesMissingRequiredFields,
            int totalMissingRequiredDocuments,
            int totalMissingRequiredFields,
            int employeesMissingAnyRequiredData
    ) {
    }
}
