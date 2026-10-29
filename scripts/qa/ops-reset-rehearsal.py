#!/usr/bin/env python3
"""Destructive reset rehearsal confined to a newly created, uniquely named QA Compose project."""
import base64, json, os, pathlib, subprocess, tempfile, time, urllib.request, uuid

repo=pathlib.Path(__file__).resolve().parents[2]
project='eema-ops-reset-'+uuid.uuid4().hex[:10]
expected=[str(i) for i in range(1,14)]+['100','101','102','103']
app=None

def command(args,check=True):
    result=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=180)
    if check and result.returncode: raise RuntimeError('QA command failed: '+args[0]+' '+args[1])
    return result

def stop_app():
    global app
    if app and app.poll() is None:
        app.terminate()
        try: app.wait(timeout=30)
        except subprocess.TimeoutExpired: app.kill();app.wait(timeout=10)
    app=None

endpoint=command(['docker','context','inspect','--format','{{.Endpoints.docker.Host}}']).stdout.strip()
assert endpoint.startswith('unix://') and (not os.environ.get('DOCKER_HOST') or os.environ['DOCKER_HOST'].startswith('unix://')),'Local Docker socket required'
with tempfile.TemporaryDirectory(prefix=project+'-') as directory:
    root=pathlib.Path(directory)
    config={'name':project,'services':{'postgres':{'image':'postgres:16-alpine','environment':{'POSTGRES_DB':'ops_reset','POSTGRES_USER':'qa','POSTGRES_PASSWORD':'qa-reset-only'},'ports':['127.0.0.1::5432'],'volumes':['database:/var/lib/postgresql/data']},'files':{'image':'postgres:16-alpine','entrypoint':['sh','-c','exec sleep 3600'],'volumes':['public:/media','private:/private']}},'volumes':{'database':{},'public':{},'private':{}}}
    compose_file=root/'compose.json';compose_file.write_text(json.dumps(config))
    compose=['docker','compose','--project-name',project,'--file',str(compose_file)]
    volumes=[project+'_'+name for name in config['volumes']]
    def sql(query):return command(compose+['exec','-T','postgres','psql','-U','qa','-d','ops_reset','-At','-c',query]).stdout.strip()
    def start(label):
        global app
        command(compose+['up','-d'])
        deadline=time.monotonic()+60
        while command(compose+['exec','-T','postgres','pg_isready','-U','qa','-d','ops_reset'],False).returncode:
            assert time.monotonic()<deadline,'PostgreSQL health timeout';time.sleep(.2)
        port=command(compose+['port','postgres','5432']).stdout.strip().rsplit(':',1)[1]
        env={'PATH':os.environ['PATH'],'LANG':'C.UTF-8','SPRING_DATASOURCE_URL':'jdbc:postgresql://127.0.0.1:'+port+'/ops_reset','SPRING_DATASOURCE_USERNAME':'qa','SPRING_DATASOURCE_PASSWORD':'qa-reset-only','JWT_SECRET':base64.b64encode(os.urandom(32)).decode()}
        with (root/(label+'.log')).open('w') as log:
            app=subprocess.Popen(['java','-Xmx384m','-jar',str(repo/'target/backend-0.0.1-SNAPSHOT.jar'),'--spring.profiles.active=local','--server.port=18093','--app.storage.upload-dir='+str(root/'unused-public'),'--app.documents.storage-dir='+str(root/'unused-private'),'--app.resend.enabled=false','--app.mollie.fee-reconciliation-enabled=false','--app.payout-destination.enabled=false'],env=env,stdout=log,stderr=subprocess.STDOUT)
        deadline=time.monotonic()+120
        while time.monotonic()<deadline:
            assert app.poll() is None,'Packaged local app failed to start'
            try:
                with urllib.request.urlopen('http://127.0.0.1:18093/api/actuator/health',timeout=2) as response:
                    if json.load(response)['status']=='UP':break
            except (OSError,ValueError):pass
            time.sleep(.2)
        else:raise AssertionError('Packaged app health timeout')
        assert sql('select version from flyway_schema_history order by installed_rank').splitlines()==expected
        assert int(sql('select count(*) from users'))>=2 and int(sql('select count(*) from venues'))>0
        assert sql('select count(*) from policy_versions')=='0'
        request=urllib.request.Request('http://127.0.0.1:18093/api/auth/login',data=json.dumps({'email':'owner@eema-dev.local','password':'password123'}).encode(),headers={'Content-Type':'application/json'})
        with urllib.request.urlopen(request,timeout=10) as response:assert response.status==200
    try:
        start('before')
        seed_count=sql('select count(*) from venues')
        sql('create table ops_reset_marker(id integer); insert into ops_reset_marker values(1)')
        command(compose+['exec','-T','files','sh','-c','printf qa > /media/marker\nprintf qa > /private/marker'])
        for name in volumes:
            info=json.loads(command(['docker','volume','inspect',name]).stdout)[0]
            assert info['Labels']['com.docker.compose.project']==project
        stop_app();command(compose+['down','-v'])
        assert all(command(['docker','volume','inspect',name],False).returncode!=0 for name in volumes)
        start('after')
        assert sql("select coalesce(to_regclass('ops_reset_marker')::text,'absent')")=='absent'
        assert sql('select count(*) from venues')==seed_count
        assert command(compose+['exec','-T','files','sh','-c','test ! -e /media/marker && test ! -e /private/marker'],False).returncode==0
        result={'result':'PASS','checks':['Only fresh uniquely named QA project and three project-owned volumes targeted','Packaged local profile applied common V1–V14/dev V100–V103 with Hibernate validation','Development seed login succeeded before and after reset','Actual Compose down -v removed database/public/private volumes','Recreated database has no marker and deterministic venue seeds; both file markers absent','No legal policy content seeded; no shared stack or real provider used'],'seedVenueCount':int(seed_count)}
    finally:
        stop_app();command(compose+['down','-v'],False)
    assert all(command(['docker','volume','inspect',name],False).returncode!=0 for name in volumes)
    result['cleanup']='All owned containers/volumes removed'
    (repo/'docs/qa/evidence/ops/reset.json').write_text(json.dumps(result,indent=2)+'\n')
    print('OPS isolated Compose reset: PASS')
