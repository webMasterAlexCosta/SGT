"""Readiness checks for the external test helper; never starts Java or HTTP."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

module_path = Path(__file__).with_name('restart-check.py')
spec = importlib.util.spec_from_file_location('restart_check', module_path)
helper = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helper)

class ReadinessTest(unittest.TestCase):
    def marker(self, content, offset=0, pid=123):
        with tempfile.TemporaryDirectory() as temp:
            log = Path(temp) / 'java.log'
            log.write_text(content)
            return helper.fresh_boot_marker(log, offset, pid)

    def test_started_before_runners_does_not_qualify(self):
        self.assertIsNone(self.marker('2026-11-01 INFO 123 --- [main] app : Started AplicacaoBackendSigGtrat in 2 seconds\n'))

    def test_accepting_traffic_of_new_process_qualifies(self):
        line = '2026-11-01 DEBUG 123 --- [main] availability : Application availability state ReadinessState changed to ACCEPTING_TRAFFIC\n'
        self.assertEqual(self.marker(line), line.rstrip())

    def test_old_ready_and_other_pid_are_ignored(self):
        old = '2026-11-01 DEBUG 123 --- [main] availability : Application availability state ReadinessState changed to ACCEPTING_TRAFFIC\n'
        newer = old.replace('123', '456')
        self.assertIsNone(self.marker(old + newer, len(old.encode())))

    def test_refusing_traffic_does_not_qualify(self):
        self.assertIsNone(self.marker('2026-11-01 DEBUG 123 --- [main] availability : Application availability state ReadinessState changed to REFUSING_TRAFFIC\n'))

if __name__ == '__main__':
    unittest.main()
