#!/usr/bin/env python3
"""Exercise the actual signed Home -> stopped keeper path, exclusively on FamilyTV_UI."""
from dev import *

assert_local()
P = 'live.tianyi.tvadbkeeper'
SETUP = P + '/.SetupActivity'
def run(*args): return adb(*args).stdout.strip()
def logs(): return run('logcat', '-d', '-s', 'FamilyAdbKeeper:I', 'ActivityManager:W', 'AndroidRuntime:E')
def home(): run('shell', 'am', 'start', '-W', '-n', COMPONENT)
def stop(): run('shell', 'am', 'force-stop', P)

run('shell', 'am', 'start', '-W', '-a', 'android.settings.SETTINGS')
run('shell', 'am', 'force-stop', APP)
run('shell', 'am', 'start', '-W', '-n', SETUP, '--ez', 'disable', 'false')
stop()
assert 'stopped=true' in run('shell', 'dumpsys', 'package', P)
run('logcat', '-c')
# Shell has DUMP but not the companion's signature permission.
run('shell', 'am', 'broadcast', '-a', P + '.HOME_READY', '-n', P + '/.HomeReadyReceiver', '-f', '0x20')
time.sleep(1)
denied = run('shell', 'dumpsys', 'activity', 'broadcasts')
assert 'Permission Denial: broadcasting Intent { act=' + P + '.HOME_READY' in denied
assert 'home-ready:' not in logs()
stop()
home()
for _ in range(30):
    current = logs()
    if 'home-ready: before=1 enabled=true' in current: break
    time.sleep(.2)
else: raise AssertionError('Signed Home did not wake the stopped keeper: ' + current)
assert 'stopped=false' in run('shell', 'dumpsys', 'package', P)
for job in range(4310, 4314):
    run('shell', 'cmd', 'jobscheduler', 'run', '-f', P, str(job))
    for _ in range(30):
        current = logs()
        if 'job-' + str(job) + ': before=1 enabled=true' in current: break
        time.sleep(.2)
    else: raise AssertionError('Retry did not execute: ' + str(job))
assert 'FATAL EXCEPTION' not in current
run('shell', 'am', 'start', '-W', '-n', SETUP, '--ez', 'disable', 'true')
stop()
run('logcat', '-c')
home()
time.sleep(1)
assert 'home-ready:' not in logs()
assert 'JOB #' not in run('shell', 'dumpsys', 'jobscheduler', P)
report = {'passed': True, 'unsigned_sender_rejected': True, 'signed_home_wakes_stopped_keeper': True,
          'four_finite_retries_executed': True, 'disabled_maintenance_not_reenabled_by_home': True}
out = ROOT / 'evidence/adb-persistence/home-wake-local.json'
out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(report, ensure_ascii=False))
