#!/usr/bin/env python3
"""Freeze the exact local review bundle. Does not approve or deploy."""
import hashlib,json,pathlib,shutil,datetime,subprocess,argparse
parser=argparse.ArgumentParser()
parser.add_argument("--with-adbkeeper",action="store_true",help="Also freeze the separately opted-in maintenance companion")
args=parser.parse_args()
r=pathlib.Path(__file__).resolve().parents[1]
a=r/'launcher/app/build/outputs/apk/release/app-release.apk'
if subprocess.check_output(['git','status','--porcelain'],cwd=r,text=True).strip():
    raise SystemExit('请先提交本地源代码，再冻结待验收包；此命令不会批准版本。')
commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip()
metadata=json.loads((a.parent/'output-metadata.json').read_text())
keeper=r/'launcher/adbkeeper/build/outputs/apk/release/adbkeeper-release.apk'
files=[a]+([keeper] if args.with_adbkeeper else [])+sorted(x for x in (r/'theme').rglob('*') if x.is_file())
manifest={'version':metadata['elements'][0]['versionName'],'source_commit':commit,'status':'AWAITING_USER_ACCEPTANCE','approval':None,'package':metadata['applicationId'],'created_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'files':{str(f.relative_to(r)):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},'real_tv_installed':False,'real_playback_verified':False}
identity=hashlib.sha256(json.dumps(manifest['files'],sort_keys=True).encode()).hexdigest()[:12]
out=r/'releases'/('review-'+identity);out.mkdir(exist_ok=True)
shutil.copy2(a,out/'family-tv-local.apk');
if args.with_adbkeeper:
    shutil.copy2(keeper,out/'adbkeeper.apk')
    companion=json.loads((keeper.parent/'output-metadata.json').read_text())
    manifest['companion']={'package':companion['applicationId'],'version':companion['elements'][0]['versionName'],'enabled_only_by_explicit_setup':True}
shutil.copytree(r/'theme',out/'theme',dirs_exist_ok=True)
(out/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print(out)
