"""Exercise destructive reset guards with a fake Docker executable; never touches Docker."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'reset-local-qa.sh'

class ResetGuards(unittest.TestCase):
    def run_reset(self, mode='--dry-run', change=None, remote=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            names = ['eema-postgres-data', 'eema-uploads', 'eema-private-documents']
            config = {'name': 'eema-backend', 'services': {
                'postgres': {'image': 'postgres:16-alpine', 'environment': {'POSTGRES_DB': 'eema', 'POSTGRES_USER': 'eema'}},
                'backend': {'environment': {'SPRING_PROFILES_ACTIVE': 'local', 'SPRING_DATASOURCE_URL': 'jdbc:postgresql://postgres:5432/eema', 'SPRING_DATASOURCE_USERNAME': 'eema'}}},
                'volumes': {n: {'name': 'eema-backend_' + n} for n in names}}
            if change:
                change(config)
            (root / 'config.json').write_text(json.dumps(config))
            docker = root / 'docker'
            docker.write_text('''#!/usr/bin/env python3
import json, os, pathlib, sys
root=pathlib.Path(os.environ['QA_FAKE_ROOT'])
with (root/'calls').open('a') as f: f.write(json.dumps(sys.argv[1:])+'\\n')
if sys.argv[1]=='context': print('ssh://remote' if os.environ.get('QA_FAKE_REMOTE')=='1' else 'unix:///var/run/docker.sock')
elif 'config' in sys.argv: print((root/'config.json').read_text())
''')
            docker.chmod(0o755)
            env = dict(os.environ, PATH=str(root) + ':' + os.environ['PATH'], QA_FAKE_ROOT=str(root), QA_FAKE_REMOTE=str(int(remote)))
            env.pop('DOCKER_HOST', None)
            result = subprocess.run(['bash', str(SCRIPT), mode], env=env, capture_output=True, text=True)
            calls = [json.loads(line) for line in (root / 'calls').read_text().splitlines()] if (root / 'calls').exists() else []
            return result, calls

    def assert_refused(self, **kwargs):
        result, calls = self.run_reset(**kwargs)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(any('down' in c or 'up' in c for c in calls))

    def test_explicit_confirmation_required(self):
        self.assert_refused(mode='--yes')

    def test_remote_context_refused(self):
        self.assert_refused(remote=True, mode='--confirm-destroy-local-qa')

    def test_production_profile_refused(self):
        self.assert_refused(change=lambda c: c['services']['backend']['environment'].update(SPRING_PROFILES_ACTIVE='prod'))

    def test_remote_database_refused(self):
        self.assert_refused(change=lambda c: c['services']['backend']['environment'].update(SPRING_DATASOURCE_URL='jdbc:postgresql://staging.example.test/eema'))

    def test_import_override_refused(self):
        self.assert_refused(change=lambda c: c['services']['backend']['environment'].update(SPRING_CONFIG_IMPORT='optional:file:/tmp/production.yml'))

    def test_external_volume_refused(self):
        self.assert_refused(change=lambda c: c['volumes']['eema-postgres-data'].update(external=True))

    def test_email_enabled_refused(self):
        self.assert_refused(change=lambda c: c['services']['backend']['environment'].update(EMAIL_NOTIFICATIONS_ENABLED='true'))

    def test_dry_run_does_not_mutate(self):
        result, calls = self.run_reset()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertFalse(any('down' in c or 'up' in c for c in calls))

    def test_confirmed_sequence_and_health(self):
        result, calls = self.run_reset(mode='--confirm-destroy-local-qa')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(1, sum('down' in c and '-v' in c for c in calls))
        self.assertEqual(1, sum('up' in c and '--build' in c for c in calls))
        self.assertTrue(any('pg_isready' in c for c in calls))
        self.assertTrue(any('wget' in c for c in calls))

if __name__ == '__main__':
    unittest.main()
