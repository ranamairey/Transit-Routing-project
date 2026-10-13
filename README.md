# 🚌 Smart Syrian Public Transportation System — Transit Routing

نظام ملاحة ذكي لشبكة النقل العام في سوريا (سرافيس وباصات): يقترح مسارات تبديلية بين نقطتين بأقل تكلفة ووقت، ويحدّث حالة الخطوط من بلاغات الركاب التشاركية.
مبني وفق وثيقة متطلبات البرمجيات (SRS) المرفقة في `docs/`.

| الجزء | التقنية |
|---|---|
| Backend API | Spring Boot 3.2 · Java 17 · Spring Security + JWT · Spring Data MongoDB |
| Database | MongoDB (Atlas أو محلي) — قاعدة `syrian_transit` |
| Frontend | React 19 · Vite · React Router · Leaflet + OpenStreetMap |
| Tests | JUnit 5 · MockMvc · MongoDB مدمج (flapdoodle) |

---

## التشغيل السريع

### 1) Backend

```bash
cp .env.example .env        # ثم ضع MONGO_URI و MONGO_DB=syrian_transit
./run.sh                    # يقرأ .env ويشغّل spring-boot:run على المنفذ 8080
```

أو بدون السكربت:

```bash
MONGO_URI='mongodb+srv://<user>:<pass>@<cluster>/' MONGO_DB=syrian_transit ./mvnw spring-boot:run
```

عند أول تشغيل على قاعدة فارغة يُزرع تلقائياً (`DataSeeder`): شبكة تجريبية لدمشق وريفها (12 محطة، 7 خطوط) وحسابات:

| الدور | البريد | كلمة السر |
|---|---|---|
| مشرف | `admin@demo.com` | `Admin#12345` |
| مستخدم | `ahmad@demo.com` / `samer@demo.com` | `User#12345` |

غيّر بيانات المشرف عبر `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD`، أو عطّل الزرع بـ `SEED_ENABLED=false`.

### 2) Frontend

```bash
cd frontend
npm install
npm run dev                 # http://localhost:5173 (يُمرّر /api إلى 8080)
```

للإنتاج: `npm run build` ثم انشر `frontend/dist` خلف نفس النطاق أو اضبط `VITE_API_BASE`.

### 3) الاختبارات

```bash
./mvnw test
```

تشمل اختبارات وحدة لخوارزمية المسارات، واختباراً تكاملياً كاملاً للـ API على قاعدة MongoDB مدمجة (يُحمَّل ملف `mongod` مرة واحدة).

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
frontend/src
├── pages/       PlannerPage (الخريطة والبحث) · Login · Register · admin/{Reports,Stations,Routes}
├── components/  MapView (Leaflet) · EndpointPicker · TripOptions · AlertsBar · ReportDialog
└── hooks/useNetwork.js   كاش الشبكة محلياً (Offline-first) مع ETag
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
| `POST` | `/auth/register` · `/auth/login` · `GET /auth/me` | التوثيق (JWT Bearer) |

### تتطلب تسجيل دخول

| الطريقة | المسار | الوصف |
|---|---|---|
| `POST` | `/reports` | إرسال بلاغ (FR-2.1): `{reportType, routeId, stationId?, newFare?, description?, location?}` |

### المشرف فقط (`ROLE_ADMIN`)

| الطريقة | المسار | الوصف |
|---|---|---|
| `POST/PUT/DELETE` | `/admin/stations[/{id}]` | إدارة المحطات (FR-3.1) |
| `POST/PUT/DELETE` | `/admin/routes[/{id}]` | إدارة الخطوط مع المحطات بالترتيب والـ polyline |
| `PATCH` | `/admin/routes/{id}/status` | ضبط حالة الخط أو تسعيرته يدوياً |
| `GET` | `/admin/reports?routeId&type&applied&dismissed&hours` | فلترة البلاغات (FR-3.2) |
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
| NFR-1.2 Offline-first | الواجهة تحفظ الشبكة وآخر نتيجة محلياً وتعمل عليها دون اتصال |
| NFR-1.3 ≤ 1.5 ث | الشبكة مبنية مرة واحدة ومخزّنة في الذاكرة (`TransitGraphFactory`)؛ `processingMillis` في كل استجابة |
| NFR-2.2 واجهة عربية عالية التباين | الواجهة RTL بالكامل بألوان عالية التباين |
| NFR-3.1 بلا تسجيل دخول | البحث والتصفح عامان؛ التوثيق فقط للبلاغات ولوحة التحكم |
| NFR-3.2 HTTPS | يُفعَّل على طبقة النشر (reverse proxy)؛ الـ API لا يعتمد على جلسات |
| NFR-4.1 التوسع الجغرافي | حقل `governorate` على المحطات والخطوط؛ الـ SRS يذكر PostGIS، وتم اعتماد MongoDB بطلب المالك |

---

## الإعدادات (`application.properties`)

| المفتاح | الافتراضي | الوصف |
|---|---|---|
| `MONGO_URI` / `MONGO_DB` | `mongodb://localhost:27017` / `syrian_transit` | الاتصال بقاعدة البيانات |
| `JWT_SECRET` | قيمة تجريبية | غيّره في الإنتاج |
| `CORS_ORIGINS` | `http://localhost:5173` | أصول الواجهة المسموح بها |
| `app.transit.walk-radius-meters` | 750 | أقصى مسافة مشي للوصول لموقف |
| `app.transit.max-ride-legs` / `alternative-paths` | 3 / 3 | حد الخطوط في الرحلة / عدد البدائل |
| `app.transit.peak-windows` | `07:00-09:30,13:00-16:00` | نوافذ الذروة بتوقيت دمشق |
| `app.transit.auto-apply-threshold` | 3 | عدد المبلّغين المختلفين للاعتماد الآلي |
| `app.transit.alert-window-hours` / `status-ttl-hours` | 3 / 6 | نافذة التنبيهات / مدة بقاء الحالة الآلية |
