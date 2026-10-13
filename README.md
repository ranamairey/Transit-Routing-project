# 🚌 Smart Syrian Public Transportation System — Transit Routing

نظام ملاحة ذكي لشبكة النقل العام في سوريا (سرافيس وباصات): يقترح مسارات تبديلية بين نقطتين بأقل تكلفة ووقت، ويحدّث حالة الخطوط من بلاغات الركاب التشاركية.
مبني وفق وثيقة متطلبات البرمجيات (SRS) المرفقة في `docs/`.

| الجزء | التقنية |
|---|---|
| Backend API | Spring Boot 3.2 · Java 17 · Spring Security + JWT · Spring Data MongoDB |
| Database | MongoDB (Atlas أو محلي) — قاعدة `syrian_transit` |
| Rider app (`frontend/`) | React 19 · Vite · Leaflet + OpenStreetMap · PWA (Workbox) · Capacitor (Android) |
| Admin console (`admin-console/`) | React 19 · Vite · Leaflet — تطبيق وحاوية مستقلّان |
| Tests / CI | JUnit 5 · MockMvc · MongoDB مدمج (flapdoodle) · Vitest · GitHub Actions |
| Deployment | Docker Compose (3 حاويات) · Caddy للـ HTTPS التلقائي |

---

## التشغيل السريع

### 1) Backend

> **ملاحظة أمنية:** لا يحتوي المستودع على أي بيانات اتصال أو كلمات سر أو مفاتيح. الخادم **يرفض الإقلاع** ما لم تُعرَّف المتغيرات الثلاثة `MONGO_URI` و`JWT_SECRET` و`SEED_ADMIN_PASSWORD` في البيئة أو في ملف `.env` (المتجاهَل في git). قاعدة البيانات وبيانات الوصول مملوكة لأصحاب المشروع ولا تُشارك.

```bash
cp .env.example .env        # ثم عبّئ MONGO_URI و JWT_SECRET و SEED_ADMIN_PASSWORD
./run.sh                    # يقرأ .env ويشغّل spring-boot:run على المنفذ 8080
```

عند أول تشغيل على قاعدة فارغة يُزرع تلقائياً (`DataSeeder`) حساب المشرف (`SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD`) وشبكة تجريبية لدمشق وريفها (12 محطة، 7 خطوط). تُنشأ حسابات ركاب تجريبية فقط إذا عُرّف `SEED_USER_PASSWORD`. عطّل الزرع بـ `SEED_ENABLED=false`.

### 2) تطبيق الركاب (Rider app, PWA)

```bash
cd frontend
npm install
npm run dev                 # http://localhost:5173 (يُمرّر /api إلى 8080)
```

يعمل كـ PWA قابل للتثبيت: غلاف التطبيق وبلاطات خريطة OpenStreetMap ولقطة الشبكة تُخزَّن محلياً (Workbox) فيعمل التصفح والبحث في المحطات دون اتصال (NFR-1.2).

### 3) لوحة الإدارة (Admin console)

```bash
cd admin-console
npm install
npm run dev                 # http://localhost:5174
```

تطبيق منفصل بالكامل عن تطبيق الركاب (ومنفصل في حاويته): نظرة عامة بالأرقام وخريطة، البلاغات (ملخص، فلاتر، صفحات، خريطة، اعتماد/رفض)، المحطات، الخطوط (محطات مرتبة + رسم المسار)، المستخدمون (إنشاء مشرفين، تغيير الأدوار، إعادة تعيين كلمة السر)، واستيراد/تصدير الشبكة JSON. الدخول للمشرفين فقط.

### 4) تطبيق أندرويد

تطبيق الركاب مغلّف بـ Capacitor. المتطلبات: Android Studio أو Android SDK + JDK 17.

```bash
cd frontend
cp .env.android.example .env.android   # VITE_API_BASE = عنوان الخادم (المحاكي: http://10.0.2.2:8080)
npm run android:apk                     # يبني الويب ثم android/app/build/outputs/apk/debug/app-debug.apk
```

أو `npm run android:open` لفتح المشروع في Android Studio. حجم APK التجريبي ≈ 4.5 MB (الحد في الـ SRS: 25 MB)، الحد الأدنى Android 7.0 (`minSdkVersion` في `android/variables.gradle` = 23 → اضبطه إلى 24 إن رغبت بالتطابق الحرفي). للنشر: `./gradlew assembleRelease` مع مفتاح توقيع.

### 5) الاختبارات

```bash
./mvnw test                  # backend: وحدات + تكامل API على MongoDB مدمج
cd frontend && npm test      # rider app: Vitest
```

يشغّل GitHub Actions (`.github/workflows/ci.yml`) الاختبارات وبناء التطبيقين وبناء APK عند كل push.

### 6) النشر بالحاويات (Docker)

```bash
cp .env.example .env         # MONGO_URI, JWT_SECRET, SEED_ADMIN_PASSWORD (إلزامية) + MONGO_DB, CORS_ORIGINS
docker compose up -d --build # backend:8080 · web:8081 · admin:8082
```

ثلاث حاويات: `backend` (Spring Boot)، `web` (nginx يقدّم تطبيق الركاب ويمرّر /api)، `admin` (nginx يقدّم لوحة الإدارة). قاعدة البيانات MongoDB Atlas.
للـ HTTPS التلقائي (NFR-3.2) عدّل النطاقات في `deploy/Caddyfile` ثم:

```bash
docker compose --profile tls up -d
```

---

## الهيكل

```
src/main/java/com/example/demo/transit
├── config/      SecurityConfig · TransitProperties · MongoConfig · DataSeeder
├── routing/     MultiLegRouter  — بحث Dijkstra متعدد الأرجل (محطة، آخر خط)
├── service/     RoutingService · TransitGraphFactory (كاش الشبكة) · StationSearchService (بحث عربي)
│                ReportService (البلاغات والأتمتة) · NetworkAdminService · AuthService
├── web/         Routing · Station · Network · Report · Auth · AdminNetwork · AdminReport
├── entity/      Station · Route (+stops +polyline) · CrowdsourceReport · User   (وثائق MongoDB)
└── dto/
frontend/src                 (تطبيق الركاب)
├── pages/       PlannerPage (الخريطة والبحث) · Login · Register
├── components/  MapView (Leaflet) · EndpointPicker · TripOptions · AlertsBar · ReportDialog
├── i18n/        عربي / إنجليزي (RTL/LTR)
└── hooks/useNetwork.js   كاش الشبكة محلياً (Offline-first) مع ETag
frontend/android             مشروع Capacitor لأندرويد
admin-console/src            (لوحة الإدارة)
└── pages/       Dashboard · Reports · Stations · Routes · Users · Data (import/export)
deploy/                      nginx · Caddyfile (HTTPS)
docker-compose.yml · Dockerfile · .github/workflows/ci.yml
```

---

## واجهة الـ API

كل المسارات تحت `/api/v1`. الاستجابات JSON مضغوطة (gzip).

### عامة — بدون تسجيل دخول (NFR-3.1)

| الطريقة | المسار | الوصف |
|---|---|---|
| `POST` | `/routing/search` | حساب المسارات التبديلية (FR-1.2) |
| `GET` | `/stations` · `/stations/search?q=` · `/stations/{id}` | المحطات والبحث النصي الذكي (FR-1.1) |
| `GET` | `/routes` · `/routes/{id}` | الخطوط مع المحطات والـ polyline |
| `GET` | `/network` | لقطة كاملة للشبكة مع `ETag` للتخزين دون اتصال (NFR-1.2) |
| `GET` | `/reports?routeId=` | آخر بلاغات خط (FR-2.2) |
| `GET` | `/health` | حالة الخادم وقاعدة البيانات |
| `POST` | `/auth/register` · `/auth/login` · `GET /auth/me` | التوثيق (JWT Bearer) |

### تتطلب تسجيل دخول

| الطريقة | المسار | الوصف |
|---|---|---|
| `POST` | `/reports` | إرسال بلاغ (FR-2.1): `{reportType, routeId, stationId?, newFare?, description?, location?}` — بلاغ واحد لكل خط كل 10 دقائق و20 بلاغاً في الساعة لكل مستخدم (429 عند التجاوز) |

### المشرف فقط (`ROLE_ADMIN`)

| الطريقة | المسار | الوصف |
|---|---|---|
| `GET` | `/admin/stats` | أرقام لوحة الإدارة |
| `GET/POST/PATCH/DELETE` | `/admin/users[/{id}]` | إدارة المستخدمين والأدوار وإعادة تعيين كلمة السر |
| `POST` / `GET` | `/admin/network/import` · `/admin/network/export` | استيراد وتصدير الشبكة (upsert) |
| `POST/PUT/DELETE` | `/admin/stations[/{id}]` | إدارة المحطات (FR-3.1) |
| `POST/PUT/DELETE` | `/admin/routes[/{id}]` | إدارة الخطوط مع المحطات بالترتيب والـ polyline |
| `PATCH` | `/admin/routes/{id}/status` | ضبط حالة الخط أو تسعيرته يدوياً |
| `GET` | `/admin/reports?routeId&type&applied&dismissed&hours&page&size` | فلترة البلاغات مع صفحات (FR-3.2) |
| `GET` | `/admin/reports/summary` | تجميع البلاغات حسب الخط والنوع مع عدد المبلّغين المختلفين |
| `POST` | `/admin/reports/{id}/approve` · `/reject` · `DELETE /admin/reports/{id}` | المراجعة اليدوية |

### مثال: طلب مسار

```json
POST /api/v1/routing/search
{ "origin": {"lat": 33.4361, "lng": 36.0825},
  "destination": {"lat": 33.511, "lng": 36.289},
  "peak": null }
```

يمكن بدل الإحداثيات استخدام `originStationId` / `destinationStationId` أو `originQuery` / `destinationQuery` (بحث نصي مثل `"جسر الرئيس"`).
`peak`: `true`/`false`، أو `null` لاكتشاف وقت الذروة تلقائياً من توقيت دمشق (`app.transit.peak-windows`).

```json
{
  "originName": "قطنا المركز", "destinationName": "جسر الرئيس", "peak": false,
  "paths": [{
    "totalFare": 3500.00, "totalDurationSeconds": 3224, "walkingMeters": 0.0, "transfers": 1,
    "legs": [
      { "type": "RIDE", "fromName": "قطنا المركز", "toName": "كراجات السومرية",
        "routeId": "…", "routeName": "قطنا - السومرية", "vehicleType": "SERVIS", "routeStatus": "NORMAL",
        "fare": 2000.00, "durationSeconds": 2110, "distanceMeters": 12894.9 },
      { "type": "RIDE", "fromName": "كراجات السومرية", "toName": "جسر الرئيس",
        "routeName": "السومرية - البرامكة - جسر الرئيس", "vehicleType": "BUS",
        "fare": 1500.00, "durationSeconds": 1113, "distanceMeters": 8662.5 }
    ]
  }],
  "alerts": [ { "routeName": "قطنا - السومرية", "reportType": "FARE_CHANGE", "newFare": 2500.00, "sentAt": "…" } ],
  "processingMillis": 4
}
```

---

## كيف تُحقَّق متطلبات الـ SRS

| المتطلب | التنفيذ |
|---|---|
| FR-1.1 تحديد المواقع (GPS / دبوس / بحث نصي) | `RoutingSearchRequest` يقبل الثلاثة؛ البحث النصي يطبّع العربية (همزات، تاء مربوطة، "ال") ويرتّب النتائج |
| FR-1.2 المسارات التبديلية | `MultiLegRouter`: Dijkstra على حالة (محطة، آخر خط) مع أرجل مشي، حتى 3 خطوط، و3 بدائل مختلفة |
| FR-1.3 التكلفة والوقت | التكلفة من آخر تسعيرة معتمدة للخط؛ الوقت من سرعة الوسيلة × معامل الذروة × معامل حالة الخط (ازدحام/تحويلة/تأخير) |
| FR-2.1 البلاغات | `POST /reports` بأنواع محددة مسبقاً، مع التوثيق للحد من البلاغات الوهمية |
| FR-2.2 التنبيهات | كل نتيجة بحث تحمل `alerts` بآخر بلاغات الخطوط المقترحة خلال `alert-window-hours` |
| FR-3.1 إدارة الشبكة | CRUD للمحطات والخطوط، محطات مرتبة، polyline مرسوم على الخريطة، كراجات/عقد تبديل عبر `stationKind` |
| FR-3.2 الفلترة والاعتماد الآلي | ملخص مجمّع + فلاتر + اعتماد/رفض؛ وعند تكرار نفس البلاغ من 3 مستخدمين مختلفين تُحدَّث حالة/تسعيرة الخط تلقائياً وتنتهي الحالة بعد `status-ttl-hours` |
| NFR-1.1 حزم خفيفة | gzip، لقطة شبكة واحدة مع ETag/304، خرائط OpenStreetMap |
| NFR-1.2 Offline-first | PWA مع Service Worker: غلاف التطبيق وبلاطات الخريطة ولقطة الشبكة مخزّنة محلياً؛ آخر نتيجة بحث محفوظة |
| NFR-1.3 ≤ 1.5 ث | الشبكة مبنية مرة واحدة ومخزّنة في الذاكرة (`TransitGraphFactory`)؛ `processingMillis` في كل استجابة |
| NFR-2.1 Android 7+ وAPK ≤ 25MB | تطبيق Capacitor، APK تجريبي ≈ 4.5MB |
| NFR-2.2 واجهة عربية عالية التباين | الواجهة RTL بالكامل بخط Cairo وألوان عالية التباين، مع إنجليزية اختيارية |
| NFR-3.1 بلا تسجيل دخول | البحث والتصفح عامان؛ التوثيق فقط للبلاغات ولوحة التحكم |
| NFR-3.2 HTTPS | Caddy مع شهادات Let's Encrypt تلقائية (`docker compose --profile tls`) |
| NFR-4.1 التوسع الجغرافي | حقل `governorate` على المحطات والخطوط؛ الـ SRS يذكر PostGIS، وتم اعتماد MongoDB بطلب المالك |

---

## الإعدادات (`application.properties`)

| المفتاح | الافتراضي | الوصف |
|---|---|---|
| `MONGO_URI` / `MONGO_DB` | **إلزامي** / `syrian_transit` | الاتصال بقاعدة البيانات |
| `JWT_SECRET` | **إلزامي** | سلسلة عشوائية طويلة (32+ حرفاً) |
| `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` | `admin@example.com` / **إلزامي** | حساب المشرف الأول |
| `SEED_USER_PASSWORD` | غير معرّف | إن عُرّف تُنشأ حسابات ركاب تجريبية |
| `CORS_ORIGINS` | `http://localhost:5173` | أصول الواجهة المسموح بها |
| `app.transit.walk-radius-meters` | 750 | أقصى مسافة مشي للوصول لموقف |
| `app.transit.max-ride-legs` / `alternative-paths` | 3 / 3 | حد الخطوط في الرحلة / عدد البدائل |
| `app.transit.peak-windows` | `07:00-09:30,13:00-16:00` | نوافذ الذروة بتوقيت دمشق |
| `app.transit.auto-apply-threshold` | 3 | عدد المبلّغين المختلفين للاعتماد الآلي |
| `app.transit.alert-window-hours` / `status-ttl-hours` | 3 / 6 | نافذة التنبيهات / مدة بقاء الحالة الآلية |
| `app.limits.login-attempts` / `login-window-minutes` | 10 / 15 | حد محاولات الدخول لكل بريد |
| `app.limits.report-cooldown-minutes` / `reports-per-hour` | 10 / 20 | حدود البلاغات لكل مستخدم |

---

## الحقوق (Copyright)

© 2026 **Tony Botros** و **Rana Almer'ey**. جميع الحقوق محفوظة.

هذا المشروع وشيفرته المصدرية وتصميمه ووثائقه ملك لأصحابه. لا يجوز نسخه أو تعديله أو توزيعه أو تشغيله تجارياً أو استخدامه بأي شكل دون إذن كتابي مسبق من أصحاب الحقوق. انظر ملف [LICENSE](LICENSE).

Copyright © 2026 Tony Botros and Rana Almer'ey. All rights reserved. See [LICENSE](LICENSE).
