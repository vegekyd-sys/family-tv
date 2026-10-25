#!/usr/bin/env python3
"""Exercise shared top-bar menus on the dedicated emulator, never a network TV."""
from dev import *
import re, xml.etree.ElementTree as ET
assert_local()
out=ROOT/'evidence/review-v0410';out.mkdir(exist_ok=True)
results=[]
base=json.loads((ROOT/'theme/config.json').read_text())
preview={**base,'live_tv_package':'com.ktcp.video'}
def apply(cfg):
    path=out/'preview-config.json';path.write_text(json.dumps(cfg,ensure_ascii=False))
    adb('push',str(path),'/data/local/tmp/menu-preview.json')
    adb('shell',f'content write --uri content://{APP}.theme/config.json < /data/local/tmp/menu-preview.json')
    adb('shell','am','force-stop',APP)
    home()
def home():
    adb('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME','-n',COMPONENT)
def key(*codes):
    adb('shell','input','keyevent',*[str(code) for code in codes])
def tree():
    adb('shell','uiautomator','dump','/sdcard/home-menus.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/home-menus.xml').stdout)
def node(t,id):
    return next((n for n in t.iter('node') if n.get('resource-id')==APP+':id/'+id),None)
def has(t,text):return any(n.get('text')==text or n.get('content-desc')==text for n in t.iter('node'))
def selected(t):
    n=next((n for n in t.iter('node') if n.get('focused')=='true'),None)
    if n is None:return ''
    return '|'.join([n.get('resource-id',''),n.get('content-desc','')]+[c.get('text','') for c in n.iter('node') if c.get('text')])
def bounds(n):return list(map(int,re.findall(r'\d+',n.get('bounds'))))
def check(label,passed,**details):
    results.append(dict(name=label,passed=bool(passed),**details))
    (out/'checks.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
    if not passed:raise AssertionError(label+' '+str(details))
    print('PASS '+label,flush=True)
def shot(name):
    (out/(name+'.png')).write_bytes(subprocess.check_output([str(ADB),'-s',SERIAL,'exec-out','screencap','-p']))
def menu(index):
    home();tree();key(19);key(*([21]*8));key(*([22]*index)) if index else None
    key(23);return tree()
apply(preview);key(22,21)
geometry=[];titles=[]
t=tree()
def name_height(t):
    n=next(n for n in t.iter('node') if n.get('resource-id')==APP+':id/name' and n.get('text')=='NBA')
    b=bounds(n);return b[3]-b[1]
original_name_height=name_height(t)
check('首页名称具有完整行高',original_name_height>=40,height=original_name_height)
entries=[('inputs','输入源','btnInputs'),('recent','最近打开','btnFamily'),('network','网络','btnWifi'),('library','应用库','btnLibraryTop'),('settings','设置','btnSettings')]
for index,(name,title,button) in enumerate(entries):
    t=menu(index);p=node(t,'homeMenuPanel');h=node(t,'homeMenuTitle')
    check(title+'打开统一面板',p is not None and h is not None and h.get('text')==title)
    geometry.append(bounds(p));titles.append(bounds(h));shot(name)
    check(title+'打开后有可操作焦点',bool(selected(t)))
    if index!=3:
        first=selected(t);key(19,21,22);check(title+'首项上下左右边界稳定',selected(tree())==first)
        key(*([20]*12));bottom=selected(tree());key(20);check(title+'末项下键不越界',selected(tree())==bottom)
    key(4);t=tree();check(title+'返回恢复原入口',node(t,button) is not None and node(t,button).get('focused')=='true' and node(t,'homeMenuPanel') is None)
    check(title+'返回后首页名称未被裁切',name_height(t)==original_name_height,height=name_height(t))
    menu(index);home();t=tree();check(title+'内HOME关闭菜单并回NBA',node(t,'homeMenuPanel') is None and 'NBA' in selected(t))
    check(title+'关闭后首页名称未被裁切',name_height(t)==original_name_height,height=name_height(t))
check('五菜单外框尺寸与位置完全相同',all(g==geometry[0] for g in geometry),bounds=geometry)
check('五菜单标题位置完全相同',all(g==titles[0] for g in titles),bounds=titles)
# Real intent dispatch to explicit test doubles / emulator system settings.
t=menu(0);key(23);t=tree();check('输入源直播入口交付测试替身',any(n.get('package')=='com.ktcp.video' for n in t.iter('node')));key(4)
t=menu(1);key(23);t=tree();check('观看历史保留action10',any('action=10' in n.get('text','') for n in t.iter('node')));key(4)
t=menu(2);key(23);t=tree();check('网络菜单可打开系统WiFi页',any(n.get('package')=='com.android.settings' for n in t.iter('node')));key(4)
t=menu(2);check('网络菜单保留配置的MonoCloud',has(t,'MonoCloud'));key(20,23);t=tree();check('网络应用正常启动',any(n.get('package')=='com.r3studio.monoproxy' for n in t.iter('node')));key(4)
t=menu(4);key(23);t=tree();check('设置菜单可打开原系统设置',any(n.get('package')=='com.android.settings' for n in t.iter('node')));key(4)
t=menu(3)
for _ in range(40):
    if '网易爆米花' in selected(t):break
    key(22);t=tree()
assert '网易爆米花' in selected(t), '测试应用未出现在应用库'
before=selected(t);key(22);t=tree();check('应用库横向移动焦点',selected(t)!=before);shot('library-next')
key(21);tree()
adb('shell','input','keyevent','--longpress','23');t=tree();check('应用库长按管理仍可达',has(t,'打开'));shot('library-actions')
key(4);t=tree();check('关闭管理菜单仍在应用库',node(t,'homeMenuTitle') is not None)
adb('shell','input','keyevent','--longpress','23');tree();key(23);t=tree()
check('应用库管理菜单可以启动应用',any(n.get('package')=='com.netease.filmlytv' for n in t.iter('node')))
key(4)
# Optional settings make a five-row list taller than the shared viewport.
apply({**preview,'module_cards':True,'dock_row':True})
t=menu(4);first=selected(t);key(*([20]*8));t=tree();last=selected(t);shot('settings-scrolled')
check('长列表可滚动到最后一项',first!=last and '编辑程序坞' in last,focused=last)
key(19);check('长列表末项可向上返回',selected(tree())!=last)
apply(preview)
errors=adb('logcat','-d','-s','AndroidRuntime:E').stdout
(out/'runtime-errors.txt').write_text(errors)
check('菜单检查无崩溃','FATAL EXCEPTION' not in errors)
menu(4);shot('settings-final')
print('CHECKS='+str(len(results)),flush=True)
