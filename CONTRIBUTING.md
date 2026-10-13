# دليل التطوير والمساهمة

هذا الدليل يصف بناء Max Share Android وتعديل مصدره بطريقة قابلة للتكرار. التطبيق Android أصلي بلغة Kotlin؛ صفحة المتصفح مضمّنة كسلسلة HTML داخل `WebUi.kt` وليست مشروع واجهة منفصلاً.

## المتطلبات

- Android Studio بإصدار يدعم Android Gradle Plugin 8.5.2.
- JDK 17.
- Android SDK Platform 34 وAndroid Build Tools المناسبة.
- Gradle 8.7 أو أحدث عند البناء من سطر الأوامر. لا يحتوي المصدر المرفق على Gradle Wrapper.

## إعداد نسخة العمل

1. استنسخ المستودع وافتح مجلده الجذري في Android Studio.
2. ثبّت Android SDK Platform 34 عند طلب Android Studio ذلك.
3. انتظر مزامنة Gradle، ثم شغّل التطبيق على جهاز حقيقي أو محاكي يدعم Wi‑Fi Direct. قد لا تعمل ميزات الشبكة الفعلية على المحاكي.
4. للبناء من الطرفية في جذر المشروع:

   ```sh
   gradle :app:assembleDebug
   ```

5. يوجد APK الاختبار في `app/build/outputs/apk/debug/app-debug.apk`.

لا ترفع `local.properties` أو مفاتيح التوقيع أو ملفات البناء إلى Git.

## خريطة المصدر

| المسار | المسؤولية |
| --- | --- |
| `app/src/main/java/com/portablefileserver/ui/MainActivity.kt` | واجهة الهاتف، الإعدادات وطلبات أذونات Android |
| `app/src/main/java/com/portablefileserver/server/ServerService.kt` | خدمة foreground ودورة تشغيل نقطة المشاركة والخادم |
| `app/src/main/java/com/portablefileserver/server/HotspotManager.kt` | Wi‑Fi Direct والمسار الاحتياطي إلى LocalOnlyHotspot |
| `app/src/main/java/com/portablefileserver/server/MyHttpServer.kt` | التوجيه المحلي ونقاط HTTP وعمليات الملفات |
| `app/src/main/java/com/portablefileserver/server/SafePathResolver.kt` | حصر المسارات ضمن المجلد ومنع اجتياز الروابط الرمزية |
| `app/src/main/java/com/portablefileserver/server/WebUi.kt` | HTML/CSS/JavaScript للمتصفح |
| `app/src/main/java/com/portablefileserver/server/ServerRuntime.kt` | حالة الخدمة ومعلومات الشبكة وإحصاءات النقل |
| `app/src/main/java/com/portablefileserver/server/ApkIconInspector.kt` | قراءة بيانات APK للعرض فقط |
| `app/src/main/java/com/portablefileserver/data/PrefsManager.kt` | إعدادات الجهاز المحفوظة محلياً |
| `app/src/main/java/com/portablefileserver/data/AppIdentity.kt` | معلومات الاسم والدعم التي تظهر في التطبيق |
| `app/src/main/assets/app-icons/` | كتالوج SVG وإشعارات المصدر والترخيص |

## طريقة العمل

- احتفظ بمسارات HTTP وواجهة المتصفح متوافقة؛ راجع `docs/HTTP_API.md` عند تغيير أي نقطة نهاية.
- اختبر قرارات الأذونات على إصدارات Android المناسبة؛ تختلف متطلبات Wi‑Fi وملفات التخزين حسب API level.
- لا تستخدم `File` أو مساراً قادماً من المتصفح دون تمريره عبر حل المسارات الآمن في `MyHttpServer`.
- لا تتبع الروابط الرمزية في التصفح أو ZIP. إذا تغيّر دعم الملفات المخفية، حدّث التصفح والحزم معاً.
- لا تضف مفاتيح أو حسابات خاصة أو بيانات مستخدم إلى السجلات أو ملفات الاختبار. بعض معلومات الدعم جزء مقصود من واجهة التطبيق.
- تنسيق Kotlin رسمي، وملفات Gradle تستخدم Kotlin DSL. صفحة الويب JavaScript عادي ومضمّنة في Kotlin؛ تحقّق من صياغتها عند تعديلها.

## الفحوصات قبل طلب دمج التغييرات

- شغّل `gradle :app:assembleDebug`.
- شغّل `gradle :app:testDebugUnitTest` لاختبارات محلّل المسارات.
- شغّل Android Lint من Android Studio أو `gradle :app:lintDebug`، وسجّل إن تعذّر تشغيله.
- نفّذ حالات الدليل في `docs/TESTING.md` على جهاز فعلي قبل تغيير سلوك Wi‑Fi أو الملفات.
- راجع `docs/RELEASING.md` قبل تعديل رقم الإصدار أو إنشاء APK موقّع.

سير العمل الحالي في GitHub يبني نسخة debug ويرفعها كـ workflow artifact. نجاح البناء لا يثبت عمل Wi‑Fi Direct على كل طراز، ولا يوقّع APK للنشر.

## الملكية والترخيص

لم يتضمن المصدر المرفق ترخيصاً للمشروع ككل؛ لا تفترض أن نشر المستودع العام يمنح إذناً بإعادة توزيع التطبيق. تحقّق من إشعارات الأيقونات في `app/src/main/assets/app-icons/` ومن حقوق العلامات التجارية قبل إعادة استخدامها.
