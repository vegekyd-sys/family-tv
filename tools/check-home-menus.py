#!/usr/bin/env python3
"""Five content-sized Liquid Glass menus, exercised only on the dedicated emulator."""
from dev import *
import re, xml.etree.ElementTree as ET
assert_local()
out=ROOT/'evidence/review-v0411';out.mkdir(exist_ok=True)
results=[]
base=json.loads((ROOT/'theme/config.json').read_text())
preview={**base,'live_tv_package':'com.ktcp.video'}
def apply(cfg):
    path=out/'preview-config.json';path.write_text(json.dumps(cfg,ensure_ascii=False))
    adb('push',str(path),'/data/local/tmp/menu-preview.json')
    adb('shell',f'content write --uri content://{APP}.theme/config.json < /data/local/tmp/menu-preview.json')
    adb('shell','am','force-stop',APP);home()
def home():adb('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME','-n',COMPONENT)
def key(*codes):
    if codes:adb('shell','input','keyevent',*[str(code) for code in codes])
def tree():
    for attempt in range(3):
        try:
            adb('shell','uiautomator','dump','/sdcard/home-menus.xml');break
        except subprocess.CalledProcessError as error:
            print('UI snapshot retry: '+str(error.stderr or error.stdout),flush=True)
            if attempt == 2:raise
            time.sleep(.5)
    return ET.fromstring(adb('shell','cat','/sdcard/home-menus.xml').stdout)
def node(t,id):return next((n for n in t.iter('node') if n.get('resource-id')==APP+':id/'+id),None)
def has(t,text):return any(n.get('text')==text or n.get('content-desc')==text for n in t.iter('node'))
def focus(t):return next((n for n in t.iter('node') if n.get('focused')=='true'),None)
def selected(t):
    n=focus(t)
    return '' if n is None else n.get('content-desc') or n.get('text') or n.get('resource-id','')
def bounds(n):return list(map(int,re.findall(r'\d+',n.get('bounds'))))
def check(label,passed,**details):
    results.append(dict(name=label,passed=bool(passed),**details))
    (out/'checks.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
    if not passed:raise AssertionError(label+' '+str(details))
    print('PASS '+label,flush=True)
def shot(name):(out/(name+'.png')).write_bytes(subprocess.check_output([str(ADB),'-s',SERIAL,'exec-out','screencap','-p']))
def menu(index,bright=False):
    home();tree()
    if bright:key(22);tree()
    key(19);key(*([21]*8));key(*([22]*index));key(23);return tree()
def item(t,label):return next((n for n in t.iter('node') if n.get('content-desc')==label),None)
def navigate(label):
    for _ in range(25):
        t=tree()
        if selected(t)==label:return t
        wanted=item(t,label);now=focus(t)
        if wanted is None:key(20);continue
        x,y,_,_=bounds(now);a,b,_,_=bounds(wanted)
        if abs(y-b)>30:key(20 if b>y else 19)
        else:key(22 if a>x else 21)
    raise AssertionError('未能导航至 '+label)
def name_height(t):
    n=next(n for n in t.iter('node') if n.get('resource-id')==APP+':id/name' and n.get('text')=='NBA')
    b=bounds(n);return b[3]-b[1]
apply(preview);adb('logcat','-c');t=tree();original=name_height(t)
geometry={}
entries=[('inputs','输入源','btnInputs'),('recent','最近打开','btnFamily'),('network','网络','btnWifi'),('library','应用库','btnLibraryTop'),('settings','设置','btnSettings')]
for index,(name,title,button) in enumerate(entries):
    t=menu(index);p=node(t,'homeMenuPanel');h=node(t,'homeMenuTitle')
    check(title+'打开且无确认键穿透',p is not None and h is not None and h.get('text')==title)
    geometry[name]=bounds(p);shot(name)
    check(title+'只有一个可操作焦点',len([n for n in t.iter('node') if n.get('focused')=='true'])==1 and bool(selected(t)))
    b=geometry[name];check(title+'未超出安全显示区域',b[0]>=48 and b[1]>=140 and b[2]<=1880 and b[3]<=1050,bounds=b)
    key(4);t=tree();check(title+'返回恢复原入口',node(t,button) is not None and node(t,button).get('focused')=='true' and node(t,'homeMenuPanel') is None)
    check(title+'返回后首页文字行高不变',name_height(t)==original)
    menu(index);home();t=tree();check(title+'内HOME回NBA首屏',node(t,'homeMenuPanel') is None and selected(t)=='NBA')
check('五菜单不再使用同一尺寸',len({tuple(v) for v in geometry.values()})>=4,bounds=geometry)
check('输入源网络设置均为紧凑控件组',all(geometry[n][2]-geometry[n][0]<=640 for n in ['inputs','network','settings']))
check('应用库按浏览需要保留较大网格',geometry['library'][2]-geometry['library'][0]>geometry['settings'][2]-geometry['settings'][0])
t=menu(0);first=selected(t);key(19,21,22,20);check('单输入源边界保持焦点',selected(tree())==first)
key(23);t=tree();check('输入源路由交付直播替身',any(n.get('package')=='com.ktcp.video' for n in t.iter('node')));key(4)
t=menu(1);navigate('极光观看历史');key(23);t=tree();check('观看历史保留action10',any('action=10' in n.get('text','') for n in t.iter('node')));key(4)
t=menu(2);check('网络保留配置应用',has(t,'MonoCloud'));first=bounds(focus(t));key(22);t=tree();second=bounds(focus(t));check('网络两个操作横排导航',selected(t)=='MonoCloud' and abs(first[1]-second[1])<20 and second[0]>first[0]);key(22);check('网络右边界稳定',selected(tree())=='MonoCloud');key(23);t=tree();check('网络应用启动',any(n.get('package')=='com.r3studio.monoproxy' for n in t.iter('node')));key(4)
menu(2);key(23);t=tree();check('WiFi打开系统设置',any(n.get('package')=='com.android.settings' for n in t.iter('node')));key(4)
t=menu(4);check('设置首项系统设置',selected(t)=='系统设置');shot('settings-system-focus');key(20);t=tree();check('设置下移到桌面设置',selected(t)=='桌面设置');shot('settings-desktop-focus');key(20);t=tree();check('设置导航与编辑分组可达',selected(t) in ['调整顺序','内容卡片']);navigate('调整顺序');a=bounds(focus(tree()));key(22);t=tree();b=bounds(focus(t));check('设置编辑操作横排',selected(t)=='内容卡片' and abs(a[1]-b[1])<20);key(20);check('设置底部不越界',selected(tree())=='内容卡片')
menu(4);key(23);t=tree();check('系统设置入口保持原页面',any(n.get('package')=='com.android.settings' for n in t.iter('node')));key(4)
menu(4);key(20,23);t=tree();check('桌面设置实际打开',node(t,'homeMenuPanel') is None and any('设置' in n.get('text','') for n in t.iter('node')));key(4)
t=menu(3);a=bounds(focus(t));first=selected(t);key(22);t=tree();b=bounds(focus(t));check('应用库同排右移',selected(t)!=first and b[0]>a[0] and abs(b[1]-a[1])<20);key(20);t=tree();c=bounds(focus(t));check('应用库下移跨行',c[1]>b[1] and abs(c[0]-b[0])<20);shot('library-second-row')
key(*([20]*10));t=tree();last=selected(t);key(20);check('应用库末排下键停留',selected(tree())==last);shot('library-scrolled');key(*([19]*12));key(*([21]*5));t=tree();check('应用库上移可返回首项',selected(t)==first)
navigate('网易爆米花');adb('shell','input','keyevent','--longpress','23');t=tree();check('应用库长按只打开管理菜单',has(t,'打开') and has(t,'应用信息'));shot('library-actions');key(4);t=tree();check('关闭管理菜单保留应用焦点',selected(t)=='网易爆米花')
adb('shell','input','keyevent','--longpress','23');tree();key(23);t=tree();check('管理菜单可以启动应用',any(n.get('package')=='com.netease.filmlytv' for n in t.iter('node')));key(4)
# Optional configuration introduces a third edit action; no reserved empty slot in the default.
apply({**preview,'module_cards':True,'dock_row':True});t=menu(4);navigate('编辑程序坞');t=tree();check('可选程序坞编辑可达',selected(t)=='编辑程序坞');shot('settings-optional');key(19);check('可选项可向上返回',selected(tree())!='编辑程序坞')
apply({**preview,'reduced_motion':True});t=menu(4);key(20);check('减少动态效果仍能移动焦点',selected(tree())=='桌面设置')
apply(preview)
for index,(name,_,_) in enumerate(entries):
    t=menu(index,bright=True);shot(name+'-bright')
    check(name+'彩色背景下仍保持菜单与焦点',node(t,'homeMenuPanel') is not None and bool(selected(t)))
errors=adb('logcat','-d','-s','AndroidRuntime:E').stdout;(out/'runtime-errors.txt').write_text(errors)
check('检查期间无崩溃','FATAL EXCEPTION' not in errors)
menu(4,bright=True);shot('settings-final')
print('CHECKS='+str(len(results)),flush=True)
