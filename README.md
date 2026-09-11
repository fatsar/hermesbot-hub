# HermesBot Hub

Türkçe arayüzlü Android sohbet istemcisi (Kotlin + Jetpack Compose, Material 3).

**Paket:** `com.sibakimya.hermesbothub` · minSdk 26 · targetSdk 34 · sürüm 1.0.0 (1)

Repo: https://github.com/fatsar/hermesbot-hub

## Demo nasıl çalışır?

- Ayarlar’da **Demo** varsayılan olarak **AÇIK**tır.
- Demo açıkken **ağ çağrısı yoktur**; URL ve Bearer anahtar yok sayılır.
- Tohum botlar: **Asistan** (mavi), **Satınalma** (yeşil), **Haber** (turuncu).
- Sohbet yanıtları yerel şablondur; mesajınızı ve bot kişiliğini kabul eder.

## Canlı mod

1. Ayarlar → Demo’yu kapatın.
2. **Base URL** ve **Bearer** girin → Kaydet (GET `{base}/health`).
3. Sağlık yeşilse bot listesi / sohbet canlı API kullanır:
   - `GET {base}/v1/bots`
   - `POST {base}/v1/chat/completions`

### Base URL ipuçları

| Ortam | Örnek |
|--------|--------|
| Emülatör | `http://10.0.2.2:8787` |
| Fiziksel telefon | `http://192.168.x.x:8787` |

Telefonda `localhost` **çalışmaz** (cihazın kendisini işaret eder).

## Sideload (APK yükleme)

1. Ayarlar → **Bilinmeyen kaynaklardan yükleme** aç.
2. Ekteki / `artifacts/hermesbothub-debug.apk` dosyasını indir → aç → **Yükle**.
3. İlk açılışta **Demo** açık; Canlı için Settings’te Base URL (emülatör `http://10.0.2.2:8787` veya telefon LAN IP) + Bearer + health OK.
4. `localhost` telefonda çalışmaz.

### Sürüm / bütünlük

- **package:** `com.sibakimya.hermesbothub`
- **versionName:** 1.0.0 · **versionCode:** 1
- **SHA-256:** `14845cf81f9edb4ced0a22f23d36586e051156ddcfa44eaf09d47e41e40fd13b`
- Dosya: `hermesbothub-debug.apk` (~17 MB)

Debug imzalıdır; Play Store için değildir. Güncellemede aynı debug anahtarı gerekir.

> Not: APK ikili dosyası bu repoya yüklenmez (boyut sınırı). Dağıtım e-posta eki veya yerel `artifacts/` üzerinden yapılır.

## Derleme

```bash
export JAVA_HOME=/workspace/jdk
export ANDROID_HOME=/workspace/android-sdk
cd /workspace/hermesbot-hub
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk` → kopya: `artifacts/hermesbothub-debug.apk`

## Mimari (MVP)

- `DemoRepository` — çevrimdışı tohum + şablon
- `HermesRepository` — health / bots / chat
- `BotRepository` — Demo/Canlı seçimine göre yönlendirir
- Room: botlar + mesaj geçmişi
- SharedPreferences: ayarlar (anahtar loglanmaz)

Ekran akışı: **Ayarlar → sağlık → Bot listesi → Sohbet**
