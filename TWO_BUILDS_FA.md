# دو نوع Build برنامه تعزیه و شبیه‌خوانی

این پروژه اکنون دو Product Flavor مستقل دارد:

- `admin`: نسخه مدیر، Application ID = `com.example.bookapp`
- `viewer`: نسخه کاربر، Application ID = `com.example.bookapp.viewer`

## خروجی GitHub Actions

Workflow دو APK می‌سازد:

- `app-admin-debug.apk` — نسخه مدیر
- `app-viewer-debug.apk` — نسخه کاربر فقط مشاهده

## نسخه کاربر

در Viewer منوی مدیریت محتوا و بروزرسانی محتوای آنلاین غیرفعال/مخفی است و اجرای مسیر مدیریت محتوا نیز از Navigation محافظت شده است. همچنین `FLAG_SECURE` برای جلوگیری از Screenshot/Screen Recording فعال است و Backup سیستم‌عامل در Manifest خاموش است.

این محافظت UI و سطح دسترسی داخل APK است و در برابر Root یا مهندسی معکوس تضمین ۱۰۰٪ برای مخفی ماندن محتوای خام ایجاد نمی‌کند.
