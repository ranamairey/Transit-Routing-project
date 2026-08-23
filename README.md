# 🚌 Transit Routing & Public Transport System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

نظام مواصلات ذكي وديناميكي مبني كـ RESTful Backend Service لتحديد وحساب أفضل مسارات النقل العام (سرافيس وحافلات) بناءً على الإحداثيات الجغرافية، مع أخذ أوقات المشي، التبديلات، وبلاغات الازدحام اللحظية بعين الاعتبار.

---

## 🌟 الميزات الأساسية (Key Features)

* **خوارزمية حساب المسارات (Routing Algorithm):** البحث عن أقصر وأسرع رحلة بين نقطتين الجغرافيتين ($Latitude, Longitude$) باستخدام أوزان متغيرة.
* **الربط الجغرافي للشبكة (Transit Network Graph):** إدارة المحطات (`Stations`)، الخطوط (`Routes`)، والتسلسل الجغرافي مع التوقيت والمسافات (`Route Stations`).
* **نظام البلاغات التشاركي (Crowdsourced Alerts):** استقبال بلاغات الحوادث والازدحام من المستخدمين وتعديل أوزان المسارات أوتوماتيكياً لترشيح بدائل أسرع.
* **الأمان والتوثيق (Security & Authentication):** حماية الخدمات والأدوار (Admin / User) باستخدام **Spring Security** و **JWT Tokens**.
* **دعم الذكاء الاصطناعي (AI Integration):** الربط مع **Spring AI** لتقديم توصيات رحلات ذكية.

---

## 🛠️ التقنيات المستخدمة (Tech Stack)

* **Backend Framework:** Spring Boot 3
* **Security:** Spring Security, JWT (JSON Web Tokens)
* **Database:** MySQL / Hibernate (JPA)
* **Data Access & Validation:** Jakarta Validation, Spring Data JPA
* **AI:** Spring AI (Anthropic Integration)
* **Tools & Testing:** Postman Collection, Git/GitHub

---

## 📐 معمارية قاعدة البيانات (Database Architecture)

يتكون النظام من الكيانات الجغرافية والخدمية التالية:
1. `stations`: تخزن بيانات المحطات الجغرافية ونوعها.
2. `routes`: تخزن خطوط النقل العامة (سرفيس / باص).
3. `route_stations`: جدول الربط والتسلسل الجغرافي وتوقيت التنقل بين المحطات.
4. `crowdsourced_reports`: تخزن بلاغات الازدحامات والحوادث الحية.
5. `users`: تخزن بيانات الحسابات والصلاحيات المشفرة.

---

## ⚙️ التثبيت التشغيل المحلي (Setup & Installation)

### المتطلبات الأساسية
* Java 17 or higher
* MySQL Server (XAMPP / MySQL Workbench)
* Maven

### خطوات التشغيل
1. قم بإنشاء قاعدة بيانات جديدة في MySQL باسم `sprimg`:
   ```sql
   CREATE DATABASE Transit Routing;