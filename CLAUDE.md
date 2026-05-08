# CLAUDE.md – Bluetooth Battery Tracker (Android, Kotlin)

## 1. Ziel des Projekts

Native Android-App in Kotlin, die den Akkustand eines verbundenen Bluetooth-Headsets
(zunächst **Valco NL25**) im 5-Minuten-Takt aufzeichnet, persistent speichert und visualisiert.
Erfassung läuft in einem Foreground Service, solange das Zielgerät verbunden ist.
Beim Disconnect wird die laufende Messsession abgeschlossen und gespeichert.

Reine Dokumentation – keine Alerts, kein Cloud-Sync, ein Gerät zur Zeit.

## 2. Funktionale Anforderungen

| #   | Anforderung |
|-----|---|
| F1  | Auswahl genau eines bereits gekoppelten Bluetooth-Headsets |
| F2  | Erfassung des Akkustands alle 5 Minuten, solange das Gerät verbunden ist |
| F3  | Bei Disconnect: laufende Session schließen, finale Speicherung |
| F4  | Tracking-Session wird vom User manuell gestartet und kann manuell gestoppt werden |
| F5  | Visualisierung: Verlaufsdiagramm pro Session und Session-Liste |
| F6  | Export einer, mehrerer oder aller Sessions als CSV und JSON via Storage Access Framework |
| F7  | Hintergrundbetrieb via Foreground Service mit persistenter Notification |
| F8  | Bei Multi-Battery-Geräten (links/rechts/Case): alle verfügbaren Werte erfassen |

## 3. Nicht-Ziele

- iOS-Support
- Mehrere Geräte parallel
- Cloud-Synchronisation
- Push-Benachrichtigungen bei niedrigem Akku
- Eigene Bluetooth-Pairing-UI (System-Settings reicht)
- Multi-User / Mandantenfähigkeit

## 4. Technische Risiken (zuerst klären!)

### R1: API-Zugriff auf den Akkustand für Classic-BT-Headsets
Die NL25 nutzt Bluetooth 5.4 mit Qualcomm QCC30XX-Chipset, BLE-fähige Hardware ist also
vorhanden. Ob ein GATT Battery Service (`0x180F`) nach außen exposed wird, ist damit aber
nicht beantwortet. Sicher ist nur: Akkustand kommt mindestens über Classic BT (HFP Battery
Indicator), denn Android zeigt ihn in den Verbindungs-Details an.

Mögliche Zugriffspfade in der zu prüfenden Reihenfolge:

1. **BLE Battery Service (`0x180F`/`0x2A19`)** – falls die NL25 zusätzlich zum Classic-Profil eine
   BLE-GATT-Schnittstelle anbietet. Das ist bei modernen TWS-Earbuds zunehmend üblich. **Sauberster
   Weg**, vollständig öffentlich. Mit `nRF Connect` als erste Diagnostik prüfen, ob die NL25 als
   BLE-Peripheral mit Battery Service auftaucht.
2. **`android.bluetooth.device.action.BATTERY_LEVEL_CHANGED`** (System-Broadcast, Extra
   `android.bluetooth.device.extra.BATTERY_LEVEL`) – inoffiziell aber breit verbreitet und in vielen
   Android-Versionen für 3rd-party-Apps empfangbar.
3. **`BluetoothDevice.getMetadata(METADATA_MAIN_BATTERY / METADATA_UNTETHERED_*)`** –
   `@SystemApi`, nur via Reflection erreichbar, mit jeder Android-Version potenziell brüchig.
4. **HFP-AT-Commands direkt mitlesen** – nicht ohne Custom-Driver/Modifikation möglich,
   praktisch ausgeschlossen.

**Spike (Meilenstein M1):** Pfade in genau dieser Reihenfolge testen, beim ersten funktionierenden
abbrechen. Pfad 1 wäre die Idealwelt, Pfad 2 die wahrscheinliche Realität, Pfad 3 das Fallback.

### R2: Foreground-Service-Restriktionen ab Android 14
`FOREGROUND_SERVICE_CONNECTED_DEVICE` muss explizit deklariert werden, Service darf nur laufen,
solange ein Gerät verbunden ist. Lifecycle entsprechend an BT-Connect/Disconnect-Events koppeln –
Service stoppt sich selbst beim Disconnect, startet bei nächstem Connect neu.

### R3: Doze / Aggressive Battery Management
Auch mit FG Service kann OEM-spezifisches Aggressive Battery Management (Samsung, Xiaomi, …)
Sampling stören. App fragt einmalig nach Battery-Optimization-Whitelist
(`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`-Intent), erzwingt es aber nicht.

## 5. Tech-Stack

| Bereich            | Wahl                                | Begründung |
|---|---|---|
| Sprache            | Kotlin 2.x                          | Standard |
| Min/Target SDK     | 31 / 36                             | Android 12 minimum für saubere BT-Permissions, Android 16 als Ziel |
| UI                 | Jetpack Compose + Material 3        | Aktueller Standard, kein XML-Layout |
| Navigation         | Navigation Compose                  | Single-Activity-Architektur |
| State Management   | ViewModel + StateFlow               | Standard-MVVM auf Android |
| DI                 | Hilt                                | Verbreiteter Standard, gute Compose-Integration |
| Persistenz         | Room                                | Type-safe, Standard, Migrations stabil |
| Async              | Coroutines + Flow                   | Standard |
| Charts             | Vico                                | Compose-native, leichtgewichtig, ausreichend für Linien |
| Logging            | Timber                              | Default |
| Build              | Gradle Kotlin DSL + Version Catalog | `libs.versions.toml` |
| Lint/Style         | ktlint + detekt                     | Beides leichtgewichtig konfigurieren |
| Tests              | JUnit 5, MockK, Turbine, Robolectric, Compose UI Test | Default |

Bewusst **nicht** dabei: WorkManager (FG Service direkt), MPAndroidChart (zu schwer für unseren
Use-Case, kein Compose), KSP-overengineerte Mapper, Multi-Module-Setup,
Clean-Architecture-Use-Case-Klassen, Modularisierung nach Feature.

## 6. Architektur

Pragmatisches **Single-Module-Projekt** mit klarer 3-Schichten-Trennung – kein eigener Domain-Layer
mit Use-Cases, Repositories werden direkt aus ViewModels verwendet:

```
app/src/main/java/<package>/
├── BatteryTrackerApp.kt              # Application, @HiltAndroidApp
├── MainActivity.kt                    # Single Activity, NavHost
├── di/
│   ├── DatabaseModule.kt
│   └── BluetoothModule.kt
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── DeviceDao.kt
│   │   ├── SessionDao.kt
│   │   ├── SampleDao.kt
│   │   └── entities/
│   │       ├── DeviceEntity.kt
│   │       ├── SessionEntity.kt
│   │       └── SampleEntity.kt
│   ├── bluetooth/
│   │   ├── BatteryLevelReceiver.kt        # ACTION_BATTERY_LEVEL_CHANGED, runtime im Service
│   │   ├── DisconnectReceiver.kt          # ACL_DISCONNECTED, runtime im Service
│   │   └── BluetoothDeviceProvider.kt     # gepairte Geräte + Connection-State-Check
│   ├── export/
│   │   ├── CsvExporter.kt
│   │   └── JsonExporter.kt
│   └── repositories/
│       ├── DeviceRepository.kt
│       └── SessionRepository.kt
├── service/
│   ├── TrackingService.kt            # Foreground Service
│   └── TrackingNotification.kt
└── ui/
    ├── theme/
    │   ├── Color.kt
    │   ├── Theme.kt
    │   └── Type.kt
    ├── navigation/
    │   └── AppNavGraph.kt
    ├── home/
    │   ├── HomeScreen.kt
    │   └── HomeViewModel.kt
    ├── sessiondetail/
    │   ├── SessionDetailScreen.kt
    │   ├── SessionDetailViewModel.kt
    │   └── BatteryChart.kt
    └── settings/
        ├── SettingsScreen.kt
        └── SettingsViewModel.kt
```

### Datenfluss

```
BroadcastReceiver (BATTERY_LEVEL_CHANGED, ACL_DISCONNECTED)
  ↓
TrackingService (Foreground)
  ↓ Coroutine: delay(5.min) → SessionRepository.appendSample()
Room DB
  ↓ Flow<List<Session>>
ViewModel (StateFlow)
  ↓ collectAsStateWithLifecycle()
Composable Screens
```

### Sampling-Mechanismus

Service hält den letzten via Broadcast empfangenen Akkustand in einem `MutableStateFlow`.
Eine Coroutine in `serviceScope` schreibt im 5-Minuten-Takt diesen Cache in die DB:

```kotlin
serviceScope.launch {
    while (isActive) {
        delay(5.minutes)
        latestBatterySnapshot.value?.let { repo.appendSample(it) }
    }
}
```

Wir pollen den BluetoothAdapter **nicht** aktiv – der System-Broadcast pusht uns Werte.
Falls in 5 Minuten kein neuer Broadcast kam (Headset hat keinen neuen Wert geschickt),
schreiben wir den letzten bekannten Wert mit aktuellem Timestamp – das ist OK für eine
Verbrauchskurve.

### Service-Lifecycle

Tracking läuft **nicht** automatisch. Der User startet jede Session bewusst.

- App-Start: Geräteauswahl in `SettingsScreen`. Auswahl wird in DataStore (oder Room) persistiert.
- `HomeScreen` zeigt prominenten **Start-Button**. Aktiv nur, wenn ein Gerät ausgewählt **und**
  laut `BluetoothAdapter.getProfileConnectionState(HEADSET)` aktuell verbunden ist.
- User tippt „Tracking starten" → `startForegroundService(TrackingService)`.
- Service-`onCreate`: legt neue `SessionEntity` an, registriert lokal `BatteryLevelReceiver` und
  einen runtime-`BroadcastReceiver` für `ACL_DISCONNECTED` des Zielgeräts, startet
  Sampling-Coroutine, zeigt persistente Notification.
- Service endet auf zwei Wegen:
  1. **ACL_DISCONNECTED** des Zielgeräts → Service ruft `stopSelf()` nach finalem Sample-Flush.
  2. **User tippt „Tracking stoppen"** in der laufenden Notification oder im UI → ebenfalls
     `stopSelf()`.
- Service-`onDestroy`: schließt Session (`endedAt = now`), de-registriert Receiver, entfernt
  Notification.
- **Reconnect des Geräts während kein Service läuft:** keine Aktion. User muss neu starten.

## 7. Domänenmodell

```kotlin
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val macAddress: String,
    val name: String,
    val firstSeen: Instant,
)

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = DeviceEntity::class,
        parentColumns = ["macAddress"],
        childColumns = ["deviceMac"],
    )],
    indices = [Index("deviceMac")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceMac: String,
    val startedAt: Instant,
    val endedAt: Instant?,        // null = laufend
)

@Entity(
    tableName = "samples",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class SampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Instant,
    val mainLevel: Int?,          // 0..100
    val leftLevel: Int?,          // 0..100, optional
    val rightLevel: Int?,         // 0..100, optional
    val caseLevel: Int?,          // 0..100, optional
)
```

Konvertierung `Instant` ↔ `Long` über Room `TypeConverters`.

## 8. Android-Konfiguration

### `AndroidManifest.xml` (relevante Auszüge)

```xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<application
    android:name=".BatteryTrackerApp"
    ...>

    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <service
        android:name=".service.TrackingService"
        android:foregroundServiceType="connectedDevice"
        android:exported="false" />
</application>
```

`BLUETOOTH_SCAN` bewusst **nicht** deklariert: Wir scannen nicht aktiv, wir lesen nur die Liste
gepairter Geräte und reagieren auf Connection-Events. `BLUETOOTH_CONNECT` reicht.

### `app/build.gradle.kts`

```kotlin
android {
    namespace = "<your.package>"
    compileSdk = 36
    defaultConfig {
        minSdk = 31
        targetSdk = 36
        ...
    }
    buildFeatures { compose = true }
    kotlinOptions { jvmTarget = "17" }
}
```

### `gradle/libs.versions.toml`

Versionskatalog mit allen oben genannten Libs anlegen. Compose BOM verwenden, damit
Compose-Versionen konsistent bleiben.

## 9. Konventionen

- **Branches:** `main` (stable), `feat/*`, `fix/*`, `spike/*`
- **Commits:** Conventional Commits (`feat:`, `fix:`, `chore:`, `refactor:`, `docs:`, `test:`)
- **Formatierung:** ktlint vor jedem Commit (Pre-commit-Hook empfohlen)
- **Lint:** detekt mit Default-Config, in CI als Fehler
- **Tests:**
  - Unit: Repositories, Sampling-Logik (Clock injectable via Hilt)
  - Room: In-Memory-DB für DAO-Tests
  - Compose: `createComposeRule()` für Screen-Tests
  - Robolectric für Receiver-Logik
- **Architektur-Regel:** UI kennt nur ViewModel, ViewModel kennt nur Repository, Repository kennt
  DAOs und Receiver. Keine Cross-Layer-Sprünge.
- **Logging:** Timber überall, kein `Log.d` direkt.

## 10. Roadmap

### M1 – Spike (1–2 Tage, eigener `spike/battery-source`-Branch)
API-Pfad für den Akkustand der NL25 finden. Reihenfolge: (1) BLE-Battery-Service mit `nRF Connect`
prüfen, (2) Minimal-App mit `BroadcastReceiver` auf `BATTERY_LEVEL_CHANGED` und Logcat, (3) wenn
nötig `getMetadata`-Reflection. Beim ersten erfolgreichen Pfad abbrechen, Ergebnis dokumentieren.
Zusätzlich beobachten: tatsächliche Wert-Auflösung (10er- vs. 1er-Schritte) und ob L, R und Case
getrennt gemeldet werden oder nur ein konsolidierter Wert kommt. Spike-Code wird nicht in `main`
gemerged, sondern dient als Erkenntnisbasis für M3.

### M2 – Foundation
Projekt-Skeleton: Hilt, Compose, Material 3, Navigation, Room-Schema v1, Theme, leere Screens.

### M3 – Tracking-Kern
TrackingService mit manuellem Start, beide runtime-Receiver, Sampling-Coroutine, Session-Lifecycle
(Start via UI / Stop via UI oder Disconnect), DAOs + Repository, Notifications mit Stop-Action.

### M4 – UI
Geräteauswahl in Settings, Sessions-Liste in Home, Detail-Screen mit Vico-Linienchart.

### M5 – Export
CSV- und JSON-Serialisierung mit Session-ID-Filter, Storage Access Framework
(`ACTION_CREATE_DOCUMENT`), Share-Intent als Alternative. UI: Long-Press auf einen Session-Eintrag
in `HomeScreen` aktiviert Selection-Mode mit kontextueller Top-App-Bar (Auswahl-Counter,
„Alle auswählen"-Action, „Exportieren"-Action mit Format-Auswahl CSV/JSON).

### M6 – Härtung
Battery-Optimization-Hinweis, Permission-Flows polieren, OEM-Edge-Cases prüfen
(Samsung, Xiaomi, Honor), Reconnect-Verhalten unter Last testen.

## 11. Annahmen

- `min/target/compileSdk = 31/36/36`. „Unterstützung an Android 16" interpretiert als
  „kompatibel zu Android 16", nicht „nur Android 16".
- Sampling-Intervall fix bei 5 min, ohne User-Konfiguration.
- Keine Verschlüsselung der lokalen DB nötig (reine Akkustand-Daten, kein Risiko).
- Kein Crash-Reporting (Sentry o.ä.) im MVP.
- Gradle Kotlin DSL + Version Catalog von Anfang an.

### Annahmen zur Valco NL25 (zu verifizieren in M1)

- Akkustand wird mit Auflösung von **10%-Schritten** gemeldet (typisches HFP-Battery-Indicator-
  Verhalten). Y-Achse und Tooltips entsprechend stufig auslegen, keine Pseudo-Präzision.
- Eine **Tracking-Session ≈ eine Trage-Episode**: Die NL25 hat keine In-Ear-Detection und
  disconnected nicht beim Herausnehmen aus dem Ohr. Disconnects passieren nur bei Ladeschale,
  Ausschalten (langes MFB) oder Out-of-Range – also bei „echten" Beendigungen.
- Erwartete Session-Länge: 1–6 Stunden, 12–72 Samples pro Session bei 5-min-Takt.
- TWS-Earbuds liefern eventuell separate Werte für links, rechts und Case. Datenmodell
  unterstützt das bereits (`leftLevel`, `rightLevel`, `caseLevel`); ob alle Felder gefüllt
  werden, hängt vom API-Pfad ab und wird im M1-Spike geklärt.
- **Valco-App in Entwicklung** (laut Hersteller-Blog 2025/2026): Sollte sie offizielle API-Pfade
  öffnen oder die Firmware um BLE-Services erweitern, wäre das ein Migrations-Pfad weg von
  HFP-Workarounds. Heute irrelevant.

Wenn eine Annahme falsch ist: in dieser Datei korrigieren, dann Code anpassen – nicht umgekehrt.

## 12. Offene Punkte für später

- CSV-Format: konsolidiert mit Session-ID-Spalte, eine Zeile pro Sample. Reicht das?
- Geräteweite Lebensdauer-Statistik über mehrere Sessions?
- Room-Migrations-Strategie ab v2.

## 13. Hinweise an Claude (für die Arbeit am Code)

- **Erst Spike (M1), dann Architektur.** Keinen App-Code in `main` schreiben, bevor R1 geklärt ist.
- Bei Vorschlägen: Einfachheit > Vollständigkeit. Lieber 30-Zeilen-Repository ohne Interface
  als 4 Klassen mit Mappern.
- Keine Vorschläge mit `flutter_*`, RxJava, LiveData, ViewBinding, XML-Layouts –
  Compose, Coroutines, StateFlow.
- Keine Vorschläge mit BLE-Libs (`Nordic`, `RxAndroidBle`) – Classic BT, anderes Problem.
- Service-Code immer mit explizitem `serviceScope` und `SupervisorJob` zeigen, kein
  `GlobalScope`.
- Bei Unsicherheit, ob eine System-API noch funktioniert: kurz Stand benennen, nicht raten.
- Antworten auf Deutsch, Code-Identifier auf Englisch, Kommentare im Code auf Englisch.
