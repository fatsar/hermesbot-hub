"""Run the installed Hermes API adapter without enabling messaging gateways."""
import asyncio
import ipaddress
import os
import secrets
import sys
from pathlib import Path

home = Path(os.environ['LOCALAPPDATA']) / 'hermes'
sys.path.insert(0, str(home / 'hermes-agent'))
os.environ['HERMES_HOME'] = str(home)
host = input('Bilgisayarin yerel IPv4 adresi (yalniz PC icin Enter): ').strip() or '127.0.0.1'
address = ipaddress.ip_address(host)
if address.version != 4 or not address.is_private or address.is_unspecified:
    raise SystemExit('Yalniz bilgisayara ait ozel IPv4 adresi kullanin; 0.0.0.0 kabul edilmez.')
key = secrets.token_hex(32)
os.environ['API_SERVER_KEY'] = key
os.environ['API_SERVER_HOST'] = host
os.environ['API_SERVER_PORT'] = '8642'

async def main():
    from dotenv import load_dotenv
    load_dotenv(home / '.env', override=False)
    from gateway.config import PlatformConfig
    from gateway.platforms.api_server import APIServerAdapter
    adapter = APIServerAdapter(PlatformConfig(enabled=True, extra={'host':host, 'port':8642, 'key':key}))
    if not await adapter.connect():
        raise SystemExit('API baslatilamadi. 8642 portu kullanimda veya Hermes hazir degil.')
    print('\nBotluk baglantisi hazir.')
    print('Adres: http://' + host + ':8642/v1')
    print('API anahtari: ' + key)
    print('Android: Demo kapali, yerel HTTP izni acik olmali.')
    print('Her baslatmada yeni anahtar uretilir. Bu pencere acik kalmali.')
    print('Yerel HTTP sifrelenmez. Guvendiginiz ozel Wi-Fi aginda kullanin.')
    print('Durdurmak icin Ctrl+C. Hermes ayar dosyalariniz degistirilmez.\n')
    try:
        await asyncio.Event().wait()
    finally:
        await adapter.disconnect()

try:
    asyncio.run(main())
except KeyboardInterrupt:
    pass
