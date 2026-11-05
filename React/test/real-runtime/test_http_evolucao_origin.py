"""Configuration of the historical fixture consumer; no real HTTP or browser."""
import json
import os
from pathlib import Path
import runpy
import sys
import unittest
from unittest.mock import patch

import test_http_origin as support


class HistoricalSetupOriginTest(unittest.TestCase):
    # Reuse only the controlled private runtime, without discovering the
    # general origin test cases a second time in this consumer module.
    setUp = support.HttpOriginConfigurationTest.setUp
    configured = support.HttpOriginConfigurationTest.configured

    def prepare_historical_fixture(self):
        fixture = {
            "synthetic": True,
            "accounts": {
                "gestor_local": {"id": 17, "username": "sgt.synthetic", "matricula": "99009901", "password": "senha-ficticia"},
                "ind08_historico": {"matricula": "99009902", "password": "senha-historica-ficticia"},
            },
            "ids": {"gestao_origem": 61, "tipo": 62, "lotacao": 63, "jornada": 64, "cargo": 65},
        }
        (self.private / "e2e-fixture.json").write_text(json.dumps(fixture))
        historical = {"matriculaGestor": "99009902", "contratoId": 37, "dataReferencia": "2026-11-01",
                      "conhecidoAntes": "2026-11-01T10:00:00Z", "conhecidoAposDescricao": "2026-11-01T11:00:00Z",
                      "conhecidoAposMatricula": "2026-11-01T12:00:00Z", "conhecidoAposAusencia": "2026-11-01T13:00:00Z",
                      "provaDescricaoId": 71, "provaMatriculaId": 72, "provaAusenciaId": 73}
        (self.private / "ind08-historico-parcial.json").write_text(json.dumps(historical))

    def setup_driver(self):
        script = Path(__file__).parent.parent / "e2e/preparar-evolucao-governanca-real.py"
        namespace = runpy.run_path(str(script), run_name="configuration_only")
        with patch.dict(os.environ, {"SGT_TEST_RUNTIME": str(self.runtime),
                                    "SGT_FRONTEND_URL": "http://127.0.0.1:18080"}), \
                patch.object(sys, "argv", [str(script), "--label", "origem-configuracao"]):
            namespace["main"]()

    def test_historical_setup_keeps_backend_origin_independent_of_frontend_origin(self):
        self.prepare_historical_fixture()
        with self.configured("http://127.0.0.1:18081"):
            self.setup_driver()
        self.assertGreater(len(self.requests), 0)
        self.assertTrue(all(r.full_url.startswith("http://127.0.0.1:18081/api/") for r in self.requests))
        state = json.loads((self.private / "evolucao-governanca-real-origem-configuracao.json").read_text())
        self.assertEqual(state["baseUrl"], "http://127.0.0.1:18081")
        self.assertTrue(state["ready"])

    def test_historical_setup_refuses_absent_backend_origin_before_requests(self):
        self.prepare_historical_fixture()
        with self.configured(None), self.assertRaisesRegex(ValueError, "SGT_BACKEND_URL"):
            self.setup_driver()
        self.assertEqual(self.requests, [])
