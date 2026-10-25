#!/usr/bin/env python3
"""Native UI checks against the dedicated local emulator and explicit app doubles."""
from dev import *
import re,xml.etree.ElementTree as ET
results=[]
def tree():
    adb('shell','rm','-f','/sdcard/family-tv-ui.xml')
    output=adb('shell','uiautomator','dump','/sdcard/family-tv-ui.xml').stdout
    if 'dumped to' not in output: raise RuntimeError('UI dump failed: '+output)
    return ET.fromstring(adb('shell','cat','/sdcard/family-tv-ui.xml').stdout)
def bounds(value): return list(map(int,re.findall(r'\d+',value)))
def size(value):
    l,t,r,b=bounds(value);return (r-l,b-t)
def card_bounds(t,label):
    return next(n.get('bounds') for n in t.iter('node') if n.get('content-desc')==label and n.get('resource-id')==APP+':id/card')
def has(t,label):return any(n.get('text')==label or n.get('content-desc')==label for n in t.iter('node'))
def check(name,value):
    results.append({'name':name,'passed':bool(value)})
    (ROOT/'evidence/smoke-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
    if not value:raise AssertionError(name)
    print('PASS '+name,flush=True)
def key(*codes):
    for k in codes:adb('shell','input','keyevent',str(k))
def tap(label):
    t=tree();matches=[n for n in t.iter('node') if n.get('text')==label or n.get('content-desc')==label or n.get('resource-id')==label]
    n=next((n for n in matches if n.get('clickable')=='true'),matches[0])
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def shot(name):
    (ROOT/'evidence'/name).write_bytes(subprocess.check_output([str(ADB),'-s',SERIAL,'exec-out','screencap','-p']))
def focused(t):
    return [(n.get('text'),n.get('content-desc'),n.get('resource-id'),n.get('bounds')) for n in t.iter('node') if n.get('focused')=='true']
assert_local();adb('logcat','-c')
import atexit
old_scale=adb('shell','settings','get','global','animator_duration_scale').stdout.strip()
adb('shell','settings','put','global','animator_duration_scale','0')
atexit.register(lambda: adb('shell','settings','put','global','animator_duration_scale',old_scale))
# Leave touch mode from the previous tap-driven check before testing TV D-pad focus.
key(22)
adb('shell','am','force-stop',APP);adb('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME','-n',COMPONENT)
t=tree();check('首屏仅家庭四卡及顶部控件',all(has(t,n) for n in ['网易爆米花','NBA','宝可梦','海绵宝宝','最近打开']) and not has(t,'云视听极光') and not has(t,'B站'))
check('首页已移除综艺',not has(t,'综艺'))
recent=next(n for n in t.iter('node') if n.get('resource-id')==APP+':id/btnFamily')
recent_bounds=list(map(int,re.findall(r'\d+',recent.get('bounds'))))
check('最近打开为右上角方形小图标',recent.get('text')=='' and recent.get('content-desc')=='最近打开' and recent_bounds[2]-recent_bounds[0]==recent_bounds[3]-recent_bounds[1] and recent_bounds[0]>960 and recent_bounds[3]<300)
family_card_bounds=next(n.get('bounds') for n in t.iter('node') if n.get('focused')=='true' and n.get('resource-id')==APP+':id/card')
family_name_bounds=next(n.get('bounds') for n in t.iter('node') if n.get('text')=='NBA')
shot('family-v045.png')

labels=[n for n in t.iter('node') if n.get('text') in ['NBA','宝可梦','海绵宝宝','网易爆米花','云视听极光','B站','QQ音乐','SmartTube']]
ordered=sorted(labels,key=lambda n:(int(re.findall(r'\d+',n.get('bounds'))[1]),int(re.findall(r'\d+',n.get('bounds'))[0])))
check('首屏四卡顺序',[n.get('text') for n in ordered]==['NBA','宝可梦','海绵宝宝','网易爆米花'])
key(22,22,22,23);t=tree()
check('首行爆米花可以启动',any('com.netease.filmlytv' in n.get('text','') for n in t.iter('node')))
key(4);t=tree();check('爆米花返回保留首行第四格焦点',any(n.get('focused')=='true' and n.get('content-desc')=='网易爆米花' for n in t.iter('node')))
key(21,21,21,20);t=tree()
check('按下连续显示家庭行和应用行',all(has(t,n) for n in ['NBA','宝可梦','海绵宝宝','网易爆米花','云视听极光','B站','QQ音乐','SmartTube']))
scrolled_family_bounds=card_bounds(t,'NBA')
check('第二行获得焦点时首行确实上移',bounds(scrolled_family_bounds)[1] < bounds(family_card_bounds)[1]-100)
app_card_bounds=next(n.get('bounds') for n in t.iter('node') if n.get('focused')=='true' and n.get('resource-id')==APP+':id/card')
app_name_bounds=next(n.get('bounds') for n in t.iter('node') if n.get('text')=='云视听极光')
check('首行与应用行卡片尺寸统一',size(family_card_bounds)==size(app_card_bounds))
check('两行保持连续间距',20 < bounds(app_card_bounds)[1]-bounds(scrolled_family_bounds)[3] < 150)
check('首行与应用行名称尺寸统一',size(family_name_bounds)==size(app_name_bounds))
shot('apps-v045.png')


key(23);t=tree();check('极光应用使用原生无开屏首页入口',any('com.tencent.qqlivetv.open' in n.get('text','') and 'action=4' in n.get('text','') for n in t.iter('node')))
key(4);t=tree();check('极光返回保留焦点和列表位置',card_bounds(t,'云视听极光')==app_card_bounds and card_bounds(t,'NBA')==scrolled_family_bounds and any(n.get('focused')=='true' and n.get('content-desc')=='云视听极光' for n in t.iter('node')))
shot('home.png');before=focused(t);key(22);t=tree();check('原应用左右导航保留',focused(t)!=before)
key(19);t=tree();check('上键进入同列家庭卡片',any(n.get('focused')=='true' and n.get('content-desc')=='宝可梦' for n in t.iter('node')))
check('上键回到首行恢复底部单行四卡',not has(t,'云视听极光') and not has(t,'B站') and abs(bounds(card_bounds(t,'NBA'))[1]-bounds(family_card_bounds)[1])<=12)
shot('family-focus.png');key(23);t=tree();check('首页交付搜索 URI 和极光原生 Action',any('action=59&search_keyword=宝可梦XY' in n.get('text','') and 'com.tencent.qqlivetv.open' in n.get('text','') for n in t.iter('node')))
key(4);t=tree();check('内容返回保留单行首屏及内容焦点',not has(t,'云视听极光') and any(n.get('focused')=='true' and n.get('content-desc')=='宝可梦' for n in t.iter('node')))
# Target the content card, then verify downward navigation returns to the original app row.
key(22,20);t=tree()
check('家庭卡片下键回到应用区',any(n.get('focused')=='true' and n.get('resource-id')==APP+':id/card' for n in t.iter('node')))
previous_app_y=bounds(card_bounds(t,'云视听极光'))[1]
key(20);t=tree();check('继续按下时整个列表再次上移',has(t,'语音 V') and bounds(card_bounds(t,'云视听极光'))[1] < previous_app_y-100);shot('more-apps.png')
key(3);t=tree();check('HOME 恢复单行首屏',has(t,'NBA') and not has(t,'云视听极光'))
key(20);tree();key(3);t=tree();check('复用桌面时 HOME 同样回到单行首屏',has(t,'NBA') and not has(t,'云视听极光'))
tap('最近打开');t=tree();check('首页展开最近打开记录',has(t,'极光观看历史') and has(t,'宝可梦'));shot('recent.png')
tap('极光观看历史');t=tree();check('历史 URI 交付测试替身',any('tenvideo2://?action=10' in n.get('text','') for n in t.iter('node')))
key(4);tree();tap(APP+':id/btnSettings');t=tree();check('原设置按钮仍能打开系统设置',any(n.get('package')=='com.android.settings' for n in t.iter('node')))
key(4);t=tree();check('设置返回后保留单行首屏',has(t,'NBA') and not has(t,'云视听极光'))
key(20);tree()
adb('shell','am','force-stop',APP);adb('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME','-n',COMPONENT);t=tree()
check('从已滚动列表冷启动 HOME 仍恢复四卡首屏',has(t,'NBA') and not has(t,'云视听极光'))
tap('最近打开');t=tree();check('进程重启保留最近打开记录',has(t,'宝可梦'))
key(4);key(3);tree();shot('home.png')
errors=adb('logcat','-d','-s','AndroidRuntime:E').stdout
(ROOT/'evidence/runtime-errors.txt').write_text(errors);check('没有 AndroidRuntime 崩溃','FATAL EXCEPTION' not in errors)
print(json.dumps({'passed':len(results)},ensure_ascii=False))
