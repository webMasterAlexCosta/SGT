#!/usr/bin/env python3
"""Compila SOMENTE a fixture opt-in usando classpath da regressão já executada.

Não executa a fixture nem conecta a serviços. Mantém scripts/validacao no
manifesto congelado; não copia Java para src/test, não altera pom.xml.
Execute depois da regressão integral e antes do goal Failsafe opt-in.
"""
import argparse
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

from gerenciar_jar_tcp import carregar_ambiente, sha


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, required=True)
    parser.add_argument("--xml-classpath", type=Path, required=True)
    parser.add_argument("--infra-env", type=Path, required=True)
    parser.add_argument("--saida", type=Path, required=True)
    args = parser.parse_args()
    if args.saida.exists():
        raise SystemExit("Não sobrescrever o recibo de uma compilação anterior")
    project = args.project.resolve()
    root = ET.parse(args.xml_classpath).getroot()
    propriedades = {p.get("name"): p.get("value") for p in root.findall("properties/property")}
    cp = propriedades.get("java.class.path")
    if not cp:
        raise SystemExit("XML não contém java.class.path; use um relatório real da regressão desta fonte.")
    # Classes de projeto vêm da versão indicada. As outras entradas são apenas
    # as dependências resolvidas pelo Maven que constam da execução arquivada.
    dependencias = [s for s in cp.split(":") if s and not s.endswith(("/target/classes", "/target/test-classes"))]
    classpath = ":".join([str(project / "target/test-classes"), str(project / "target/classes")] + dependencias)
    source = project / "scripts/validacao/PreparacaoHttpTcpIT.java"
    env = carregar_ambiente(args.infra_env)
    javac = Path(env["JAVA_HOME"]) / "bin/javac"
    command = [str(javac), "--release", "17", "-encoding", "UTF-8", "-proc:none", "-cp", classpath,
               "-d", str(project / "target/test-classes"), str(source)]
    subprocess.run(command, cwd=project, env=env, check=True)
    dest = project / "target/test-classes/com/example/siggtrat/auditoria20261029/PreparacaoHttpTcpIT.class"
    result = dict(fonte=str(source), fonte_sha256=sha(source), classe=str(dest), classe_sha256=sha(dest),
                  classpath_fonte_xml=str(args.xml_classpath.resolve()), classpath_xml_sha256=sha(args.xml_classpath),
                  executou_fixture=False, alterou_src_test=False, compilador=str(javac), release=17)
    args.saida.parent.mkdir(parents=True, exist_ok=True)
    with args.saida.open("x") as stream:
        stream.write(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
