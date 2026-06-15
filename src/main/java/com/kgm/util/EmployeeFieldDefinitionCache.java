package com.kgm.util;

import com.kgm.dao.EmployeeFieldDefinitionDao;
import com.kgm.model.EmployeeFieldDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EmployeeFieldDefinitionCache {
    private static volatile List<EmployeeFieldDefinition> cachedFields;

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
            EmployeeDocumentUtil.refreshDocumentTypes();
            return cachedFields;
        }
    }

    public static void invalidate() {
        synchronized (EmployeeFieldDefinitionCache.class) {
            cachedFields = null;
            EmployeeDocumentUtil.refreshDocumentTypes();
        }
    }

    public static List<EmployeeFieldDefinition> detailFields() {
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

    public static List<EmployeeFieldDefinition> documentFields() {
        List<EmployeeFieldDefinition> definitions = new ArrayList<>();
        for (EmployeeFieldDefinition definition : fields()) {
            if (definition.documentField()) {
                definitions.add(definition);
            }
        }
        definitions.sort(fieldOrder());
        return List.copyOf(definitions);
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
