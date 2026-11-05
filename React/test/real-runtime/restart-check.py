#!/usr/bin/env python3
"""Restart the configured candidate JAR, preserve read-only snapshots, and remain live."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import time
import manage
import snapshot


def alive(pid):
    try:
        os.kill(pid, 0)
        return True
    except ProcessLookupError:
        return False


def fresh_boot_marker(log_path, offset, pid):
    """Accept only readiness AFTER Spring runners for the new JVM, never Started."""
    with Path(log_path).open("rb") as stream:
        stream.seek(offset)
        appended = stream.read().decode(errors="replace")
    pattern = re.compile(rf"\s{pid}\s+---.*\bReadinessState changed to ACCEPTING_TRAFFIC\b")
    return next((line for line in appended.splitlines() if pattern.search(line)), None)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--expected-jar-sha256", required=True)
    parser.add_argument("--label", required=True)
    args = parser.parse_args()
    manage.require_configuration()
    if (manage.EVIDENCE / (args.label + "-restart-comparison.json")).exists():
        raise RuntimeError("Refusing to overwrite a preserved restart receipt; use a new label")
    comparison = {"status": "FAILED", "stage": "preconditions",
                  "expected_jar_sha256": args.expected_jar_sha256.lower(),
                  "http_requests_performed_by_helper": 0, "java_sources_modified": False,
                  "application_ddl_executed_by_helper": False}
    child = None
    recorded = False
    try:
        if hashlib.sha256(manage.JAR.read_bytes()).hexdigest() != comparison["expected_jar_sha256"]:
            raise RuntimeError("JAR differs from the reviewed candidate; refusing restart")
        old_pid = int((manage.PRIVATE / "java.pid").read_text())
        comparison["old_java_pid"] = old_pid
        comparison["stage"] = "shutdown"
        manage.stop("app")
        for _ in range(150):
            if not alive(old_pid):
                break
            time.sleep(0.2)
        else:
            raise RuntimeError("Graceful JAR shutdown timed out; no forced restart attempted")
        comparison["stage"] = "capture_before"
        before, _ = snapshot.capture(args.label + "-before")
        comparison["before"] = str(before)
        application_log = manage.PRIVATE / "app/java.log"
        log_offset = application_log.stat().st_size
        with (manage.PRIVATE / "restart-supervisor.log").open("ab") as log:
            child = subprocess.Popen([sys.executable, str(manage.ROOT / "manage.py"), "serve-app", "--expected-jar-sha256", args.expected_jar_sha256], stdout=log, stderr=log)

            def stop(_sig, _frame):
                child.terminate()

            signal.signal(signal.SIGTERM, stop)
            signal.signal(signal.SIGINT, stop)
            comparison["stage"] = "readiness"
            new_pid = None
            boot_marker = None
            for _ in range(150):
                if child.poll() is not None:
                    raise RuntimeError("Restarted JAR supervisor exited before a fresh startup marker")
                try:
                    new_pid = int((manage.PRIVATE / "java.pid").read_text())
                    if new_pid != old_pid and alive(new_pid):
                        boot_marker = fresh_boot_marker(application_log, log_offset, new_pid)
                        if boot_marker:
                            break
                except (OSError, ValueError):
                    pass
                time.sleep(0.5)
            else:
                raise RuntimeError("Restarted JAR did not produce a fresh startup marker while alive")
            # No HTTP request is made anywhere in this helper. Capture and compare
            # persistence immediately after boot, before the coordinator resumes HTTP.
            comparison.update({"new_java_pid": new_pid, "readiness_method": "fresh_accepting_traffic_after_runners_for_new_live_java_pid",
                               "readiness_marker": boot_marker, "startup_runners_completed": True,
                               "stage": "capture_after"})
            after, _ = snapshot.capture(args.label + "-after")
            comparison["after"] = str(after)
            if child.poll() is not None or not alive(new_pid):
                raise RuntimeError("Restarted JVM exited before the post-boot snapshot could be certified")
            comparison.update(snapshot.compare(before, after))
            comparison["stage"] = "comparison"
            comparison["matches_expected_jar"] = (comparison.get("jar_sha256_before")
                == comparison["expected_jar_sha256"] == comparison.get("jar_sha256_after"))
            failures = [name for name in ("same_jar", "same_database_identity", "same_schema_metadata",
                        "same_all_table_data", "matches_expected_jar") if comparison.get(name) is not True]
            failures.extend(name for name in ("added_tables", "removed_tables", "changed_tables", "excluded_tables")
                            if comparison.get(name))
            comparison["failure_reasons"] = failures
            comparison["status"] = "FAILED" if failures else "PASSED"
            manage.public_json(args.label + "-restart-comparison.json", comparison)
            recorded = True
            print(json.dumps(comparison, indent=2), flush=True)
            if failures:
                raise SystemExit(1)
            print("Restart validation recorded; keeping the exact JAR live until stop-app.", flush=True)
            child.wait()
    except Exception as error:
        if not recorded:
            comparison.update({"status": "FAILED", "error_type": type(error).__name__, "error": str(error)})
            manage.public_json(args.label + "-restart-comparison.json", comparison)
            print(json.dumps(comparison, indent=2), flush=True)
        raise
    finally:
        if child is not None and child.poll() is None:
            child.terminate()
            child.wait(timeout=40)


if __name__ == "__main__":
    main()
