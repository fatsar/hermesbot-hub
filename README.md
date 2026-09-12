# Botluk 0.1.0 — Android

Türkçe Android istemcisi. Android 8.0 ve üstü. Birden fazla bot profili, ayrı talimat ve sohbet geçmişi, eşzamanlı istekler, bağlantı testi, demo modu ve sohbet paylaşımı içerir.

## İndirme

Son imzalı APK: [`release/Botluk-0.1.0.apk`](release/Botluk-0.1.0.apk). Kaynak ve kurulum paketi: [`release/Botluk-0.1.0-kaynak-ve-kurulum.zip`](release/Botluk-0.1.0-kaynak-ve-kurulum.zip).

## APK kurulumu

Botluk-0.1.0.apk dosyasını telefonunuza indirin. Android istediğinde dosyayı açtığınız uygulama için “Bu kaynaktan yüklemeye izin ver” seçeneğini kullanın. Uygulamayı açıp “Demo botlarıyla dene” ile başlayabilirsiniz. Demo yanıtları örnektir; gerçek model çağrısı yapmaz.

APK hiçbir sunucu adresi, kullanıcı API anahtarı veya test sohbeti içermez. Botları telefonda oluşturursunuz. Bot verileri Android Keystore AES-GCM ile şifrelenir; yedekleme ve cihaz aktarımı kapalıdır. Uygulamayı silmek kayıtları siler.

## Mevcut Windows Hermes kurulumuna bağlanma

Bu bilgisayarda Hermes `%LOCALAPPDATA%\hermes` altında bulunmuştur. Kaynak paketindeki `Start-Botluk.cmd` dosyası mevcut Hermes kurulumunun API adaptörünü açar; mevcut Hermes yapılandırmasını değiştirmez ve diğer mesajlaşma ağ geçitlerini başlatmaz.

1. Telefon ve bilgisayarı aynı güvenilir özel Wi-Fi ağına bağlayın.
2. Bilgisayarda `ipconfig` çalıştırıp Wi-Fi/Ethernet IPv4 adresini bulun (örneğin `192.168.1.20`).
3. `Start-Botluk.cmd` dosyasını açın. İstendiğinde bilgisayarın yerel IPv4 adresini girin. Boş bırakırsanız sadece bilgisayardan erişilir.
4. Ekranda gösterilen adresi ve API anahtarını Android'de botun düzenleme ekranına girin. Demo modunu kapatın, yerel HTTP kutusunu açın, bağlantıyı test edin ve kaydedin.
5. Model / rota alanı `hermes-agent` olarak kalabilir. Modeli ve sağlayıcı erişimini Hermes yönetir.
6. Sunucu penceresi açık kalmalıdır. Her açılışta yeni anahtar üretilir; uygulamada güncelleyin. Windows güvenlik duvarı engelliyorsa yalnızca özel ağda ve yerel alt ağdan TCP 8642 erişimi verin. Modem port yönlendirmesi yapmayın.

Yerel HTTP anahtarı ve mesajları şifrelemez. İnternet üzerinden kullanım için HTTPS veya güvenli bir özel ağ gerekir. Hermes kendi sunucu araçlarını çalıştırabilir; yalnızca güvendiğiniz kişilere anahtar verin. `localhost` telefonda bilgisayarınızı göstermez.

## Hostinger / başka VPS

VPS üzerinde Hermes ve model sağlayıcısını kurun; resmi Hermes API sunucusunu etkinleştirin. API'yi `127.0.0.1:8642` üzerinde tutup alan adınızda TLS sertifikalı ters vekil kullanın. Android botunda örneğin `https://hermes.sizin-alan-adiniz/v1` ve VPS API anahtarını kaydedin. Uygulamayı yeniden derlemek gerekmez. HTTP yerel ağ izni, genel internet adresleri için kullanılmaz.

Kaynak: https://hermes-agent.nousresearch.com/docs/user-guide/features/api-server

## Botların çalışma biçimi ve sınırlar

Her bot ayrı sistem talimatı ve istemci sohbet geçmişidir. Sunucuda ayrı işletim sistemi süreçleri / konteynerler oluşturmaz; aynı Hermes profilinin araç ve dosya izinlerini paylaşır. Ayrı güvenlik alanı isterseniz farklı Hermes profillerini ayrı API adresleriyle kurup her botu ilgili adrese bağlayın.

En fazla dört ağ isteği aynı anda yürür; ek istekler sıraya girer. Sunucunun kendi eşzamanlılık sınırları ayrıca geçerlidir. Bu sürüm 7/24 zamanlanmış otomasyon veya telefon kapalıyken iş takibi içermez. Uygulama süreci Android tarafından kapatılırsa devam eden yanıtı geri alma garantisi yoktur. Ağ zaman aşımı sonrası sunucuda iş devam ediyor olabilir; istekler otomatik tekrarlanmaz.

Sunucu veya demo modu değişikliği botun eski sohbetini temizler; kayıt öncesi bu durum ekranda belirtilir. Sağlayıcı/model seçiminin geçerli olması Hermes yapılandırmasına bağlıdır. Bu sürüm metin sohbeti içindir; dosya ekleme ve sesli sohbet içermez.

## Derleme ve test

Java 17–23 ile Gradle 8.11.1, Android SDK 36 ve Android Gradle Plugin 8.10.1 kullanılır. Bu makinede Java 19 ile derlendi.

```
gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

7 JVM testi: adres normalleştirme, güvenli adres doğrulaması, açık yerel HTTP izni, demo/hata mesajlarının API geçmişinden ayrılması, yetkilendirme ve Türkçe HTTP yanıtı, yönlendirme reddi, hatalı anahtar.

Android API 36 emülatörü: açılış, bot oluşturma, şifreli kayıt/okuma, demo mesaj/yanıt, kayıt ve gezinme test edildi. Mevcut Windows Hermes API adaptörüyle hem doğrudan HTTP hem Android'den gerçek model yanıtı doğrulandı (`hermes-integration-result.json`). Geçici test servisi kapatıldı; kalıcı ağ erişimi veya güvenlik duvarı değişikliği yapılmadı.

Dağıtım APK'sı ayrı bir uygulama imza anahtarıyla imzalanır. İmza anahtarı kaynak/indirme paketine dahil edilmez. APK Play Store'a yayımlanmamıştır.
