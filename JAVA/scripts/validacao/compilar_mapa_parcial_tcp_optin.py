#!/usr/bin/env python3
"""Compila apenas PreparacaoMapaParcialTcpIT pelo classpath XML da fonte corrente.

Segue compilar_fixture_tcp_optin.py. Não executa fixture/HTTP/DDL, não copia para
src/test e não muda o POM. Use depois de preservar a regressão integral corrente.
"""
import argparse
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

from gerenciar_jar_tcp import carregar_ambiente, sha


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for nome in ("project", "xml-classpath", "infra-env", "saida"):
        parser.add_argument("--" + nome, type=Path, required=True)
    args = parser.parse_args()
    project = args.project.resolve()
    if args.saida.exists():
        raise SystemExit("Não sobrescrever o recibo de uma compilação anterior")
    root = ET.parse(args.xml_classpath).getroot()
    props = {p.get("name"): p.get("value") for p in root.findall("properties/property")}
    cp = props.get("java.class.path")
    if not cp:
        raise SystemExit("XML sem java.class.path: use relatório real da execução corrente")
    dependencias = [s for s in cp.split(":") if s and not s.endswith(("/target/classes", "/target/test-classes"))]
    saida_classes = project / "target/test-classes"
    for pasta in (saida_classes, project / "target/classes"):
        if not pasta.is_dir():
            raise SystemExit("A fonte corrente deve ter classes principais/de teste previamente compiladas")
    base = saida_classes / "com/example/siggtrat/auditoria20261029/ContratosHttpBase.class"
    if not base.is_file():
        raise SystemExit("ContratosHttpBase compilado da execução corrente não foi localizado")
    classpath = ":".join([str(saida_classes), str(project / "target/classes")] + dependencias)
    source = project / "scripts/validacao/PreparacaoMapaParcialTcpIT.java"
    if not source.is_file():
        raise SystemExit("Integre o Java somente em scripts/validacao da fonte corrente")
    env = carregar_ambiente(args.infra_env)
    javac = Path(env["JAVA_HOME"]) / "bin/javac"
    subprocess.run([str(javac), "--release", "17", "-encoding", "UTF-8", "-proc:none", "-cp", classpath,
                    "-d", str(saida_classes), str(source)], cwd=project, env=env, check=True)
    destino = saida_classes / "com/example/siggtrat/auditoria20261029/PreparacaoMapaParcialTcpIT.class"
    recibo = dict(fonte=str(source), fonte_sha256=sha(source), classe=str(destino), classe_sha256=sha(destino),
                  classe_base_sha256=sha(base), classpath_fonte_xml=str(args.xml_classpath.resolve()),
                  classpath_xml_sha256=sha(args.xml_classpath), executou_fixture=False,
                  alterou_src_test=False, compilador=str(javac), release=17)
    args.saida.parent.mkdir(parents=True, exist_ok=True)
    with args.saida.open("x") as stream:
        stream.write(json.dumps(recibo, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(recibo, ensure_ascii=False))


if __name__ == "__main__":
    main()
