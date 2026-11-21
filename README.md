# Park Asistanı

Arabanın Bluetooth bağlantısı kopunca park yerini otomatik kaydeden, seni arabana geri götüren ve ücretli park süresini hatırlatan kişisel bir Android uygulaması.

## Özellikler

- **Otomatik kayıt:** Seçtiğin araç Bluetooth cihazı kopunca konum kaydedilir. Kısa kopmalar (2 dk içinde yeniden bağlanma) yok sayılır.
- **Elle kayıt:** "Park ettim" butonu ve ana ekran widget'ı.
- **Arabama götür:** Google Haritalar'da yürüyüş yol tarifi. Harita API anahtarı gerekmez.
- **Not ve fotoğraf:** Kat, sıra, otopark fotoğrafı.
- **Park sayacı:** Bildirim çubuğunda geri sayım. 10 dk kala çift titreşim, süre dolunca 1 dk titreşim.
- **Geçmiş:** Son 50 kayıt, günlere göre.

## Gizlilik

- Uygulamanın **internet izni yoktur**. Hiçbir veri telefondan çıkmaz.
- Kayıtlar Google yedeğine ve telefondan telefona aktarıma dahil edilmez.
- Kilit ekranında bildirim ayrıntıları gizlenir.

## Gereksinimler

- Android 12 (API 31) ve üstü
- Android Studio (JDK 17 veya 21) ve Android SDK 36

## Derleme

```bash
./gradlew testDebugUnitTest   # testler
./gradlew assembleDebug       # geliştirme sürümü
./gradlew assembleRelease     # imzalı sürüm (aşağıya bak)
```

## İmzalı sürüm

İmza anahtarı ve şifreleri **depoda tutulmaz**. Derleme, bilgileri ev klasöründeki bir dosyadan okur:

```
~/.android-keys/park-asistani.properties     (Windows: %USERPROFILE%\.android-keys\)
```

```properties
storeFile=C:/Users/<kullanıcı>/.android-keys/park-asistani.jks
storePassword=...
keyAlias=parkasistani
keyPassword=...
```

Dosyanın yolunu `PARK_SIGNING_PROPS` ortam değişkeniyle değiştirebilirsin. Dosya yoksa `assembleRelease` imzasız bir APK üretir. Debug derlemesi her zaman çalışır.

> ⚠️ Anahtar dosyasını (`.jks`) ve şifre dosyasını güvenli bir yere yedekle. Kaybolursa yeni sürümler eskisinin üzerine kurulamaz. Uygulamayı silip yeniden kurman gerekir ve park geçmişin silinir.
