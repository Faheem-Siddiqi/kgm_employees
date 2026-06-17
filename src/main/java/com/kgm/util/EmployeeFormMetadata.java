package com.kgm.util;

import com.kgm.model.EmployeeFieldDefinition;

import java.util.List;

public record EmployeeFormMetadata(
        List<EmployeeFieldDefinition> basicDefinitions,
        List<EmployeeFieldDefinition> detailDefinitions,
        List<EmployeeDocumentUtil.DocumentType> documentTypes,
        boolean[] requiredDocumentFlags,
        boolean profileImageRequired
) {
    public static EmployeeFormMetadata snapshot() {
        return new EmployeeFormMetadata(
                EmployeeBasicFieldUtil.loadBasicDefinitions(),
                EmployeeFieldDefinitionCache.detailFields(),
                EmployeeDocumentUtil.documentTypes(),
                EmployeeDocumentUtil.requiredDocumentFlags(),
                EmployeeDocumentUtil.isProfileImageRequired()
        );
    }

    public static EmployeeFormMetadata fallback() {
        return new EmployeeFormMetadata(
                EmployeeBasicFieldUtil.fallbackDefinitions(),
                List.of(),
                EmployeeDocumentUtil.builtInDocumentTypes(),
                new boolean[EmployeeDocumentUtil.builtInDocumentTypes().size()],
                false
        );
    }

    public EmployeeFormMetadata {
        basicDefinitions = List.copyOf(basicDefinitions == null ? List.of() : basicDefinitions);
        detailDefinitions = List.copyOf(detailDefinitions == null ? List.of() : detailDefinitions);
        documentTypes = List.copyOf(documentTypes == null ? List.of() : documentTypes);
        requiredDocumentFlags = requiredDocumentFlags == null ? new boolean[0] : requiredDocumentFlags.clone();
    }

    @Override
    public boolean[] requiredDocumentFlags() {
        return requiredDocumentFlags.clone();
    }
}
