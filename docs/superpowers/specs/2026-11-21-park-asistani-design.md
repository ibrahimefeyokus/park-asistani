# Park Asistanı — Tasarım (kısa)

Tarih: 2026-11-21 · Durum: Onaylandı

## Amaç
Kişisel kullanım için Android uygulaması: araba park edildiğinde konumu otomatik kaydetsin,
arabaya geri dönmeyi kolaylaştırsın, ücretli park süresi dolmadan uyarsın.
Tek kullanıcı (geliştiricinin kendisi), tek telefon: Realme RMX3871, Android 16 (API 36).

## Hedefler
- Araç Bluetooth'u (varsayılan: "Kensa"; "FIAT_4504" de seçilebilir) kopunca konumu otomatik kaydet.
- Bildirim: "Park yeri kaydedildi" + **Süre başlat** / **Yanlış, sil** aksiyonları.
- Elle kayıt: uygulamada büyük "Park ettim" butonu + ana ekran widget'ı.
- Son park yeri ekranı; Google Haritalar'da yürüyüş yol tarifini açan buton (gömülü harita yok, API anahtarı yok).
- Park kaydına not ("B2, 14 numara") ve fotoğraf eklenebilsin.
- Ücretli park sayacı: süre seçilir, bitimden 10 dk önce ve bitişte bildirim.
- Park geçmişi telefonda (yerel veritabanı).
- İlk açılış kurulumu: izinler (konum "her zaman", Bluetooth bağlantı, bildirim, tam zamanlı alarm)
  ve Realme pil kısıtlamasının kaldırılması için ilgili ayar ekranına yönlendirme.

## Kapsam dışı
iPhone, Play Store yayını, hesap/sunucu/bulut yedekleme, gömülü harita.

## Anti-hedefler
- Sessizce çalışmayan otomatik kayıt: pil kısıtı yüzünden kayıt kaçarsa kullanıcı bunu fark edebilmeli
  (kurulum ekranında durum göstergesi).
- Pili belirgin şekilde tüketen sürekli konum takibi.

## Yaklaşımlar
1. **Yerel Kotlin + Jetpack Compose — SEÇİLDİ.** Bluetooth bağlantı olayları, arka plan konumu,
   bildirim ve alarmlar için en güvenilir yol; Realme'nin agresif arka plan kapatmasıyla en iyi başa çıkar.
2. React Native (Expo): ileride iOS'a taşınabilir ama çekirdek iş (BT + arka plan) yine yerel modül ister.
3. Google Haritalar park kaydı + MacroDroid: kodsuz ama sayaç/geçmiş/özel ekran istenen gibi olmaz.

## Teknik kararlar (geliştirici kararları)
- minSdk 31, targetSdk 36. Kotlin, Compose (Material 3), Room, Glance (widget), Play Services Location.
- Park algılama: manifest'e kayıtlı `BluetoothDevice.ACTION_ACL_DISCONNECTED` alıcısı (örtük yayın
  istisnası listesinde). Cihaz adresi seçili araç cihazlarından biriyse `goAsync` içinde
  FusedLocation `getCurrentLocation(HIGH_ACCURACY)`, başarısızsa `lastLocation`.
  Bunun için `ACCESS_BACKGROUND_LOCATION` gerekir.
- Araç cihazı seçimi: ayarlarda eşleşmiş cihaz listesinden çoklu seçim; adresle (MAC) eşleştirme.
- Sayaç: `AlarmManager.setExactAndAllowWhileIdle` + bildirim; uygulama yeniden başlatmada (BOOT_COMPLETED) alarmlar yeniden kurulur.
- Fotoğraf: kamera intent'i ile uygulamanın özel dizinine (FileProvider).
- Derleme: kullanıcının Windows bilgisayarında Gradle wrapper + Android Studio JBR; USB/adb ile kurulum.
- Proje yolu: `C:\Users\ibrah\park-asistani`, paket adı `com.ibrah.parkasistani`.

## Geliştiriciye kalanlar
Ekran düzeni ayrıntıları, renkler/ikon, geçmişte tutulacak kayıt sayısı (öneri: son 50).
