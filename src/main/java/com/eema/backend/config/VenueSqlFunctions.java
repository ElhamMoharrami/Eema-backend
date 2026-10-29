package com.eema.backend.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/** Retain native PostgreSQL enum-array containment without Hibernate's generic enum literal inference. */
public class VenueSqlFunctions implements FunctionContributor {
    @Override
    public void contributeFunctions(FunctionContributions contributions) {
        contributions.getFunctionRegistry().registerPattern(
                "eema_activity_contains",
                "(?1 @> cast(array[?2] as production_activity[]))",
                contributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN));
    }
}
