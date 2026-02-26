package com.trinity.hermes.config;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * Only includes objects in the "backend" schema (or the default/empty schema which maps to
 * "backend" via the default_schema config). Excludes all other schemas like "external_data".
 */
public class BackendSchemaFilter implements SchemaFilter {

  static final BackendSchemaFilter INSTANCE = new BackendSchemaFilter();

  private static final String BACKEND_SCHEMA = "backend";

  @Override
  public boolean includeNamespace(Namespace namespace) {
    String schema =
        namespace.getName().getSchema() != null ? namespace.getName().getSchema().getText() : "";
    return schema.isEmpty() || BACKEND_SCHEMA.equalsIgnoreCase(schema);
  }

  @Override
  public boolean includeTable(Table table) {
    return true;
  }

  @Override
  public boolean includeSequence(Sequence sequence) {
    return true;
  }
}
