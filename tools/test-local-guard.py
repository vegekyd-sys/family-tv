import importlib.util,pathlib,unittest
from unittest.mock import patch
from types import SimpleNamespace
s=importlib.util.spec_from_file_location('dev',pathlib.Path(__file__).with_name('dev.py'));d=importlib.util.module_from_spec(s);s.loader.exec_module(d)
class GuardTests(unittest.TestCase):
 def test_real_devices_rejected_before_adb(self):
  for serial in ['192.0.2.1:5555','usb-device','emulator-5554','']:
   with patch.object(d,'adb') as call:
    with self.assertRaises(RuntimeError): d.assert_local(serial)
    call.assert_not_called()
 def test_non_emulator_rejected(self):
  with patch.object(d,'adb',return_value=SimpleNamespace(stdout='0')):
   with self.assertRaises(RuntimeError):d.assert_local()
 def test_wrong_avd_rejected(self):
  with patch.object(d,'adb',side_effect=[SimpleNamespace(stdout='1'),SimpleNamespace(stdout='OtherAVD\nOK')]):
   with self.assertRaises(RuntimeError):d.assert_local()
 def test_project_emulator_accepted(self):
  with patch.object(d,'adb',side_effect=[SimpleNamespace(stdout='1'),SimpleNamespace(stdout='FamilyTV_UI\nOK')]):d.assert_local()
unittest.main()
