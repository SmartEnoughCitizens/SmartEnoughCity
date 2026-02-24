package com.trinity.hermes.config;

import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

/**
 * Restricts Hibernate DDL operations (ddl-auto: update) to the "backend" schema only,
 * preventing modifications to the "external_data" schema which is managed by the data-handler
 * service and for which the backend_user has no DDL permissions.
 */
public class BackendSchemaFilterProvider implements SchemaFilterProvider {

  @Override
  public SchemaFilter getCreateFilter() {
    return BackendSchemaFilter.INSTANCE;
  }

  @Override
  public SchemaFilter getDropFilter() {
    return BackendSchemaFilter.INSTANCE;
  }

  @Override
  public SchemaFilter getMigrateFilter() {
    return BackendSchemaFilter.INSTANCE;
  }

  @Override
  public SchemaFilter getValidateFilter() {
    return SchemaFilter.ALL;
  }

  @Override
  public SchemaFilter getTruncatorFilter() {
    return BackendSchemaFilter.INSTANCE;
  }
}
