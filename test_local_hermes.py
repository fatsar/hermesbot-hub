"""Temporary loopback-only integration test; never changes Hermes configuration."""
import asyncio, json, os, secrets, sys, subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent
HERMES = Path(os.environ['LOCALAPPDATA']) / 'hermes'
sys.path.insert(0, str(HERMES / 'hermes-agent'))
os.environ['HERMES_HOME'] = str(HERMES)
key = secrets.token_hex(32)
os.environ['API_SERVER_KEY'] = key
os.environ['API_SERVER_HOST'] = '127.0.0.1'
os.environ['API_SERVER_PORT'] = '18642'

async def main():
    from dotenv import load_dotenv
    load_dotenv(HERMES / '.env', override=False)
    from gateway.config import PlatformConfig
    from gateway.platforms.api_server import APIServerAdapter
    import aiohttp
    adapter = APIServerAdapter(PlatformConfig(enabled=True, extra={'host':'127.0.0.1','port':18642,'key':key}))
    if not await adapter.connect():
        raise RuntimeError('Temporary Hermes API could not start')
    try:
        async with aiohttp.ClientSession(headers={'Authorization':'Bearer '+key}, timeout=aiohttp.ClientTimeout(total=180)) as client:
            async with client.get('http://127.0.0.1:18642/v1/models') as response:
                data=await response.json()
                print('MODELS_HTTP', response.status, 'VALID', isinstance(data.get('data'),list), flush=True)
            async with client.post('http://127.0.0.1:18642/v1/chat/completions', json={'model':'hermes-agent','stream':False,'messages':[{'role':'system','content':'Do not use any tools. This is a connectivity test.'},{'role':'user','content':'Reply with exactly HERMES_BOTS_OK. Do not perform any action or use tools.'}]}) as response:
                data=await response.json()
                result=data.get('choices',[{}])[0].get('message',{}).get('content','')
                report={'status':response.status,'has_reply':bool(result),'expected_marker':'HERMES_BOTS_OK' in result}
                (ROOT/'hermes-integration-result.json').write_text(json.dumps(report),encoding='utf-8')
                print('CHAT_RESULT',json.dumps(report),flush=True)
            if '--android' in sys.argv:
                adb=str(Path(os.environ['LOCALAPPDATA'])/'Android'/'Sdk'/'platform-tools'/'adb.exe')
                result=await asyncio.to_thread(subprocess.run,[adb,'-s','emulator-5554','shell','am','instrument','-w','-e','api_key',key,'com.fatih.hermesbots.test/com.fatih.hermesbots.SmokeInstrumentation'],capture_output=True,text=True,timeout=210)
                passed='PASS:' in result.stdout and 'FAIL:' not in result.stdout
                report['android_end_to_end_pass']=passed
                (ROOT/'hermes-integration-result.json').write_text(json.dumps(report),encoding='utf-8')
                print('ANDROID_END_TO_END',passed,flush=True)
                if not passed:
                    print(result.stdout.replace(key,'[redacted]'),flush=True)
    finally:
        await adapter.disconnect()

asyncio.run(main())
