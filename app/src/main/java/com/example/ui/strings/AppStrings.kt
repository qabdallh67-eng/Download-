package com.example.ui.strings

data class AppStrings(
    val isArabic: Boolean
) {
    val appNameAr = "عبدالله أبو قاعود"
    val appNameEn = "Abdullah Abu Qaoud"

    // Navigation Tabs
    val navHome = if (isArabic) "الرئيسية" else "Home"
    val navDownloads = if (isArabic) "التحميلات" else "Downloads"
    val navFiles = if (isArabic) "الملفات" else "Files"
    val navFavorites = if (isArabic) "المفضلة" else "Favorites"
    val navSettings = if (isArabic) "الإعدادات" else "Settings"

    // Home Screen
    val welcomeBadge = if (isArabic) "تحميل الفيديوهات من جميع المنصات" else "Professional Media Downloader"
    val welcomeMessage = if (isArabic)
        "أهلاً بك! الصق رابط فيديو مباشر أو شارك من أي تطبيق للبدء فوراً"
    else
        "Welcome! Paste a video link or share from any app to start instantly"
    val urlHint = if (isArabic) "الصق رابط الفيديو أو الصوت هنا..." else "Paste video or audio URL here..."
    val pasteUrl = if (isArabic) "لصق الرابط" else "Paste URL"
    val analyzeUrl = if (isArabic) "تحليل الرابط" else "Analyze URL"
    val analyzing = if (isArabic) "جاري فحص الرابط والمنصة..." else "Inspecting link & source..."
    val quickShareTitle = if (isArabic) "مشاركة سريعة بدون مغادرة التطبيق" else "Quick Share Without Leaving App"
    val quickShareDesc = if (isArabic)
        "اضغط «مشاركة» داخل أي تطبيق ← اختر «عبدالله أبو قاعود» لتظهر نافذة التنزيل المدمجة فوراً."
    else
        "Tap Share in any app → Choose Abdullah Abu Qaoud to open the compact download popup."
    val supportedPlatformsTitle = if (isArabic) "أو اختر المنصة مباشرة؟" else "Supported Platforms"
    val recentDownloadsTitle = if (isArabic) "التحميلات الأخيرة" else "Recent Downloads"
    val sampleLinksTitle = if (isArabic) "روابط عامة سريعة للتجربة المباشرة" else "Public Sample Streams (Quick Test)"
    val noRecentDownloads = if (isArabic) "لا توجد تحميلات حديثة بعد — ابدأ بتحليل رابط أعلاه." else "No recent downloads yet — analyze a URL above."

    // Quick Share Popup
    val whatToDownload = if (isArabic) "ماذا تريد تنزيله؟" else "What would you like to download?"
    val videoOption = if (isArabic) "فيديو" else "Video"
    val audioOnlyOption = if (isArabic) "صوت فقط" else "Audio Only"
    val selectQualityTitle = if (isArabic) "اختيار الجودة (الأعلى جودة متاحة مجاناً)" else "Select Quality (Highest Available Free)"
    val highestAvailableBadge = if (isArabic) "أعلى جودة متاحة مجاناً" else "Highest Available Free"
    val selectAudioFormatTitle = if (isArabic) "اختر صيغة وجودة الصوت" else "Choose Audio Format & Quality"
    val startDownload = if (isArabic) "بدء التنزيل" else "Start Download"
    val closeLabel = if (isArabic) "إغلاق" else "Close"
    val fromPlatformPrefix = if (isArabic) "من" else "From"
    val downloadStartedToast = if (isArabic) "بدأ التنزيل في الخلفية..." else "Download started in background..."
    val viewInDownloads = if (isArabic) "عرض في التحميلات" else "View Downloads"
    val aiNoticeBanner = if (isArabic)
        "ملاحظة: مساعد الذكاء الاصطناعي يعمل بالوضع التجريبي لأن Firebase غير مفعّل."
    else
        "Note: AI insights are in sample mode because Firebase is not enabled."

    // Downloads Screen
    val activeDownloads = if (isArabic) "جاري التحميل" else "Active"
    val completedDownloads = if (isArabic) "مكتملة" else "Completed"
    val allDownloads = if (isArabic) "الكل" else "All"
    val pause = if (isArabic) "إيقاف مؤقت" else "Pause"
    val resume = if (isArabic) "استئناف" else "Resume"
    val cancel = if (isArabic) "إلغاء" else "Cancel"
    val retry = if (isArabic) "إعادة المحاولة" else "Retry"
    val downloadSuccess = if (isArabic) "تم التحميل بنجاح" else "Download Complete"
    val savedIn = if (isArabic) "تم الحفظ في:" else "Saved in:"
    val clearHistory = if (isArabic) "مسح السجل" else "Clear History"
    val emptyDownloads = if (isArabic) "قائمة التحميلات فارغة حالياً." else "Your download queue is empty."

    // File Manager
    val catVideos = if (isArabic) "الفيديوهات" else "Videos"
    val catAudio = if (isArabic) "الملفات الصوتية" else "Audio Files"
    val catFavorites = if (isArabic) "المفضلة" else "Favorites"
    val catRecent = if (isArabic) "التحميلات الأخيرة" else "Recent"
    val searchFilesHint = if (isArabic) "ابحث في الملفات المحفوظة..." else "Search saved files..."
    val selectFolder = if (isArabic) "اختيار مجلد الحفظ" else "Select Storage Folder"
    val sortNewest = if (isArabic) "الأحدث" else "Newest"
    val sortName = if (isArabic) "الاسم" else "Name"
    val sortSize = if (isArabic) "الأكبر حجماً" else "Largest"
    val play = if (isArabic) "تشغيل" else "Play"
    val share = if (isArabic) "مشاركة" else "Share"
    val rename = if (isArabic) "إعادة تسمية" else "Rename"
    val delete = if (isArabic) "حذف" else "Delete"
    val details = if (isArabic) "التفاصيل" else "Details"

    // Settings
    val languageSetting = if (isArabic) "اللغة" else "Language"
    val themeSetting = if (isArabic) "المظهر" else "Appearance"
    val darkModeLabel = if (isArabic) "الوضع الداكن" else "Dark Mode"
    val lightModeLabel = if (isArabic) "الوضع الفاتح" else "Light Mode"
    val defaultVideoQuality = if (isArabic) "جودة الفيديو الافتراضية" else "Default Video Quality"
    val defaultAudioFormat = if (isArabic) "صيغة الصوت الافتراضية" else "Default Audio Format"
    val downloadFolder = if (isArabic) "مجلد التنزيل" else "Download Folder"
    val wifiOnly = if (isArabic) "التنزيل عبر Wi-Fi فقط" else "Wi-Fi Only Downloads"
    val notifications = if (isArabic) "الإشعارات" else "Notifications"
    val storageManagement = if (isArabic) "إدارة التخزين" else "Storage Management"
    val clearTempCache = if (isArabic) "تنظيف الملفات المؤقتة" else "Clean Temporary Files"
    val privacyPolicy = if (isArabic) "سياسة الخصوصية والأمان" else "Privacy & Security Policy"
    val aboutDeveloper = if (isArabic) "عن عبدالله أبو قاعود" else "About Abdullah Abu Qaoud"
}
