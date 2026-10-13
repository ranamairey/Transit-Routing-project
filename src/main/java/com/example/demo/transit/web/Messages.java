package com.example.demo.transit.web;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * API messages are written in Arabic at the point of use; this maps them to English for clients
 * that send Accept-Language: en. Prefix entries (ending with ": " or " «") keep their suffix.
 */
public final class Messages {

    private static final Map<String, String> EN = new LinkedHashMap<>();

    static {
        EN.put("البريد الإلكتروني أو كلمة السر غير صحيحة", "Incorrect email or password");
        EN.put("بيانات الدخول غير صحيحة", "Incorrect sign-in details");
        EN.put("البريد الإلكتروني مستخدم بالفعل", "This email is already registered");
        EN.put("البلاغ غير موجود", "Report not found");
        EN.put("الخط غير موجود", "Line not found");
        EN.put("المحطة غير موجودة", "Station not found");
        EN.put("المستخدم غير موجود", "User not found");
        EN.put("الخط يحتاج محطتين على الأقل", "A line needs at least two stops");
        EN.put("الخط يحتاج محطتين مختلفتين على الأقل", "A line needs at least two different stops");
        EN.put("إرسال البلاغ يتطلب توثيقاً وتسجيل دخول", "Submitting a report requires signing in");
        EN.put("هذه العملية تتطلب تسجيل الدخول", "This action requires signing in");
        EN.put("بلاغ تغيير التسعيرة يتطلب تحديد التسعيرة الجديدة", "A fare-change report must include the new fare");
        EN.put("تجاوزت الحد المسموح من البلاغات لهذه الساعة", "You have reached the hourly limit of reports");
        EN.put("تعذر تنفيذ العملية: السجل موجود مسبقاً (قيمة مكررة)", "Could not complete the operation: the record already exists (duplicate value)");
        EN.put("حدث خطأ غير متوقع في السيرفر", "An unexpected server error occurred");
        EN.put("حدد نقطة البداية/الوجهة عبر GPS أو دبوس أو بحث نصي", "Set the origin and destination by GPS, a map pin or a text search");
        EN.put("طلب غير صالح", "Invalid request");
        EN.put("طلب غير صالح: تحقق من شكل البيانات المرسلة", "Invalid request: check the submitted data");
        EN.put("لا يمكنك تغيير صلاحياتك الخاصة", "You cannot change your own role");
        EN.put("لا يمكنك حذف حسابك الخاص", "You cannot delete your own account");
        EN.put("لا يوجد مسار تبديلي ضمن نطاق المشي المحدد", "No route found within the walking range");
        EN.put("لم يُعثر على مسار بين النقطتين ضمن حدود المشي والتبديلات المسموحة", "No route found between these points within the walking and transfer limits");
        EN.put("ليس لديك الصلاحيات الكافية لتنفيذ هذا الإجراء", "You do not have permission to perform this action");
        EN.put("نقطة الانطلاق والوجهة متطابقتان", "Origin and destination are the same");
        EN.put("يجب أن يبقى مشرف واحد على الأقل", "At least one admin must remain");
        EN.put("رابط غير صالح أو منتهي الصلاحية", "This link is invalid or has expired");
        EN.put("تم تأكيد البريد الإلكتروني مسبقاً", "This email is already verified");
        EN.put("يجب تأكيد البريد الإلكتروني قبل إرسال البلاغات", "Please verify your email before sending reports");
        EN.put("طلبات كثيرة، حاول لاحقاً", "Too many requests, please try again later");
        // prefixes
        EN.put("لا يمكن حذف المحطة لأنها مستخدمة في الخطوط: ", "The station cannot be deleted because it is used by these lines: ");
        EN.put("لم يُعثر على الموقع: ", "Location not found: ");
        EN.put("محطة غير موجودة: ", "Station not found: ");
        EN.put("المستخدم غير موجود: ", "User not found: ");
        EN.put("محاولات دخول كثيرة، حاول مجدداً بعد ", "Too many sign-in attempts, try again in ");
        EN.put("أرسلت بلاغاً عن هذا الخط قبل قليل، يمكنك الإبلاغ مجدداً بعد ", "You reported this line a moment ago; you can report again in ");
    }

    private Messages() {
    }

    public static boolean wantsEnglish(String acceptLanguage) {
        return acceptLanguage != null && acceptLanguage.trim().toLowerCase(Locale.ROOT).startsWith("en");
    }

    public static String translate(String arabic, String acceptLanguage) {
        if (arabic == null || !wantsEnglish(acceptLanguage)) {
            return arabic;
        }
        String exact = EN.get(arabic);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, String> e : EN.entrySet()) {
            if (e.getKey().endsWith(" ") && arabic.startsWith(e.getKey())) {
                String rest = arabic.substring(e.getKey().length());
                return e.getValue() + rest.replace(" دقيقة", " min");
            }
        }
        return arabic;
    }
}
