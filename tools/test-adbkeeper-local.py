#!/usr/bin/env python3
"""Verify the opt-in maintenance module ONLY on FamilyTV_UI. Build :adbkeeper first."""
from pathlib import Path
import sys,time,json
ROOT = Path(__file__).resolve().parents[1]
import os
os.chdir(ROOT)
sys.path.insert(0,str(ROOT/'tools'))
import dev
dev.assert_local()
P='live.tianyi.tvadbkeeper'; C=P+'/.SetupActivity'
r=[]
def run(*a):return dev.adb(*a).stdout.strip()
(ROOT/'evidence/adb-persistence').mkdir(parents=True,exist_ok=True)
r.append(run('install','-r','launcher/adbkeeper/build/outputs/apk/release/adbkeeper-release.apk'))
run('shell','logcat','-c')
run('shell','pm','revoke',P,'android.permission.WRITE_SECURE_SETTINGS')
r.append(run('shell','am','start','-W','-n',C))
time.sleep(3)
s=run('shell','logcat','-d','-s','FamilyAdbKeeper:I','AndroidRuntime:E');assert 'permission missing; no change' in s;s0=s
run('shell','pm','grant',P,'android.permission.WRITE_SECURE_SETTINGS')
r.append(run('shell','am','start','-W','-n',C))
for _ in range(20):
 time.sleep(3)
 s=run('shell','logcat','-d','-s','FamilyAdbKeeper:I','AndroidRuntime:E')
 if 'job-4311: before=1 enabled=true' in s:break
else:raise AssertionError('Delayed job did not run within 60 seconds')
assert run('shell','settings','get','global','adb_enabled')=='1'
run('shell','am','start','-W','-n',C,'--ez','disable','true')
j=run('shell','dumpsys','jobscheduler',P);assert 'JOB #' not in j
Path('evidence/adb-persistence/local-test.json').write_text(json.dumps({'passed':True,'missing_permission_safe':True,'granted_jobs_ran':True,'disable_cancels_jobs':True,'log':s,'operations':r},indent=2))
print('PASS: missing permission safe; authorized jobs executed; disable cancels jobs')
