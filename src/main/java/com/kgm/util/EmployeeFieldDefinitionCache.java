package com.kgm.util;

import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.model.EmployeeFieldDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EmployeeFieldDefinitionCache {
    private static volatile List<EmployeeFieldDefinition> cachedFields;
    private static volatile List<EmployeeFieldDefinition> cachedBasicFields;
    private static volatile List<EmployeeFieldDefinition> cachedDetailFields;
    private static volatile List<EmployeeFieldDefinition> cachedDocumentFields;

    private EmployeeFieldDefinitionCache() {
    }

    public static List<EmployeeFieldDefinition> fields() {
        List<EmployeeFieldDefinition> current = cachedFields;
        if (current != null) {
            return current;
        }
        synchronized (EmployeeFieldDefinitionCache.class) {
            if (cachedFields == null) {
                cachedFields = loadFromDatabase(true);
            }
            return cachedFields;
        }
    }

    public static List<EmployeeFieldDefinition> refreshFromDatabase() {
        return refreshFromDatabase(true);
    }

    public static List<EmployeeFieldDefinition> refreshPreparedMetadata() {
        return refreshFromDatabase(false);
    }

    private static List<EmployeeFieldDefinition> refreshFromDatabase(boolean syncWithDatabase) {
        synchronized (EmployeeFieldDefinitionCache.class) {
            cachedFields = loadFromDatabase(syncWithDatabase);
            clearDerivedCaches();
            EmployeeDocumentUtil.refreshDocumentTypes();
            return cachedFields;
        }
    }

    public static void invalidate() {
        synchronized (EmployeeFieldDefinitionCache.class) {
            cachedFields = null;
            clearDerivedCaches();
            EmployeeDocumentUtil.refreshDocumentTypes();
        }
    }

    public static List<EmployeeFieldDefinition> basicFields() {
        List<EmployeeFieldDefinition> current = cachedBasicFields;
        if (current != null) {
            return current;
        }
        synchronized (EmployeeFieldDefinitionCache.class) {
            if (cachedBasicFields == null) {
                cachedBasicFields = EmployeeBasicFieldUtil.basicDefinitions(fields());
            }
            return cachedBasicFields;
        }
    }

    public static List<EmployeeFieldDefinition> detailFields() {
        List<EmployeeFieldDefinition> current = cachedDetailFields;
        if (current != null) {
            return current;
        }
        synchronized (EmployeeFieldDefinitionCache.class) {
            if (cachedDetailFields == null) {
                cachedDetailFields = loadDetailFields();
            }
            return cachedDetailFields;
        }
    }

    public static List<EmployeeFieldDefinition> documentFields() {
        List<EmployeeFieldDefinition> current = cachedDocumentFields;
        if (current != null) {
            return current;
        }
        synchronized (EmployeeFieldDefinitionCache.class) {
            if (cachedDocumentFields == null) {
                cachedDocumentFields = loadDocumentFields();
            }
            return cachedDocumentFields;
        }
    }

    private static List<EmployeeFieldDefinition> loadDetailFields() {
        List<EmployeeFieldDefinition> definitions = new ArrayList<>();
        for (EmployeeFieldDefinition definition : fields()) {
            if (!definition.documentField()
                    && definition.detailField()
                    && !definition.coreField()
                    && !EmployeeBasicFieldUtil.isFundamentalsHeading(definition.heading())) {
                definitions.add(definition);
            }
        }
        definitions.sort(fieldOrder());
        return List.copyOf(definitions);
    }

    private static List<EmployeeFieldDefinition> loadDocumentFields() {
        List<EmployeeFieldDefinition> definitions = new ArrayList<>();
        for (EmployeeFieldDefinition definition : fields()) {
            if (definition.documentField()) {
                definitions.add(definition);
            }
        }
        definitions.sort(fieldOrder());
        return List.copyOf(definitions);
    }

    private static void clearDerivedCaches() {
        cachedBasicFields = null;
        cachedDetailFields = null;
        cachedDocumentFields = null;
    }

    private static List<EmployeeFieldDefinition> loadFromDatabase(boolean syncWithDatabase) {
        EmployeeFieldDefinitionDao dao = new EmployeeFieldDefinitionDao();
        return List.copyOf(syncWithDatabase ? dao.listFields() : dao.listPreparedFields());
    }

    private static Comparator<EmployeeFieldDefinition> fieldOrder() {
        return Comparator
                .comparing(EmployeeFieldDefinition::heading, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(EmployeeFieldDefinition::sortOrder)
                .thenComparing(EmployeeFieldDefinition::label, String.CASE_INSENSITIVE_ORDER);
    }
}
