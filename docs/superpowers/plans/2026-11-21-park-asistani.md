# Park Asistanı Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Araç Bluetooth'u kopunca park konumunu otomatik kaydeden, arabaya dönüşte Google Haritalar'a yönlendiren ve ücretli park süresini hatırlatan kişisel Android uygulaması.

**Architecture:** Tek modüllü Kotlin/Compose uygulaması. Saf mantık (cihaz eşleştirme, sayaç zamanları, harita URI'si, kurulum durumu, tekrar kayıt filtresi) Android'den bağımsız sınıflarda ve JVM testleriyle doğrulanır; Android kenarları (receiver, alarm, konum, bildirim, widget) bu sınıfları çağıran ince katmanlardır. Veri Room'da, ayarlar DataStore'da.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, DataStore Preferences, Glance AppWidget, Play Services Location, JUnit4 + Robolectric + kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-11-21-park-asistani-design.md`

## Global Constraints
- Paket: `com.ibrah.parkasistani`; proje kökü `C:\Users\ibrah\park-asistani`.
- minSdk 31, targetSdk 36, compileSdk 36. Hedef cihaz: Realme RMX3871, Android 16.
- Varsayılan araç cihazı adı "Kensa"; "FIAT_4504" de seçilebilir; eşleştirme MAC adresiyle.
- Sayaç uyarıları: bitimden 10 dk önce ve bitişte.
- Geçmiş: en fazla son 50 kayıt.
- Gömülü harita yok, API anahtarı yok, sunucu/hesap yok.
- Arayüz metinleri Türkçe.
- Derleme: Gradle wrapper, `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`; kurulum `adb install -r`.
- adb yolu: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.

## Review Focus
1. Kapalı otoparkta GPS yok: `getCurrentLocation` null/timeout → `lastLocation`'a düş; o da yoksa kayıt yine oluşur (koordinatsız) ve bildirim "Konum alınamadı, not ekle" der. (Task 4 testi)
2. Sürüş sırasında anlık BT kopması: otomatik kayıttan sonra 2 dk içinde aynı araç cihazı yeniden bağlanırsa o otomatik kayıt silinir. (Task 5 testi)
3. Kensa ve FIAT_4504 birlikte kopar: 2 dk içindeki ikinci otomatik kayıt oluşturulmaz. (Task 4 testi)
4. Yeni sayaç başlatılırken eskisi aktif: eski alarmlar iptal edilip yenisi kurulur; süre < 10 dk ise yalnız bitiş alarmı. (Task 6 testi)
5. Telefon yeniden başlatıldı: geleceğe ait sayaç alarmları yeniden kurulur, geçmiştekiler kurulmaz; tam zamanlı alarm izni yoksa esnek alarma düşülür. (Task 6 testi)

---

### Task 1: Proje iskeleti, derleme ve telefona kurulum

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `.gitignore`
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/ibrah/parkasistani/MainActivity.kt`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `MainActivity` (Compose host), `app_name = "Park Asistanı"`.

- [ ] **Step 1:** Kullanıcının Gradle önbelleğinde (`%USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.android.tools.build\gradle`) bulunan en yeni AGP sürümünü ve uyumlu Gradle wrapper'ı (önbellekte 8.14 ve 9.1.0 var) seç; Kotlin ve Compose BOM sürümlerini buna uygun belirle. `git init`.
- [ ] **Step 2:** "Merhaba" yazan tek ekranlı Compose uygulamasını oluştur; JVM test desteği (`testOptions.unitTests.isIncludeAndroidResources = true`, Robolectric) ekle.
- [ ] **Step 3: Doğrula** — Run: `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL. Run: `adb install -r app\build\outputs\apk\debug\app-debug.apk` → Success; `adb shell am start -n com.ibrah.parkasistani/.MainActivity` → uygulama açılır.
- [ ] **Step 4: Commit** — `git commit -m "chore: proje iskeleti"`

### Task 2: Veri katmanı (Room) ve depo

**Files:**
- Create: `app/src/main/java/com/ibrah/parkasistani/data/ParkRecord.kt`, `ParkDao.kt`, `AppDatabase.kt`, `ParkRepository.kt`
- Test: `app/src/test/java/com/ibrah/parkasistani/data/ParkRepositoryTest.kt` (Robolectric, in-memory Room)

**Interfaces:**
- Produces:
  - `@Entity data class ParkRecord(id: Long = 0, savedAt: Long, lat: Double?, lng: Double?, accuracyM: Float?, source: ParkSource, deviceAddress: String?, note: String? = null, photoPath: String? = null, timerEndAt: Long? = null)`
  - `enum class ParkSource { AUTO, MANUAL }`
  - `class ParkRepository(dao: ParkDao)`: `suspend fun insert(r: ParkRecord): Long` (ekledikten sonra 50'yi aşan en eskileri siler), `fun observeLatest(): Flow<ParkRecord?>`, `fun observeHistory(): Flow<List<ParkRecord>>` (yeniden eskiye), `suspend fun get(id: Long): ParkRecord?`, `suspend fun latest(): ParkRecord?`, `suspend fun update(r: ParkRecord)`, `suspend fun delete(id: Long)`
  - `AppDatabase.get(context): AppDatabase` (singleton)

- [ ] **Step 1: Failing tests**
  - `insert_then_latest_returns_newest`: savedAt 1000 ve 2000 olan iki kayıt → `latest().savedAt == 2000`.
  - `history_is_capped_at_50`: 51 kayıt ekle → `observeHistory().first().size == 50` ve en eski (savedAt en küçük) yok.
  - `delete_removes_record`: ekle, sil → `get(id) == null`.
- [ ] **Step 2:** Run `.\gradlew.bat testDebugUnitTest --tests "*ParkRepositoryTest"` → FAIL (sınıflar yok).
- [ ] **Step 3:** Entity, DAO, veritabanı ve depoyu uygula (enum için TypeConverter).
- [ ] **Step 4:** Aynı komut → PASS.
- [ ] **Step 5: Commit** — `feat: park kaydı veri katmanı`

### Task 3: Araç cihazı ayarları ve eşleştirici

**Files:**
- Create: `app/src/main/java/com/ibrah/parkasistani/car/CarDeviceMatcher.kt`, `car/CarDevicePrefs.kt`, `car/BondedDevices.kt`
- Test: `app/src/test/java/com/ibrah/parkasistani/car/CarDeviceMatcherTest.kt`

**Interfaces:**
- Produces:
  - `data class BtDevice(address: String, name: String?)`
  - `object CarDeviceMatcher { fun isCar(address: String, selected: Set<String>): Boolean; fun defaultSelection(bonded: List<BtDevice>): Set<String> }` — adres karşılaştırması büyük/küçük harf duyarsız; varsayılan seçim adı tam olarak "Kensa" olan cihaz(lar).
  - `class CarDevicePrefs(context)`: `val selected: Flow<Set<String>>`, `suspend fun setSelected(addresses: Set<String>)`, `suspend fun selectedOnce(): Set<String>`; hiç ayar yapılmamışsa ilk okumada `defaultSelection(BondedDevices.list(context))` kaydedilir.
  - `object BondedDevices { fun list(context): List<BtDevice> }` (BLUETOOTH_CONNECT izni yoksa boş liste).

- [ ] **Step 1: Failing tests** — `isCar_matches_case_insensitive` ("AA:AA:AA:00:00:01" ∈ {"AA:AA:AA:00:00:01"} → true); `isCar_false_for_other` (Philips adresi → false); `default_selects_Kensa_only` (Kensa, FIAT_4504, HP listesi → yalnız Kensa adresi); `default_empty_when_no_Kensa`.
- [ ] **Step 2:** Run `--tests "*CarDeviceMatcherTest"` → FAIL.
- [ ] **Step 3:** Uygula.
- [ ] **Step 4:** → PASS. **Step 5: Commit** — `feat: araç cihazı seçimi`

### Task 4: Park kaydetme akışı, konum ve bildirim

**Files:**
- Create: `app/src/main/java/com/ibrah/parkasistani/park/ParkSaver.kt`, `location/LocationFetcher.kt`, `notify/Notifications.kt`, `notify/NotificationActionReceiver.kt`
- Modify: `AndroidManifest.xml` (izinler: ACCESS_FINE/COARSE/BACKGROUND_LOCATION, POST_NOTIFICATIONS; receiver kaydı)
- Test: `app/src/test/java/com/ibrah/parkasistani/park/ParkSaverTest.kt`

**Interfaces:**
- Consumes: `ParkRepository` (Task 2).
- Produces:
  - `data class Fix(lat: Double, lng: Double, accuracyM: Float)`
  - `interface LocationFetcher { suspend fun current(): Fix? }`; gerçek uygulama `FusedLocationFetcher(context)`: `getCurrentLocation(PRIORITY_HIGH_ACCURACY)` 20 sn zaman aşımıyla, null ise `lastLocation`, o da yoksa null.
  - `class ParkSaver(repo, location: LocationFetcher, clock: () -> Long)`: `suspend fun save(source: ParkSource, deviceAddress: String?): SaveResult`
  - `sealed interface SaveResult { data class Saved(id: Long, hasLocation: Boolean); object SkippedDuplicate }` — AUTO kaynakta, son kayıt AUTO ve 2 dk (120_000 ms) içindeyse `SkippedDuplicate`.
  - `object Notifications { fun ensureChannels(ctx); fun showSaved(ctx, id: Long, hasLocation: Boolean); fun showTimer(ctx, id: Long, minutesLeft: Int) }` — kanallar `park` ve `timer`. `showSaved` metni: konum varsa "Park yeri kaydedildi", yoksa "Konum alınamadı, not ekle"; aksiyonlar **Süre başlat** (uygulamayı sayaç seçiminde açar) ve **Yanlış, sil** (`NotificationActionReceiver` → `repo.delete(id)` ve bildirimi kapatır).

- [ ] **Step 1: Failing tests** (sahte LocationFetcher ve in-memory repo ile)
  - `save_with_fix_stores_coordinates` → Saved(hasLocation=true), kayıtta lat/lng dolu.
  - `save_without_fix_still_saves` → fetcher null döner → Saved(hasLocation=false), lat == null.
  - `second_auto_within_2min_is_skipped` → t=0 AUTO, t=60_000 AUTO → ikinci sonuç SkippedDuplicate, kayıt sayısı 1.
  - `manual_within_2min_is_not_skipped` → t=0 AUTO, t=60_000 MANUAL → Saved.
- [ ] **Step 2:** Run `--tests "*ParkSaverTest"` → FAIL.
- [ ] **Step 3:** Uygula.
- [ ] **Step 4:** → PASS. **Step 5: Commit** — `feat: park kaydetme ve bildirim`

### Task 5: Bluetooth kopma algılama

**Files:**
- Create: `app/src/main/java/com/ibrah/parkasistani/car/CarBluetoothReceiver.kt`, `car/ReconnectPolicy.kt`
- Modify: `AndroidManifest.xml` (BLUETOOTH_CONNECT izni; `ACL_DISCONNECTED` ve `ACL_CONNECTED` için exported=false receiver)
- Test: `app/src/test/java/com/ibrah/parkasistani/car/ReconnectPolicyTest.kt`

**Interfaces:**
- Consumes: `CarDeviceMatcher`, `CarDevicePrefs`, `ParkSaver`, `Notifications`, `ParkRepository`.
- Produces: `object ReconnectPolicy { fun shouldDiscard(latest: ParkRecord?, reconnectedAddress: String, now: Long): Boolean }` — latest AUTO, aynı adres (harf duyarsız) ve `now - savedAt <= 120_000` ise true.
- Receiver: `goAsync()` + coroutine. DISCONNECTED ve cihaz araçsa `ParkSaver.save(AUTO, address)`; Saved ise `showSaved`. CONNECTED ve araçsa `ReconnectPolicy.shouldDiscard` true ise kaydı sil ve park bildirimini kapat.

- [ ] **Step 1: Failing tests** — `discard_when_same_device_reconnects_within_2min`; `keep_after_2min`; `keep_for_manual_record`; `keep_for_other_device`.
- [ ] **Step 2:** → FAIL. **Step 3:** Uygula. **Step 4:** → PASS.
- [ ] **Step 5: Cihazda doğrula** — Uygulamaya izinleri adb ile ver (`pm grant ... ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `BLUETOOTH_CONNECT`, `POST_NOTIFICATIONS`), test için seçime telefonun eşleşmiş ve şu an bağlanabilir bir cihazını (ör. kullanıcıyla kararlaştırılan kulaklık) geçici ekle; bağlantıyı kesince `adb shell dumpsys notification | findstr parkasistani` → park bildirimi görünür. Gerçek Kensa testi kullanıcıya bırakılır.
- [ ] **Step 6: Commit** — `feat: bluetooth ile otomatik park algılama`

### Task 6: Ücretli park sayacı

**Files:**
- Create: `app/src/main/java/com/ibrah/parkasistani/timer/TimerPlan.kt`, `timer/TimerScheduler.kt`, `timer/TimerAlarmReceiver.kt`, `timer/BootReceiver.kt`
- Modify: `AndroidManifest.xml` (SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, receiver'lar)
- Test: `app/src/test/java/com/ibrah/parkasistani/timer/TimerPlanTest.kt`

**Interfaces:**
- Consumes: `ParkRepository`, `Notifications.showTimer`.
- Produces:
  - `data class Alarm(at: Long, minutesLeft: Int)`
  - `object TimerPlan { fun alarms(endAt: Long, now: Long): List<Alarm> }` — `endAt - 10 dk > now` ise `Alarm(endAt-600_000, 10)`; `endAt > now` ise `Alarm(endAt, 0)`; geçmiştekiler dahil değil.
  - `class TimerScheduler(context)`: `suspend fun start(recordId: Long, durationMin: Int)` (kaydın `timerEndAt`'ini yazar, önce mevcut alarmları iptal eder, sonra kurar), `suspend fun cancel(recordId: Long)`, `suspend fun rescheduleAll()` (en son kaydın aktif sayacı için). `canScheduleExactAlarms()` false ise `setAndAllowWhileIdle`.
  - `showTimer` metni: 10 → "Park süren 10 dakika içinde doluyor", 0 → "Park süren doldu".

- [ ] **Step 1: Failing tests** — `two_alarms_for_60min` (now=0, end=3_600_000 → [3_000_000/10, 3_600_000/0]); `only_end_alarm_when_under_10min` (end=300_000 → [300_000/0]); `no_alarms_when_past` (now=5_000, end=4_000 → []); `restart_after_boot_skips_past_warning` (now=3_100_000, end=3_600_000 → yalnız bitiş).
- [ ] **Step 2:** → FAIL. **Step 3:** Uygula. **Step 4:** → PASS.
- [ ] **Step 5: Cihazda doğrula** — 1 dk'lık sayaç başlat → ~1 dk sonra "Park süren doldu" bildirimi (`dumpsys notification`).
- [ ] **Step 6: Commit** — `feat: ücretli park sayacı`

### Task 7: Ana ekranlar (Compose)

**Files:**
- Create: `app/src/main/java/com/ibrah/parkasistani/nav/MapsIntents.kt`, `ui/AppNav.kt`, `ui/HomeScreen.kt`, `ui/DetailScreen.kt`, `ui/HistoryScreen.kt`, `ui/SettingsScreen.kt`, `ui/TimerPicker.kt`
- Create: `app/src/main/res/xml/file_paths.xml`; Modify: `AndroidManifest.xml` (FileProvider, CAMERA intent sorgusu)
- Test: `app/src/test/java/com/ibrah/parkasistani/nav/MapsIntentsTest.kt`

**Interfaces:**
- Consumes: Task 2–6'daki tüm sınıflar.
- Produces: `object MapsIntents { fun walkingUri(lat: Double, lng: Double): String; fun webUri(lat: Double, lng: Double): String }` → `"google.navigation:q=<lat>,<lng>&mode=w"` (Locale.US ile nokta ondalık); Haritalar yoksa `https://www.google.com/maps/dir/?api=1&destination=<lat>,<lng>&travelmode=walking`.
- Ekranlar: **Ana** (son park: zaman, koordinat/doğruluk, not; "Arabama götür"; büyük "Park ettim"; sayaç durumu ve "Süre başlat" → TimerPicker: 15/30/60/120 dk + özel). Bildirimdeki "Süre başlat" `MainActivity`'yi `extra recordId` ile açıp TimerPicker'ı gösterir. **Detay** (not düzenle, fotoğraf çek/göster, sil). **Geçmiş** (liste → detay). **Ayarlar** (eşleşmiş cihazlar çoklu seçim, kurulum ekranına git).

- [ ] **Step 1: Failing tests** — `walking_uri_uses_dot_decimal` (41.0082, 28.9784 → `google.navigation:q=41.0082,28.9784&mode=w`, varsayılan yerel ayar tr-TR iken de); `web_fallback_uri`.
- [ ] **Step 2:** → FAIL. **Step 3:** MapsIntents ve ekranları uygula. **Step 4:** → PASS; `assembleDebug` başarılı.
- [ ] **Step 5: Cihazda doğrula** — kur, "Park ettim"e bas (`adb exec-out screencap -p` ile ekran görüntüsü alıp kontrol et), "Arabama götür" Haritalar'ı açar, not kaydedilir, geçmişte görünür.
- [ ] **Step 6: Commit** — `feat: ana ekranlar`

### Task 8: İlk kurulum ve durum göstergesi

**Files:**
- Create: `app/src/main/java/com/ibrah/parkasistani/setup/SetupStatus.kt`, `setup/SetupScreen.kt`, `setup/BatterySettings.kt`
- Test: `app/src/test/java/com/ibrah/parkasistani/setup/SetupStatusTest.kt`

**Interfaces:**
- Produces:
  - `enum class SetupItem { NOTIFICATIONS, FINE_LOCATION, BACKGROUND_LOCATION, BLUETOOTH, EXACT_ALARM, BATTERY_UNRESTRICTED, CAR_SELECTED }`
  - `data class SetupState(granted: Set<SetupItem>)` ile `object SetupStatus { fun missing(state: SetupState): List<SetupItem>; fun autoParkReady(state: SetupState): Boolean }` — sıra enum sırası; `autoParkReady` için FINE_LOCATION, BACKGROUND_LOCATION, BLUETOOTH, BATTERY_UNRESTRICTED, CAR_SELECTED şart.
  - `object BatterySettings { fun intents(context): List<Intent> }` — sırayla: `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (paketle), Realme/ColorOS arka plan ekranları, son çare `ACTION_APPLICATION_DETAILS_SETTINGS`; çözülebilen ilki kullanılır.
- Ana ekranın üstünde `autoParkReady` false ise "Otomatik kayıt kapalı — kurulumu tamamla" bandı. İlk açılışta kurulum ekranı gösterilir; arka plan konumu, ön plan konum verildikten sonra ayrı istenir.

- [ ] **Step 1: Failing tests** — `all_granted_ready`; `missing_background_location_not_ready`; `missing_exact_alarm_still_ready` (ama `missing` listesinde); `missing_lists_in_enum_order`.
- [ ] **Step 2:** → FAIL. **Step 3:** Uygula. **Step 4:** → PASS.
- [ ] **Step 5: Commit** — `feat: kurulum ekranı ve durum göstergesi`

### Task 9: Ana ekran widget'ı

**Files:**
- Create: `app/src/main/java/com/ibrah/parkasistani/widget/ParkWidget.kt`, `widget/ParkWidgetReceiver.kt`, `res/xml/park_widget_info.xml`
- Modify: `AndroidManifest.xml`

**Interfaces:**
- Consumes: `ParkSaver.save(MANUAL, null)`, `Notifications.showSaved`.
- Produces: 2x1 Glance widget — "Park ettim" butonu ve son kaydın saati. Kayıttan sonra widget güncellenir.

- [ ] **Step 1:** Uygula. **Step 2:** `assembleDebug` başarılı. **Step 3: Cihazda doğrula** — `adb shell dumpsys appwidget | findstr parkasistani` sağlayıcıyı gösterir; kullanıcı widget'ı ana ekrana ekleyip dener.
- [ ] **Step 4: Commit** — `feat: park widget'ı`

### Task 10: Uçtan uca doğrulama

- [ ] **Step 1:** `.\gradlew.bat testDebugUnitTest assembleDebug` → tüm testler PASS.
- [ ] **Step 2:** Temiz kurulum (`adb uninstall` sonra `install`), kurulum ekranını adım adım geç (kullanıcı onaylar), durum bandının kaybolduğunu ekran görüntüsüyle doğrula.
- [ ] **Step 3:** Kullanıcıya gerçek araç testi listesi ver: Kensa'ya bağlan → kontağı kapat → bildirim; 2 dk içinde yeniden bağlanma → kayıt silinir; sayaç bildirimi.
- [ ] **Step 4: Commit** — `chore: ilk sürüm`
