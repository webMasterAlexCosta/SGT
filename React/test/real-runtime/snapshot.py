#!/usr/bin/env python3
"""Read-only, consistent MySQL snapshots containing metadata and aggregate hashes only."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import time
import uuid
import manage


def digest(value):
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def ident(value):
    return "`" + value.replace("`", "``") + "`"


def capture(label):
    if not re.fullmatch(r"[a-zA-Z0-9_-]{1,80}", label):
        raise ValueError("Snapshot label must contain only letters, digits, underscores, and hyphens")
    manage.require_configuration()
    directory = manage.EVIDENCE / "snapshots"
    directory.mkdir(parents=True, exist_ok=True)
    target = directory / (label + ".json")
    if target.exists():
        raise ValueError("Refusing to overwrite a preserved snapshot")
    schema = manage.SCHEMA
    columns = manage.mysql(f"SELECT TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION FROM information_schema.columns WHERE TABLE_SCHEMA='{schema}' ORDER BY TABLE_NAME,ORDINAL_POSITION;")
    tables = {}
    for line in columns.splitlines():
        table, column, _ordinal = line.split("\t")
        tables.setdefault(table, []).append(column)
    if not tables:
        raise RuntimeError("The isolated schema has no application tables")
    metadata_queries = {
        "tables": f"SELECT TABLE_NAME,ENGINE,ROW_FORMAT,TABLE_COLLATION,CREATE_OPTIONS,TABLE_COMMENT FROM information_schema.tables WHERE TABLE_SCHEMA='{schema}' ORDER BY TABLE_NAME",
        "columns": f"SELECT TABLE_NAME,ORDINAL_POSITION,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COALESCE(COLUMN_DEFAULT,'<NULL>'),EXTRA,COALESCE(CHARACTER_SET_NAME,''),COALESCE(COLLATION_NAME,''),GENERATION_EXPRESSION FROM information_schema.columns WHERE TABLE_SCHEMA='{schema}' ORDER BY TABLE_NAME,ORDINAL_POSITION",
        "indexes": f"SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COALESCE(COLUMN_NAME,''),COALESCE(COLLATION,''),COALESCE(SUB_PART,0),INDEX_TYPE,NULLABLE,INDEX_COMMENT,COALESCE(EXPRESSION,'') FROM information_schema.statistics WHERE TABLE_SCHEMA='{schema}' ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX",
        "constraints": f"SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE,ENFORCED FROM information_schema.table_constraints WHERE TABLE_SCHEMA='{schema}' ORDER BY TABLE_NAME,CONSTRAINT_NAME",
        "keys": f"SELECT TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION,COLUMN_NAME,COALESCE(REFERENCED_TABLE_NAME,''),COALESCE(REFERENCED_COLUMN_NAME,'') FROM information_schema.key_column_usage WHERE TABLE_SCHEMA='{schema}' ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION",
        "references": f"SELECT TABLE_NAME,CONSTRAINT_NAME,UNIQUE_CONSTRAINT_NAME,MATCH_OPTION,UPDATE_RULE,DELETE_RULE,REFERENCED_TABLE_NAME FROM information_schema.referential_constraints WHERE CONSTRAINT_SCHEMA='{schema}' ORDER BY TABLE_NAME,CONSTRAINT_NAME",
        "checks": f"SELECT CONSTRAINT_NAME,CHECK_CLAUSE FROM information_schema.check_constraints WHERE CONSTRAINT_SCHEMA='{schema}' ORDER BY CONSTRAINT_NAME",
    }
    # All table reads below share one repeatable-read, explicitly READ ONLY transaction.
    sql = ["SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ", "START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY",
           "SELECT '@@IDENTITY'", "SELECT VERSION(), @@server_uuid, DATABASE(), @@port, CURRENT_USER()"]
    for name, query in metadata_queries.items():
        sql.extend([f"SELECT '@@META:{name}'", query])
    for table, names in tables.items():
        encoded = [f"IF({ident(column)} IS NULL,'N',CONCAT('V',HEX(CAST({ident(column)} AS BINARY))))" for column in names]
        sql.extend([f"SELECT '@@TABLE:{table}'", f"SELECT SHA2(CONCAT_WS('|',{','.join(encoded)}),256) FROM {ident(schema)}.{ident(table)}"])
    sql.append("COMMIT")
    output = manage.mysql(";\n".join(sql) + ";")
    metadata = {}
    rows = {}
    identity_rows = []
    current = None
    for line in output.splitlines():
        if line == "@@IDENTITY":
            current = identity_rows
        elif line.startswith("@@META:"):
            current = metadata.setdefault(line.removeprefix("@@META:"), [])
        elif line.startswith("@@TABLE:"):
            current = rows.setdefault(line.removeprefix("@@TABLE:"), [])
        elif current is not None:
            current.append(line)
        else:
            raise RuntimeError("Unexpected snapshot output before a section boundary")
    if len(identity_rows) != 1 or len(identity_rows[0].split("\t")) != 5:
        raise RuntimeError("Snapshot must observe one complete database identity in its read-only transaction")
    version, server_uuid, observed_schema, port, current_user = identity_rows[0].split("\t")
    try:
        mysql_identity = {"version": version, "server_uuid": str(uuid.UUID(server_uuid)),
                          "schema": observed_schema, "port": int(port), "current_user": current_user}
    except (ValueError, TypeError):
        raise RuntimeError("Snapshot database identity has an invalid UUID or port") from None
    if (version != manage.EXPECTED_MYSQL_VERSION or observed_schema != schema
            or not 1 <= mysql_identity["port"] <= 65535 or not current_user):
        raise RuntimeError("Snapshot database identity differs from the required server/catalog")
    if set(rows) != set(tables):
        raise RuntimeError("Snapshot did not capture exactly the enumerated tables")
    for hashes in rows.values():
        if any(not re.fullmatch(r"[a-f0-9]{64}", h) for h in hashes):
            raise RuntimeError("Snapshot expected row digests only; raw data will not be persisted")
    hashes = {table: {"row_count": len(values), "data_sha256": digest(sorted(values)), "column_count": len(tables[table])} for table, values in sorted(rows.items())}
    result = {"label": label, "captured_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), "schema": schema, "mysql_version": version, "mysql_identity": mysql_identity, "jar_sha256": hashlib.sha256(manage.JAR.read_bytes()).hexdigest(), "read_only_transaction": True, "consistent_snapshot": True, "application_ddl": False, "raw_row_values_exported": False, "schema_metadata_sha256": digest(metadata), "schema_metadata_sections_sha256": {name: digest(value) for name, value in metadata.items()}, "tables": hashes, "aggregate_data_sha256": digest(hashes), "table_count": len(tables)}
    target.write_text(json.dumps(result, indent=2) + "\n")
    return target, result


def _has_observed_identity(value):
    identity = value.get("mysql_identity")
    if not isinstance(identity, dict):
        return False
    try:
        uuid.UUID(identity["server_uuid"])
        return (identity["version"] == value.get("mysql_version") == manage.EXPECTED_MYSQL_VERSION
                and identity["schema"] == value.get("schema") and bool(identity["schema"])
                and type(identity["port"]) is int and 1 <= identity["port"] <= 65535
                and isinstance(identity["current_user"], str) and bool(identity["current_user"]))
    except (KeyError, TypeError, ValueError, AttributeError):
        return False


def compare(before, after):
    first = json.loads(Path(before).read_text())
    second = json.loads(Path(after).read_text())
    common = first["tables"].keys() & second["tables"].keys()
    changed = {name: {"before": first["tables"][name], "after": second["tables"][name]} for name in sorted(common) if first["tables"][name] != second["tables"][name]}
    result = {"before": str(before), "after": str(after),
              "jar_sha256_before": first["jar_sha256"], "jar_sha256_after": second["jar_sha256"],
              "database_identity_before": first.get("mysql_identity"), "database_identity_after": second.get("mysql_identity"),
              "same_database_identity": _has_observed_identity(first) and _has_observed_identity(second)
                  and first["mysql_identity"] == second["mysql_identity"],
              "same_jar": first["jar_sha256"] == second["jar_sha256"], "same_schema_metadata": first["schema_metadata_sha256"] == second["schema_metadata_sha256"], "same_all_table_data": first["aggregate_data_sha256"] == second["aggregate_data_sha256"], "added_tables": sorted(second["tables"].keys() - first["tables"].keys()), "removed_tables": sorted(first["tables"].keys() - second["tables"].keys()), "changed_tables": changed, "excluded_tables": []}
    result["failure_reasons"] = [name for name in ("same_jar", "same_database_identity", "same_schema_metadata", "same_all_table_data") if result[name] is not True]
    result["failure_reasons"].extend(name for name in ("added_tables", "removed_tables", "changed_tables") if result[name])
    result["status"] = "FAILED" if result["failure_reasons"] else "PASSED"
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["capture", "compare"])
    parser.add_argument("paths", nargs="+")
    args = parser.parse_args()
    if args.action == "capture":
        if len(args.paths) != 1:
            parser.error("capture requires one new snapshot label")
        path, result = capture(args.paths[0])
        print(json.dumps({"path": str(path), "tables": result["table_count"], "aggregate_data_sha256": result["aggregate_data_sha256"], "read_only": True}, indent=2))
    else:
        if len(args.paths) != 2:
            parser.error("compare requires before and after JSON paths")
        result = compare(*args.paths)
        print(json.dumps(result, indent=2))
        if result["status"] != "PASSED":
            raise SystemExit(1)


if __name__ == "__main__":
    main()
