package com.portablefileserver.ui

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.portablefileserver.data.PrefsManager
import com.portablefileserver.qr.QrGenerator
import com.portablefileserver.server.ServerRuntime
import com.portablefileserver.server.ServerService
import java.io.File
import java.security.SecureRandom
import java.util.Locale

class MainActivity : Activity() {

    // Max Share design tokens follow the phone's light/dark appearance.
    private val isDarkMode: Boolean
        get() = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    private val cBg: Int get() = if (isDarkMode) 0xFF10191B.toInt() else 0xFFF4F7F7.toInt()
    private val cCard: Int get() = if (isDarkMode) 0xFF182426.toInt() else 0xFFFFFFFF.toInt()
    private val cField: Int get() = if (isDarkMode) 0xFF202E30.toInt() else 0xFFEDF3F2.toInt()
    private val cText: Int get() = if (isDarkMode) 0xFFEFF7F6.toInt() else 0xFF15272B.toInt()
    private val cSub: Int get() = if (isDarkMode) 0xFFA5B6B5.toInt() else 0xFF607276.toInt()
    private val cPrimary: Int get() = if (isDarkMode) 0xFF74C9BD.toInt() else 0xFF137B77.toInt()
    private val cOk = 0xFF187B5B.toInt()
    private val cWarn = 0xFFA36613.toInt()
    private val cErr = 0xFFB83D49.toInt()
    private val cBorder: Int get() = if (isDarkMode) 0xFF2B3B3D.toInt() else 0xFFDCE6E4.toInt()

    private lateinit var prefs: PrefsManager
    private val ui = Handler(Looper.getMainLooper())

    private lateinit var etName: EditText
    private lateinit var etPass: EditText
    private lateinit var etPort: EditText
    private lateinit var etPath: EditText
    private lateinit var swUpload: Switch
    private lateinit var swDelete: Switch
    private lateinit var swScreen: Switch
    private lateinit var btnMain: ServerSwipeControl
    private lateinit var pill: TextView
    private lateinit var tvStatusMsg: TextView
    private lateinit var runCard: LinearLayout
    private lateinit var ivQr: ImageView
    private lateinit var btnQrMode: Button
    private lateinit var tvSsid: TextView
    private lateinit var tvPass: TextView
    private lateinit var tvUrl: TextView
    private lateinit var tvStats: TextView
    private lateinit var tvSentRate: TextView
    private lateinit var tvReceivedRate: TextView
    private lateinit var tvSentTotal: TextView
    private lateinit var tvReceivedTotal: TextView
    private lateinit var tvMode: TextView
    private lateinit var settingsViews: List<View>
    private lateinit var homeScroll: ScrollView
    private lateinit var shareScroll: ScrollView
    private lateinit var statsScroll: ScrollView
    private lateinit var settingsScroll: ScrollView
    private lateinit var aboutScroll: ScrollView
    private lateinit var extraScroll: ScrollView
    private lateinit var extraContent: LinearLayout
    private lateinit var shareIdleCard: View
    private lateinit var drawerOverlay: FrameLayout
    private lateinit var homeTabItem: TextView
    private lateinit var shareTabItem: TextView
    private lateinit var statsTabItem: TextView
    private lateinit var settingsTabItem: TextView
    private var selectedTab = 0
    private var lastSampleAt = 0L
    private var lastSentBytes = 0L
    private var lastReceivedBytes = 0L

    private var qrWifi = true
    private var lastQrKey: String? = null

    private val listener: () -> Unit = { refresh() }
    private val ticker = object : Runnable {
        override fun run() {
            refreshStats()
            ui.postDelayed(this, 1000)
        }
    }

    // ---------- أدوات بناء الواجهة ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
        setStroke(dp(1), cBorder)
    }

    private fun lp(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(top); bottomMargin = dp(bottom) }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            setColor(cCard)
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), cBorder)
        }
        setPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = lp(bottom = 12)
    }

    private fun heading(t: String) = TextView(this).apply {
        text = t
        textSize = 16f
        setTextColor(cText)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, dp(8))
    }

    private fun small(t: String) = TextView(this).apply {
        text = t
        textSize = 12f
        setTextColor(cSub)
    }

    private fun buttonShape(color: Int, filled: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            setColor(if (filled) color else cField)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), if (filled) color else cBorder)
        }
    }

    private fun button(t: String, color: Int, filled: Boolean = true, onClick: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        textSize = 14f
        setTextColor(if (filled) {
            if (isDarkMode) 0xFF103330.toInt() else 0xFFFFFFFF.toInt()
        } else color)
        letterSpacing = 0.01f
        background = RippleDrawable(
            ColorStateList.valueOf(0x44FFFFFF),
            buttonShape(color, filled),
            buttonShape(0xFFFFFFFF.toInt(), true)
        )
        backgroundTintList = null
        setPadding(dp(14), dp(8), dp(14), dp(8))
        minHeight = dp(44)
        stateListAnimator = null
        elevation = 0f
        setOnClickListener { onClick() }
    }

    private fun metric(title: String, value: TextView, total: TextView): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cField, 14)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            addView(small(title))
            value.textSize = 19f
            value.setTextColor(cText)
            value.typeface = android.graphics.Typeface.DEFAULT_BOLD
            addView(value, lp(top = 4))
            total.textSize = 11f
            total.setTextColor(cSub)
            addView(total, lp(top = 2))
        }

    private fun selectTab(tab: Int) {
        val active = tab.coerceIn(0, 3)
        listOf(homeScroll, shareScroll, statsScroll, settingsScroll).forEachIndexed { index, page ->
            page.visibility = if (index == active) View.VISIBLE else View.GONE
        }
        aboutScroll.visibility = View.GONE
        extraScroll.visibility = View.GONE
        selectedTab = active
        listOf(homeTabItem, shareTabItem, statsTabItem, settingsTabItem).forEachIndexed { index, item ->
            val selected = index == active
            item.setTextColor(if (selected) cPrimary else cSub)
            item.background = if (selected) rounded(cField, 12) else null
        }
        if (::drawerOverlay.isInitialized) drawerOverlay.visibility = View.GONE
    }

    private fun showSecondaryPage(page: ScrollView) {
        listOf(homeScroll, shareScroll, statsScroll, settingsScroll, aboutScroll, extraScroll)
            .forEach { it.visibility = View.GONE }
        page.visibility = View.VISIBLE
        selectedTab = -1
        listOf(homeTabItem, shareTabItem, statsTabItem, settingsTabItem).forEach { item ->
            item.setTextColor(cSub)
            item.background = null
        }
        if (::drawerOverlay.isInitialized) drawerOverlay.visibility = View.GONE
    }

    private fun showExtraPage(title: String, content: LinearLayout.() -> Unit) {
        extraContent.removeAllViews()
        extraContent.addView(heading(title), lp(bottom = 8))
        extraContent.content()
        showSecondaryPage(extraScroll)
    }

    private fun infoCard(title: String, body: String): LinearLayout = card().apply {
        addView(heading(title))
        addView(TextView(this@MainActivity).apply {
            text = body
            textSize = 14f
            setTextColor(cSub)
            setLineSpacing(dp(3).toFloat(), 1f)
        })
    }

    private fun createDrawer(): FrameLayout {
        val layer = FrameLayout(this).apply {
            visibility = View.GONE
            isClickable = true
            contentDescription = "القائمة الجانبية"
        }
        layer.addView(View(this).apply {
            setBackgroundColor(0x99000000.toInt())
            setOnClickListener { layer.visibility = View.GONE }
        }, FrameLayout.LayoutParams(-1, -1))

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(18), dp(18), dp(12))
            setBackgroundColor(cBg)
            isClickable = true
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val icon = ImageView(this).apply {
            setImageResource(com.portablefileserver.R.drawable.max_share_icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(cField, 14)
            clipToOutline = true
            contentDescription = "أيقونة Max Share"
        }
        header.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(10) })
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = "Max Share"
                textSize = 17f
                setTextColor(cText)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            addView(small("القائمة الرئيسية"))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(button("إغلاق", cPrimary, false) { layer.visibility = View.GONE })
        panel.addView(header, lp(bottom = 16))

        val menuScroll = ScrollView(this)
        val menuItems = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        menuScroll.addView(menuItems)
        fun item(label: String, action: () -> Unit) {
            menuItems.addView(TextView(this).apply {
                text = label
                textSize = 14f
                setTextColor(cText)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(13), dp(14), dp(13))
                background = rounded(cCard, 13)
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }, lp(bottom = 7))
        }

        item("الرئيسية") { selectTab(0) }
        item("الشبكة والاتصال") { selectTab(1) }
        item("نشاط النقل") { selectTab(2) }
        item("الإعدادات") { selectTab(3) }
        menuItems.addView(View(this).apply { setBackgroundColor(cBorder) },
            LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(8); bottomMargin = dp(12) })
        item("حول التطبيق") { showSecondaryPage(aboutScroll) }
        item("دليل الاستخدام") {
            showExtraPage("دليل الاستخدام") {
                addView(infoCard("١. ابدأ المشاركة", "افتح الرئيسية واسحب زر التشغيل. ينشئ Max Share شبكة Wi‑Fi Direct خاصة؛ وقد يستخدم نقطة اتصال محلية كخيار احتياطي."))
                addView(infoCard("٢. وصّل الجهاز الآخر", "امسح رمز QR للشبكة أو أدخل اسم الشبكة وكلمة المرور يدوياً. عند الاتصال، امسح رمز QR للرابط أو افتح الرابط في متصفح الجهاز."))
                addView(infoCard("٣. انقل الملفات", "اختر مجلداً أو ملفاً من صفحة المتصفح. الرفع والحذف وإعادة التسمية متاحة بحسب الأذونات التي فعّلتها من إعدادات Max Share."))
                addView(infoCard("أوقف الخادم عند الانتهاء", "أوقف المشاركة من زر الرئيسية. يظل الوصول محصوراً بالشبكة المحلية ما دام الخادم يعمل."))
            }
        }
        item("الخصوصية والأمان") {
            showExtraPage("الخصوصية والأمان") {
                addView(infoCard("نقل محلي", "يستضيف الهاتف الملفات على شبكة محلية ولا يحتاج النقل إلى حساب أو خدمة تخزين سحابية. لا تشارك كلمة مرور الشبكة مع من لا تريد منحه الوصول."))
                addView(infoCard("اتصال HTTP", "الرابط المحلي يعمل عبر HTTP وليس HTTPS؛ لذلك لا تستخدمه على شبكة عامة أو غير موثوقة. أوقف الخادم بعد انتهاء النقل."))
                addView(infoCard("صلاحيات المشاركة", "يمكن للأجهزة المتصلة تنزيل الملفات المعروضة. لا تسمح بالرفع أو الحذف إلا عند الحاجة، واختر مجلد المشاركة بعناية."))
            }
        }
        item("الدعم والتبرع") { showDonationPage() }
        item("تواصل معنا") { showContactPage() }
        item("إرسال رسالة") { showMessagePage() }

        panel.addView(menuScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        layer.addView(panel, FrameLayout.LayoutParams(
            minOf(dp(340), (resources.displayMetrics.widthPixels * 0.88f).toInt()),
            -1,
            Gravity.END
        ))
        return layer
    }

    private fun showDonationPage() {
        showExtraPage("الدعم والتبرع") {
            addView(infoCard("التطبيق مجاني", "استخدام Max Share مجاني بالكامل. التبرع اختياري لدعم تطوير التطبيق."))
            val account = com.portablefileserver.data.AppIdentity.shamCashAccount
            val accountCard = card()
            accountCard.addView(heading("حساب شام كاش"))
            accountCard.addView(TextView(this@MainActivity).apply {
                text = account
                textSize = 15f
                setTextColor(cText)
                textDirection = View.TEXT_DIRECTION_LTR
                gravity = Gravity.START
                setTextIsSelectable(true)
            })
            accountCard.addView(button("نسخ رقم الحساب", cPrimary, false) { copy(account) }, lp(top = 8))
            addView(accountCard)
        }
    }

    private fun showContactPage() {
        showExtraPage("تواصل معنا") {
            addView(infoCard("اكتب رسالة", "لا يوجد بريد إلكتروني أو حساب دعم محدد داخل التطبيق. يمكنك كتابة رسالتك ثم اختيار تطبيق البريد أو المشاركة على جهازك."))
            addView(button("كتابة رسالة", cPrimary) { showMessagePage() }, lp(top = 4))
        }
    }

    private fun showMessagePage() {
        showExtraPage("إرسال رسالة") {
            addView(infoCard("اختيار قناة الإرسال", "لن تُرسل الرسالة مباشرة من Max Share. سيعرض Android التطبيقات المتاحة لتختار منها تطبيقاً وترسل الرسالة بنفسك."))
            val message = EditText(this@MainActivity).apply {
                hint = "اكتب رسالتك هنا"
                minLines = 5
                gravity = Gravity.TOP or Gravity.START
                textDirection = View.TEXT_DIRECTION_RTL
                setTextColor(cText)
                setHintTextColor(cSub)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = rounded(cField, 14)
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            addView(message, lp(bottom = 8))
            addView(button("اختيار تطبيق لإرسالها", cPrimary) {
                val text = message.text.toString().trim()
                if (text.isEmpty()) {
                    toast("اكتب الرسالة أولاً")
                } else {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "رسالة حول Max Share")
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    try {
                        startActivity(Intent.createChooser(send, "اختر تطبيق الإرسال"))
                    } catch (_: Exception) {
                        toast("لا يوجد تطبيق متاح لإرسال الرسالة")
                    }
                }
            })
        }
    }

    override fun onBackPressed() {
        if (::drawerOverlay.isInitialized && drawerOverlay.visibility == View.VISIBLE) {
            drawerOverlay.visibility = View.GONE
            return
        }
        if (selectedTab == -1 || selectedTab != 0) {
            selectTab(0)
            return
        }
        super.onBackPressed()
    }

    private fun addAboutFact(parent: LinearLayout, label: String, value: String, ltr: Boolean = false) {
        val group = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(7), 0, dp(7))
        }
        group.addView(small(label))
        group.addView(TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(cText)
            if (ltr) {
                textDirection = View.TEXT_DIRECTION_LTR
                gravity = Gravity.START
            }
        }, lp(top = 3))
        parent.addView(group)
    }

    @Suppress("DEPRECATION")
    private fun appVersionDetails(): Pair<String, Long> {
        val info = packageManager.getPackageInfo(packageName, 0)
        val code = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        return (info.versionName ?: "غير متاح") to code
    }

    private fun field(hint: String, value: String, number: Boolean = false) = EditText(this).apply {
        setText(value)
        this.hint = hint
        setSingleLine()
        textSize = 16f
        setTextColor(cText)
        setHintTextColor(cSub)
        background = rounded(cField, 10)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        if (number) inputType = InputType.TYPE_CLASS_NUMBER
    }

    private fun labeled(parent: LinearLayout, label: String, f: View) {
        parent.addView(small(label).apply { setPadding(0, dp(10), 0, dp(4)) })
        parent.addView(f, lp())
    }

    private fun switchRow(text: String, checked: Boolean) = Switch(this).apply {
        this.text = text
        isChecked = checked
        textSize = 14f
        setTextColor(cText)
        setPadding(0, dp(8), 0, dp(8))
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        thumbTintList = ColorStateList(states, intArrayOf(cOk, 0xFF8790AA.toInt()))
        trackTintList = ColorStateList(states, intArrayOf(0x773AD6A4, 0x55434D66))
    }

    /** صف معلومات: عنوان صغير + قيمة قابلة للتحديد + زر نسخ. */
    private fun infoRow(parent: LinearLayout, label: String, tv: TextView) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(small(label))
        tv.apply { textSize = 16f; setTextColor(cText); setTextIsSelectable(true) }
        col.addView(tv)
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(button("نسخ", cPrimary, false) { copy(tv.text.toString()) })
        parent.addView(row, lp())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsManager(this)

        window.statusBarColor = cBg
        window.navigationBarColor = cBg
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (isDarkMode) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        homeScroll = ScrollView(this).apply { setBackgroundColor(cBg); isFillViewport = true }
        homeScroll.addView(col)
        val shareCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        shareScroll = ScrollView(this).apply { setBackgroundColor(cBg); isFillViewport = true }
        shareScroll.addView(shareCol)
        val statsCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        statsScroll = ScrollView(this).apply { setBackgroundColor(cBg); isFillViewport = true }
        statsScroll.addView(statsCol)
        val settingsCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        settingsScroll = ScrollView(this).apply { setBackgroundColor(cBg); isFillViewport = true }
        settingsScroll.addView(settingsCol)
        val aboutCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        aboutScroll = ScrollView(this).apply { setBackgroundColor(cBg); isFillViewport = true }
        aboutScroll.addView(aboutCol)
        extraContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        extraScroll = ScrollView(this).apply { setBackgroundColor(cBg); isFillViewport = true }
        extraScroll.addView(extraContent)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cBg)
        }
        val pages = FrameLayout(this)
        pages.addView(homeScroll, FrameLayout.LayoutParams(-1, -1))
        pages.addView(shareScroll, FrameLayout.LayoutParams(-1, -1))
        pages.addView(statsScroll, FrameLayout.LayoutParams(-1, -1))
        pages.addView(settingsScroll, FrameLayout.LayoutParams(-1, -1))
        pages.addView(aboutScroll, FrameLayout.LayoutParams(-1, -1))
        pages.addView(extraScroll, FrameLayout.LayoutParams(-1, -1))
        root.addView(pages, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = rounded(cCard, 18)
        }
        homeTabItem = TextView(this).apply {
            text = "الرئيسية"
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            setOnClickListener { selectTab(0) }
        }
        shareTabItem = TextView(this).apply {
            text = "الشبكة"
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            setOnClickListener { selectTab(1) }
        }
        statsTabItem = TextView(this).apply {
            text = "النشاط"
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            setOnClickListener { selectTab(2) }
        }
        settingsTabItem = TextView(this).apply {
            text = "الإعدادات"
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            setOnClickListener { selectTab(3) }
        }
        nav.addView(homeTabItem, LinearLayout.LayoutParams(0, dp(54), 1f))
        nav.addView(shareTabItem, LinearLayout.LayoutParams(0, dp(54), 1f))
        nav.addView(statsTabItem, LinearLayout.LayoutParams(0, dp(54), 1f))
        nav.addView(settingsTabItem, LinearLayout.LayoutParams(0, dp(54), 1f))
        root.addView(nav, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(16), dp(4), dp(16), dp(10))
        })

        // رأس الصفحة + حالة
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val menuButton = ImageView(this).apply {
            setImageResource(com.portablefileserver.R.drawable.ic_menu)
            background = rounded(cField, 12)
            setPadding(dp(9), dp(9), dp(9), dp(9))
            contentDescription = "فتح القائمة الجانبية"
            isClickable = true
            isFocusable = true
            setOnClickListener { drawerOverlay.visibility = View.VISIBLE }
        }
        header.addView(menuButton, LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginEnd = dp(8) })
        val logo = ImageView(this).apply {
            setImageResource(com.portablefileserver.R.drawable.max_share_icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(cField, 16)
            clipToOutline = true
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(54), dp(54)).apply { marginEnd = dp(12) })
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(this).apply {
            text = getString(com.portablefileserver.R.string.app_name); textSize = 20f; setTextColor(cText)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        titles.addView(small("مشاركة الملفات عبر شبكة محلية"))
        header.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        pill = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }
        header.addView(pill)
        col.addView(header, lp(bottom = 12))

        // آخر انهيار (إن وجد)
        val crash = File(filesDir, "crash.log")
        if (crash.exists()) {
            val cc = card()
            cc.addView(heading("آخر انهيار مسجّل"))
            cc.addView(TextView(this).apply { text = crash.readText().take(1200); textSize = 11f; setTextColor(cErr) })
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            r.addView(button("نسخ", cPrimary, false) { copy(crash.readText()) })
            r.addView(button("إخفاء", cSub, false) { crash.delete(); cc.visibility = View.GONE })
            cc.addView(r, lp(top = 8))
            col.addView(cc)
        }

        // زر التشغيل الرئيسي + رسالة الحالة
        val main = card()
        btnMain = ServerSwipeControl(this).apply { onAction = { toggle() } }
        main.addView(btnMain, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(78)))
        tvStatusMsg = TextView(this).apply { textSize = 13f; setTextColor(cSub); gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) }
        main.addView(tvStatusMsg)
        col.addView(main)

        val shortcuts = card()
        shortcuts.addView(heading("انتقال سريع"))
        val shortcutRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        shortcutRow.addView(button("الشبكة", cPrimary, false) { selectTab(1) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        shortcutRow.addView(button("النشاط", cPrimary, false) { selectTab(2) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        shortcuts.addView(shortcutRow)
        col.addView(shortcuts)

        val metrics = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        tvSentRate = TextView(this).apply { text = "0 B/s" }
        tvSentTotal = TextView(this).apply { text = "إجمالي: 0 B" }
        tvReceivedRate = TextView(this).apply { text = "0 B/s" }
        tvReceivedTotal = TextView(this).apply { text = "إجمالي: 0 B" }
        metrics.addView(metric("تنزيل إلى الأجهزة", tvSentRate, tvSentTotal),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
        metrics.addView(metric("رفع من الأجهزة", tvReceivedRate, tvReceivedTotal),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) })
        statsCol.addView(heading("نشاط النقل"), lp(bottom = 4))
        statsCol.addView(small("الإحصاءات المباشرة للجلسة الحالية."), lp(bottom = 12))
        statsCol.addView(metrics, lp(bottom = 12))
        val statsCard = card()
        statsCard.addView(heading("حالة الطلبات والأجهزة"))
        tvStats = TextView(this).apply {
            textSize = 14f
            setTextColor(cSub)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        statsCard.addView(tvStats)
        statsCol.addView(statsCard)

        // بطاقة الاتصال (تظهر عند التشغيل)
        runCard = card().apply { visibility = View.GONE }
        shareCol.addView(heading("الشبكة والاتصال"), lp(bottom = 4))
        shareCol.addView(small("امسح رمز QR أو انسخ بيانات الشبكة لربط الجهاز الآخر."), lp(bottom = 12))
        shareIdleCard = infoCard("المشاركة متوقفة", "ابدأ الخادم من صفحة الرئيسية حتى تظهر بيانات الشبكة ورمز QR.")
        shareCol.addView(shareIdleCard)
        runCard.addView(heading("بيانات الاتصال"))
        ivQr = ImageView(this)
        runCard.addView(ivQr, LinearLayout.LayoutParams(dp(220), dp(220)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        btnQrMode = button("عرض QR الرابط", cPrimary, false) { qrWifi = !qrWifi; refresh() }
        runCard.addView(btnQrMode, lp(top = 6))
        tvSsid = TextView(this); tvPass = TextView(this); tvUrl = TextView(this)
        infoRow(runCard, "اسم الشبكة", tvSsid)
        infoRow(runCard, "كلمة المرور", tvPass)
        infoRow(runCard, "الرابط", tvUrl)
        tvMode = small("")
        runCard.addView(tvMode)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("فتح في المتصفح", cPrimary) { openUrl() }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(button("مشاركة", cPrimary, false) { shareInfo() }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        runCard.addView(actions, lp(top = 8))
        runCard.addView(small("١. اتصل بالشبكة من الجهاز الآخر (امسح QR أو أدخل الاسم والكلمة)\n٢. افتح الرابط في المتصفح").apply { setPadding(0, dp(10), 0, 0) })
        shareCol.addView(runCard)

        // الإعدادات
        settingsCol.addView(TextView(this).apply {
            text = "الإعدادات"
            textSize = 24f
            setTextColor(cText)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, lp(bottom = 4))
        settingsCol.addView(small("خصّص الشبكة ومجلدات النقل والصلاحيات."), lp(bottom = 16))
        val sc = card()
        sc.addView(heading("إعدادات الشبكة"))
        etName = field("مثال: Share", prefs.networkName)
        labeled(sc, "اسم الشبكة (يظهر DIRECT-PF- قبل الاسم، أحرف إنجليزية وأرقام)", etName)
        etPass = field("8 أحرف على الأقل", prefs.password)
        labeled(sc, "كلمة المرور", etPass)
        val passRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnRandom = button("كلمة عشوائية", cPrimary, false) { etPass.setText(randomPassword()) }
        passRow.addView(btnRandom)
        sc.addView(passRow, lp(top = 6))
        etPort = field("8080", prefs.port.toString(), true)
        labeled(sc, "المنفذ", etPort)
        etPath = field("مجلد المشاركة", prefs.rootPath)
        labeled(sc, "مجلد المشاركة", etPath)
        val pathRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnApp = button("مجلد التطبيق", cPrimary, false) { etPath.setText(prefs.defaultRoot()) }
        val btnDl = button("التنزيلات", cPrimary, false) { useDownloads() }
        pathRow.addView(btnApp)
        pathRow.addView(btnDl)
        sc.addView(pathRow, lp(top = 6))
        swUpload = switchRow("السماح للأجهزة بالرفع وإنشاء المجلدات", prefs.allowUpload)
        swDelete = switchRow("السماح بالحذف وإعادة التسمية", prefs.allowDelete)
        swScreen = switchRow("إبقاء الشاشة مضاءة أثناء التشغيل", prefs.keepScreenOn)
        sc.addView(swUpload, lp(top = 8))
        sc.addView(swDelete, lp())
        sc.addView(swScreen, lp())
        settingsCol.addView(sc)
        settingsViews = listOf(etName, etPass, etPort, etPath, btnRandom, btnApp, btnDl, swUpload, swDelete)

        // نصائح
        val tc = card()
        tc.addView(heading("لاستمرار التشغيل"))
        tc.addView(small("بعض الهواتف توقف التطبيق في الخلفية. استثنِ التطبيق من توفير البطارية حتى لا تنقطع الشبكة أثناء النقل."))
        tc.addView(button("استثناء من توفير البطارية", cPrimary, false) { ignoreBattery() }, lp(top = 8))
        settingsCol.addView(tc)
        val speedNote = card()
        speedNote.addView(heading("نقل أسرع للملفات الكبيرة"))
        speedNote.addView(small("يختار التطبيق نطاق Wi‑Fi ‏5 GHz تلقائياً عندما يدعمه الهاتف، ويرجع إلى 2.4 GHz إذا لم يتوفر. أبقِ الهاتف والأجهزة قريبة أثناء النقل."))
        settingsCol.addView(speedNote)

        // صفحة تعريف التطبيق ومعلومات البناء الفعلية.
        aboutCol.addView(heading("حول Max Share"), lp(bottom = 4))
        val aboutIntro = card()
        aboutIntro.addView(heading("مشاركة محلية من هاتفك"))
        aboutIntro.addView(TextView(this).apply {
            text = "ينشئ التطبيق شبكة مشاركة محلية ويشغّل خادم الملفات على الهاتف. تتصل الأجهزة الأخرى بالشبكة وتفتح الرابط في المتصفح لتصفح الملفات وتنزيلها، من دون تثبيت التطبيق على الجهاز المستقبِل. يمكن للمرسل ضبط السماح بالرفع والحذف. لا يعتمد النقل على خدمة تخزين سحابية."
            textSize = 14f
            setTextColor(cSub)
            setLineSpacing(dp(3).toFloat(), 1f)
        }, lp(top = 4))
        aboutCol.addView(aboutIntro)

        val people = card()
        people.addView(heading("الجهة المطوّرة"))
        addAboutFact(people, "الشركة", com.portablefileserver.data.AppIdentity.company)
        addAboutFact(people, "المطوّر", com.portablefileserver.data.AppIdentity.developer)
        aboutCol.addView(people, lp(top = 12))

        val support = card()
        support.addView(heading("السعر والدعم"))
        addAboutFact(support, "السعر", com.portablefileserver.data.AppIdentity.price)
        addAboutFact(support, "التبرعات", com.portablefileserver.data.AppIdentity.funding)
        if (com.portablefileserver.data.AppIdentity.shamCashAccount.isBlank()) {
            support.addView(small("حساب شام كاش: لم يزوّد المطوّر التطبيق ببيانات حساب مؤكدة بعد."), lp(top = 2))
        } else {
            addAboutFact(support, "حساب شام كاش", com.portablefileserver.data.AppIdentity.shamCashAccount, ltr = true)
            support.addView(button("نسخ بيانات الحساب", cPrimary, false) {
                copy(com.portablefileserver.data.AppIdentity.shamCashAccount)
            }, lp(top = 4))
        }
        aboutCol.addView(support, lp(top = 12))

        val (versionName, versionCode) = appVersionDetails()
        val technical = card()
        technical.addView(heading("معلومات تقنية"))
        addAboutFact(technical, "الإصدار", versionName)
        addAboutFact(technical, "رقم البناء", versionCode.toString())
        addAboutFact(technical, "معرّف الحزمة", packageName, ltr = true)
        addAboutFact(technical, "المنصة", "Android")
        addAboutFact(technical, "التقنيات", "Kotlin وAndroid وNanoHTTPD")
        aboutCol.addView(technical, lp(top = 12))

        drawerOverlay = createDrawer()
        val screenHost = FrameLayout(this)
        screenHost.addView(root, FrameLayout.LayoutParams(-1, -1))
        screenHost.addView(drawerOverlay, FrameLayout.LayoutParams(-1, -1))
        setContentView(screenHost)
        selectTab(0)
    }

    override fun onResume() {
        super.onResume()
        ServerRuntime.addListener(listener)
        refresh()
        ui.post(ticker)
    }

    override fun onPause() {
        ServerRuntime.removeListener(listener)
        ui.removeCallbacks(ticker)
        super.onPause()
    }

    // ---------- تحديث الواجهة ----------
    private fun refresh() {
        val r = ServerRuntime
        val (pillText, pillColor) = when (r.state) {
            ServerRuntime.State.RUNNING -> "يعمل" to cOk
            ServerRuntime.State.STARTING -> "جارٍ التشغيل" to cWarn
            ServerRuntime.State.ERROR -> "خطأ" to cErr
            ServerRuntime.State.STOPPED -> "متوقف" to cField
        }
        pill.text = pillText
        pill.setTextColor(if (r.state == ServerRuntime.State.STOPPED) cSub else 0xFFFFFFFF.toInt())
        pill.background = rounded(pillColor, 20)
        tvStatusMsg.text = if (r.state == ServerRuntime.State.RUNNING) "" else r.status
        tvStatusMsg.setTextColor(if (r.state == ServerRuntime.State.ERROR) cErr else cSub)

        btnMain.updateState(r.state)
        settingsViews.forEach { it.isEnabled = !r.active }

        if (r.active && prefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val info = r.info
        val url = r.url
        if (r.active && info != null && url != null) {
            runCard.visibility = View.VISIBLE
            shareIdleCard.visibility = View.GONE
            tvSsid.text = info.ssid
            tvPass.text = info.password
            tvUrl.text = url
            tvMode.text = getString(com.portablefileserver.R.string.connection_mode, info.mode)
            val text = if (qrWifi) QrGenerator.wifi(info.ssid, info.password) else url
            btnQrMode.text = if (qrWifi) "عرض QR الرابط" else "عرض QR الشبكة"
            val key = (if (qrWifi) "w:" else "u:") + text
            if (key != lastQrKey) {
                lastQrKey = key
                ivQr.setImageBitmap(QrGenerator.make(text, 600))
            }
            refreshStats()
        } else {
            runCard.visibility = View.GONE
            shareIdleCard.visibility = View.VISIBLE
            lastQrKey = null
        }
    }

    private fun refreshStats() {
        val sent = ServerRuntime.bytesSent.get()
        val received = ServerRuntime.bytesReceived.get()
        if (!ServerRuntime.active || ServerRuntime.url == null) {
            lastSampleAt = 0L
            lastSentBytes = sent
            lastReceivedBytes = received
            tvSentRate.text = "0 B/s"
            tvReceivedRate.text = "0 B/s"
            tvSentTotal.text = getString(com.portablefileserver.R.string.transfer_total, fmt(sent))
            tvReceivedTotal.text = getString(com.portablefileserver.R.string.transfer_total, fmt(received))
            tvStats.text = "الخادم متوقف"
            return
        }
        val now = android.os.SystemClock.elapsedRealtime()
        if (lastSampleAt == 0L) {
            lastSampleAt = now
            lastSentBytes = sent
            lastReceivedBytes = received
        } else {
            val elapsed = (now - lastSampleAt).coerceAtLeast(1L)
            val downRate = ((sent - lastSentBytes).coerceAtLeast(0L) * 1000L) / elapsed
            val upRate = ((received - lastReceivedBytes).coerceAtLeast(0L) * 1000L) / elapsed
            tvSentRate.text = getString(com.portablefileserver.R.string.transfer_rate, fmt(downRate))
            tvReceivedRate.text = getString(com.portablefileserver.R.string.transfer_rate, fmt(upRate))
            lastSampleAt = now
            lastSentBytes = sent
            lastReceivedBytes = received
        }
        tvSentTotal.text = getString(com.portablefileserver.R.string.transfer_total, fmt(sent))
        tvReceivedTotal.text = getString(com.portablefileserver.R.string.transfer_total, fmt(received))
        tvStats.text = getString(
            com.portablefileserver.R.string.transfer_stats,
            ServerRuntime.activeClients(),
            ServerRuntime.requests.get()
        )
    }

    private fun fmt(n: Long): String = when {
        n < 1024 -> "$n B"
        n < 1024 * 1024 -> String.format(Locale.ROOT, "%.1f KB", n / 1024.0)
        n < 1024L * 1024 * 1024 -> String.format(Locale.ROOT, "%.1f MB", n / 1048576.0)
        else -> String.format(Locale.ROOT, "%.2f GB", n / 1073741824.0)
    }

    // ---------- الإجراءات ----------
    private fun toggle() {
        if (ServerRuntime.active) {
            startService(Intent(this, ServerService::class.java).setAction(ServerService.ACTION_STOP))
            return
        }
        val name = etName.text.toString().trim()
        val pass = etPass.text.toString()
        val port = etPort.text.toString().toIntOrNull() ?: 0
        val path = etPath.text.toString().trim()
        when {
            name.isEmpty() -> { toast("أدخل اسم الشبكة"); return }
            pass.length < 8 -> { toast("كلمة المرور يجب أن تكون 8 أحرف على الأقل"); return }
            port !in 1024..65535 -> { toast("المنفذ يجب أن يكون بين 1024 و 65535"); return }
            path.isEmpty() -> { toast("حدد مجلد المشاركة"); return }
        }
        prefs.networkName = name
        prefs.password = pass
        prefs.port = port
        prefs.rootPath = path
        prefs.allowUpload = swUpload.isChecked
        prefs.allowDelete = swDelete.isChecked
        prefs.keepScreenOn = swScreen.isChecked

        val missing = neededPermissions()
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 1) else startServer()
    }

    private fun neededPermissions(): List<String> {
        val l = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            l.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            l.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            l.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            l.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return l.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val wifiPerm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES else Manifest.permission.ACCESS_FINE_LOCATION
        if (checkSelfPermission(wifiPerm) == PackageManager.PERMISSION_GRANTED) startServer()
        else toast("الصلاحية مطلوبة لإنشاء الشبكة")
    }

    private fun startServer() {
        val path = File(prefs.rootPath)
        if (!path.exists()) path.mkdirs()
        if (!path.isDirectory) { toast("مجلد المشاركة غير صالح"); return }
        try {
            ContextCompat.startForegroundService(
                this, Intent(this, ServerService::class.java).setAction(ServerService.ACTION_START)
            )
        } catch (e: Exception) {
            toast("تعذّر تشغيل الخدمة: " + (e.message ?: ""))
        }
    }

    private fun useDownloads() {
        etPath.setText(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            toast("امنح صلاحية الوصول لكل الملفات ثم ارجع")
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun ignoreBattery() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                toast("افتح إعدادات تحسين البطارية واختر Max Share يدوياً")
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } else {
                toast("التطبيق مستثنى بالفعل")
            }
        } catch (e: Exception) {
            toast("تعذّر فتح الإعداد")
        }
    }

    private fun randomPassword(): String {
        val alphabet = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        return (1..10).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun copy(text: String) {
        try {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("pfs", text))
            toast("تم النسخ")
        } catch (e: Exception) {
            toast("تعذّر النسخ")
        }
    }

    private fun openUrl() {
        val u = ServerRuntime.url ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
        } catch (e: Exception) {
            toast("لا يوجد متصفح")
        }
    }

    private fun shareInfo() {
        val info = ServerRuntime.info ?: return
        val url = ServerRuntime.url ?: return
        val text = "الشبكة: " + info.ssid + "\nكلمة المرور: " + info.password + "\nالرابط: " + url
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "مشاركة بيانات الاتصال"))
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
