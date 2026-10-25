#!/usr/bin/env python3
"""Record real emulator motion and verify row anchors; never uses a physical TV."""
from dev import *
import re, xml.etree.ElementTree as ET
OUT=ROOT/'evidence/review-v049'
OUT.mkdir(parents=True,exist_ok=True)
def key(k): adb('shell','input','keyevent',str(k))
def tree():
    adb('shell','uiautomator','dump','/sdcard/row-motion.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/row-motion.xml').stdout)
def cards(t):
    return {n.get('content-desc'):{'bounds':list(map(int,re.findall(r'\d+',n.get('bounds')))), 'focused':n.get('focused')=='true'}
            for n in t.iter('node') if n.get('resource-id')==APP+':id/card'}
def shot(name): (OUT/name).write_bytes(subprocess.check_output([str(ADB),'-s',SERIAL,'exec-out','screencap','-p']))
assert_local()
adb('shell','settings','put','global','animator_duration_scale','1')
key(3);key(22);key(21)
stages=[]
for i,k in enumerate([None,20,20,19,19]):
    if k:key(k)
    t=tree();c=cards(t);stages.append(c);shot('row-'+str(i)+'.png')
anchors=[next(v['bounds'][1] for v in c.values() if v['focused']) for c in stages]
assert max(anchors)-min(anchors)<=2,anchors
assert not any(n in stages[-1] for n in ['云视听极光','B站'])
assert stages[-1]['NBA']['focused']
# No UI dumps while recording: capture the native animation at its normal speed.
key(3)
rec=subprocess.Popen([str(ADB),'-s',SERIAL,'shell','screenrecord','--size','1280x720','--bit-rate','6000000','--time-limit','12','/sdcard/row-motion.mp4'],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
start=time.monotonic();events=[]
for deadline,k in [(2,20),(4,20),(6,19),(8,19)]:
    time.sleep(max(0,start+deadline-time.monotonic()));before=time.monotonic()-start;key(k);events.append({'key':k,'before':before,'after':time.monotonic()-start})
out,err=rec.communicate(timeout=20)
if rec.returncode:raise RuntimeError(err.decode())
adb('pull','/sdcard/row-motion.mp4',str(OUT/'row-motion-native.mp4'))
subprocess.run(['ffmpeg','-y','-loglevel','error','-i',str(OUT/'row-motion-native.mp4'),'-vf','fps=30','-c:v','libx264','-crf','21','-pix_fmt','yuv420p','-movflags','+faststart',str(OUT/'row-motion.mp4')],check=True)
# Repeated direction reversals must end back on the same first-row column.
key(3);tree()
adb('shell','input','keyevent','20','20')
c=cards(tree());assert c['语音 V']['focused'],c
adb('shell','input','keyevent','19','19')
c=cards(tree());assert c['NBA']['focused'] and '云视听极光' not in c
adb('shell','input','keyevent','20','20','19','19')
c=cards(tree());assert c['NBA']['focused'] and '云视听极光' not in c
result={'passed':True,'anchor_y_px':anchors,'stages':stages,'recorded_events':events,'rapid_two_rows_roundtrip':True,'animation_scale':1,'device':SERIAL,'physical_tv_tested':False}
(OUT/'motion-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'anchors':anchors,'motion_video':str(OUT/'row-motion.mp4'),'rapid_two_rows_roundtrip':True},ensure_ascii=False),flush=True)
