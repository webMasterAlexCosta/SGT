#!/usr/bin/env python3
"""Executa cenários reais recebidos sem alterar seus oráculos; para na primeira falha."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time

parser = argparse.ArgumentParser()
parser.add_argument("cenarios", nargs="+")
parser.add_argument("--recibo", default="sequencia-e2e")
args = parser.parse_args()
runtime = Path(os.environ["SGT_TEST_RUNTIME"]).resolve()
output = Path(os.environ.get("SGT_E2E_OUTPUT", runtime.parent / "evidence/browser"))
output.mkdir(parents=True, exist_ok=True)
here = Path(__file__).resolve().parent
results = []
for name in args.cenarios:
    script = here / (name + ".cjs")
    if not script.is_file():
        raise RuntimeError("Cenário inexistente: " + name)
    start = time.monotonic()
    with (output / (name + ".log")).open("w") as log:
        proc = subprocess.run(["node", str(script)], stdout=log, stderr=subprocess.STDOUT)
    receipt = output / (name + ".json")
    report = json.loads(receipt.read_text()) if receipt.exists() else {}
    row = {"scenario": name, "exit_code": proc.returncode,
           "passed": report.get("passed") is True,
           "assertions": len(report.get("assertions", [])),
           "seconds": round(time.monotonic() - start, 3)}
    results.append(row)
    (output / (args.recibo + ".json")).write_text(json.dumps(results, indent=2) + "\n")
    print(json.dumps(row), flush=True)
    if proc.returncode != 0 or not row["passed"]:
        raise SystemExit(1)
