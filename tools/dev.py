#!/usr/bin/env python3
"""Reproducible local-only workflow. Never connects to a network TV."""
import argparse, json, os, pathlib, subprocess, time
ROOT=pathlib.Path(__file__).resolve().parents[1]
SDK=pathlib.Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or ('/opt/homebrew/share/android-commandlinetools' if pathlib.Path('/opt/homebrew/share/android-commandlinetools').is_dir() else pathlib.Path.home()/'Android/Sdk'))
ADB=SDK/'platform-tools/adb'
SERIAL='emulator-5580'
AVD='FamilyTV_UI'
APP='live.tianyi.familytv.dev'
COMPONENT=APP+'/com.gathin.tvhome.MainActivity'

def adb(*args, capture=True):
    return subprocess.run([str(ADB),'-s',SERIAL,*args],check=True,capture_output=capture,text=True,timeout=90)

def assert_local(serial=SERIAL):
    if serial != SERIAL: raise RuntimeError('只允许本项目 emulator-5580；禁止真实电视或其他设备')
    if adb('shell','getprop','ro.kernel.qemu').stdout.strip() != '1': raise RuntimeError('目标不是模拟器')
    if adb('emu','avd','name').stdout.splitlines()[0].strip()!=AVD: raise RuntimeError('目标不是 FamilyTV_UI 专用环境')

def start():
    if AVD not in subprocess.check_output([str(SDK/'emulator/emulator'),'-list-avds'],text=True).splitlines():
        image='system-images;android-36;google_apis;arm64-v8a'
        subprocess.run([str(SDK/'cmdline-tools/latest/bin/sdkmanager'),image],check=True)
        subprocess.run([str(SDK/'cmdline-tools/latest/bin/avdmanager'),'create','avd','-n',AVD,'-k',image,'-d','tv_1080p'],input='no\n',text=True,check=True)
    devices=subprocess.check_output([str(ADB),'devices'],text=True)
    if SERIAL not in devices:
        log=open(ROOT/'evidence/emulator.log','a')
        subprocess.Popen([str(SDK/'emulator/emulator'),'@'+AVD,'-port','5580','-no-window','-no-audio','-no-boot-anim','-gpu','swiftshader','-memory','2560'],stdout=log,stderr=log,start_new_session=True)
    for _ in range(90):
        try:
            if adb('shell','getprop','sys.boot_completed').stdout.strip()=='1': break
        except subprocess.SubprocessError: pass
        time.sleep(1)
    else: raise RuntimeError('模拟器未启动完成，请查看 evidence/emulator.log')
    assert_local();adb('shell','wm','size','1920x1080');adb('shell','wm','density','320')
    print('FamilyTV_UI ready: Android emulator, 1920x1080 / density 320')

def build():
    env=os.environ.copy();env['ANDROID_HOME']=str(SDK)
    if 'JAVA_HOME' not in env and pathlib.Path('/opt/homebrew/opt/openjdk@21').is_dir():
        env['JAVA_HOME']='/opt/homebrew/opt/openjdk@21'
    subprocess.run(['./gradlew','--no-daemon','--console=plain',':app:testReleaseUnitTest',':app:lintRelease',':app:assembleRelease',':fixtures:assembleDebug'],cwd=ROOT/'launcher',env=env,check=True)

def install():
    assert_local()
    for apk in sorted((ROOT/'launcher/fixtures/build/outputs/apk').glob('*/debug/*.apk')):
        adb('install','-r',str(apk))
    apk=ROOT/'launcher/app/build/outputs/apk/release/app-release.apk'
    adb('install','-r',str(apk))
    adb('shell','cmd','locale','set-app-locales',APP,'--user','0','--locales','zh-CN')
    adb('shell','settings','put','system','time_12_24','24')
    adb('shell','settings','put','global','policy_control','immersive.full='+APP)
    adb('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME','-n',COMPONENT)
    # Install the theme through the launcher's narrow theme provider.
    for path in sorted((ROOT/'theme').rglob('*')):
        if not path.is_file(): continue
        rel=path.relative_to(ROOT/'theme').as_posix();temp='/data/local/tmp/family-tv-'+path.name
        adb('push',str(path),temp)
        adb('shell',f'content write --uri content://{APP}.theme/{rel} < {temp}')
        adb('shell','rm',temp)
    adb('shell','am','force-stop',APP)
    adb('shell','cmd','package','set-home-activity',COMPONENT)
    adb('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME','-n',COMPONENT)
    print('仅模拟器已安装。极光等应用是测试替身，不验证真实播放。')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['start','build','install','up','check','home','screenshot'])
    action=p.parse_args().action;(ROOT/'evidence').mkdir(exist_ok=True)
    if action=='up':start();build();install()
    elif action=='start':start()
    elif action=='build':build()
    elif action=='install':install()
    else:
        assert_local()
        if action=='home':adb('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME','-n',COMPONENT)
        elif action=='screenshot':
            out=ROOT/'evidence/home.png';out.write_bytes(subprocess.check_output([str(ADB),'-s',SERIAL,'exec-out','screencap','-p']));print(out)
        else:print('LOCAL_ONLY: '+SERIAL+' / '+AVD)
