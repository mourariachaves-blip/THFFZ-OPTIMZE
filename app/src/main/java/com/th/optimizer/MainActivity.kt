package com.th.optimizer

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val RED = "#FF1744"
    private val CARD = "#1A0006"
    private val DAY = 86_400_000L
    private val h = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("th", MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private var tab = ""
    private var largeBox: LinearLayout? = null
    private var unusedBox: LinearLayout? = null

    private var pulse: ValueAnimator? = null
    private val navViews = mutableMapOf<String, LinearLayout>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.BLACK; window.navigationBarColor = Color.BLACK
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(0, dp(30), 0, 0)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#22000A"), Color.BLACK))
        }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20), dp(6), dp(20), dp(6)) }
        head.addView(ImageView(this).apply { setImageResource(R.drawable.logo) }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(8) })
        head.addView(label("TH", 30f, RED, true).apply { setShadowLayer(24f, 0f, 0f, Color.parseColor(RED)) })
        head.addView(label(" OPTIMIZE", 30f, "#FFFFFF", true))
        head.addView(label("  v4.1", 11f, "#9CA3AF", false))
        root.addView(head)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), dp(24)) }
        root.addView(ScrollView(this).apply { addView(content); isVerticalScrollBarEnabled = false }, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#0D0003")); setPadding(0, dp(8), 0, dp(10)) }
        listOf("INÍCIO" to "", "OTIMIZAR" to "", "GAMER" to "", "LIMPEZA" to "", "MAIS" to "").forEach { (n, ic) ->
            val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setOnClickListener { show(n) } }
            item.addView(label(n.lowercase().replaceFirstChar { it.uppercase() }, 12f, "#9CA3AF", false).apply { gravity = Gravity.CENTER })
            navViews[n] = item; nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        setContentView(root)
        show("INÍCIO")
    }

    private fun show(n: String) {
        h.removeCallbacksAndMessages(null); pulse?.cancel(); tab = n; content.removeAllViews()
        val active = if (n in listOf("INÍCIO", "OTIMIZAR", "GAMER", "LIMPEZA")) n else "MAIS"
        navViews.forEach { (k, v) -> v.alpha = if (k == active) 1f else 0.45f; (v.getChildAt(0) as TextView).setTextColor(Color.parseColor(if (k == active) RED else "#9CA3AF")) }
        when (n) { "INÍCIO" -> home(); "RAM" -> ramTab(); "HZ" -> hzTab(); "GAMER" -> gamerTab(); "OTIMIZAR" -> otimizarTab(); "LIMPEZA" -> cleanTab(); "BATERIA" -> batTab(); "ROOT" -> rootTab(); "MAIS" -> maisTab(); else -> infoTab() }
        for (i in 0 until content.childCount) content.getChildAt(i).apply { alpha = 0f; translationY = 70f; animate().alpha(1f).translationY(0f).setStartDelay(i * 80L).setDuration(380).start() }
    }

    private fun maisTab() {
        val c = card("Mais ferramentas")
        c.addView(btn("MAX HZ (taxa de atualização)", CARD) { show("HZ") })
        c.addView(btn("RAM Boost", CARD) { show("RAM") })
        c.addView(btn("Bateria", CARD) { show("BATERIA") })
        c.addView(btn("Info do aparelho e histórico", CARD) { show("INFO") })
        c.addView(btn("Modo Root (avançado)", CARD) { show("ROOT") })
    }

    // ---------- INÍCIO ----------
    private fun home() {
        fun rounded(fill: String, stroke: String, r: Int) = GradientDrawable().apply { setColor(Color.parseColor(fill)); cornerRadius = dp(r).toFloat(); setStroke(dp(1), Color.parseColor(stroke)) }
        fun sec(t: String) = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(22), 0, dp(8))
            addView(View(this@MainActivity).apply { setBackgroundColor(Color.parseColor(RED)) }, LinearLayout.LayoutParams(dp(3), dp(14)).apply { marginEnd = dp(8) })
            addView(label(t, 12f, "#9CA3AF", true).apply { letterSpacing = 0.15f })
        }
        fun panel() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(14), dp(12), dp(14)); background = rounded("#14000A", "#33FF1744", 18) }

        // Hero
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(22))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#3A000F"), Color.parseColor("#12000A"))).apply { cornerRadius = dp(22).toFloat(); setStroke(dp(1), Color.parseColor("#55FF1744")) }
        }
        hero.addView(label("CENTRAL DE PERFORMANCE", 11f, RED, true).apply { letterSpacing = 0.2f })
        hero.addView(TextView(this).apply {
            textSize = 26f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); setPadding(0, dp(8), 0, dp(8))
            val sp = SpannableString("MAIS DESEMPENHO\nPARA O SEU JOGO"); val i = sp.indexOf("SEU JOGO")
            sp.setSpan(ForegroundColorSpan(Color.parseColor(RED)), i, sp.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); text = sp
        })
        val sub = label("Limpeza de cache e memória com métodos oficiais do Android, sem hacks e sem root.", 13f, "#9CA3AF", false).apply { setPadding(0, 0, 0, dp(16)) }
        hero.addView(sub)
        val go = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(12), dp(24), dp(12))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor("#FF1744"), Color.parseColor("#B0002A"))).apply { cornerRadius = dp(16).toFloat() }
        }
        go.addView(label("Otimizar\nagora", 15f, "#FFFFFF", true).apply { setPadding(dp(10), 0, 0, 0) })
        hero.addView(go, LinearLayout.LayoutParams(-2, -2))
        content.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        pulse = ValueAnimator.ofFloat(1f, 1.04f).apply {
            duration = 900; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
            addUpdateListener { go.scaleX = it.animatedValue as Float; go.scaleY = it.animatedValue as Float }; start()
        }

        // Resultado (aparece após otimizar)
        val resP = panel().apply { visibility = View.GONE }
        resP.addView(label("RESULTADO", 11f, RED, true).apply { letterSpacing = 0.15f })
        val res = label("", 14f, "#FFFFFF", false).apply { setPadding(0, dp(6), 0, 0) }; resP.addView(res)
        content.addView(resP, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        // Status do dispositivo
        content.addView(sec("STATUS DO DISPOSITIVO"))
        fun ring(cap: String): Pair<LinearLayout, RingView> {
            val r = RingView(this)
            val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            l.addView(r, LinearLayout.LayoutParams(dp(78), dp(78)))
            l.addView(label(cap, 10f, "#9CA3AF", true).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0); letterSpacing = 0.1f })
            return l to r
        }
        val st = panel(); val row = LinearLayout(this)
        val (l1, r1) = ring("DESEMPENHO"); val (l2, r2) = ring("MEMÓRIA"); val (l3, r3) = ring("TELA (Hz)")
        listOf(l1, l2, l3).forEach { row.addView(it, LinearLayout.LayoutParams(0, -2, 1f)) }
        st.addView(row); content.addView(st, LinearLayout.LayoutParams(-1, -2))
        live("INÍCIO", 2000) {
            val m = mem(); val d = disp(); val sc = score()
            val maxHz = d.supportedModes.maxOf { it.refreshRate }
            val ram = (100 - m.availMem * 100 / m.totalMem).toInt()
            r1.progress = sc; r1.centerText = "$sc%"
            r2.progress = ram; r2.centerText = "$ram%"
            r3.progress = (d.refreshRate / maxHz * 100).toInt(); r3.centerText = "${Math.round(d.refreshRate)}"
        }

        // Acesso rápido
        content.addView(sec("ACESSO RÁPIDO"))
        fun quick(icon: String, title: String, subt: String, target: String) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(14)); background = rounded("#14000A", "#33FF1744", 18)
            val top = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL }
            top.addView(label(icon, 20f, "#FFFFFF", false).apply { gravity = Gravity.CENTER; setPadding(dp(10), dp(6), dp(10), dp(6)); background = rounded("#2A0A10", "#55FF1744", 10) })
            top.addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 1, 1f))
            top.addView(label("›", 22f, "#9CA3AF", false))
            addView(top)
            addView(label(title, 16f, "#FFFFFF", true).apply { setPadding(0, dp(14), 0, dp(2)) })
            addView(label(subt, 12f, "#9CA3AF", false))
            setOnClickListener { show(target) }
        }
        val qr = LinearLayout(this)
        qr.addView(quick("OT", "Otimização", "Melhore sua performance", "OTIMIZAR"), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        qr.addView(quick("AJ", "Ajustes", "Personalize sua experiência", "MAIS"), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        content.addView(qr)

        val vis = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        resP.addView(vis)
        go.setOnClickListener {
            go.isEnabled = false; resP.visibility = View.VISIBLE; sub.text = "Otimizando…"
            cleanVisual(vis, true) { sub.text = "Otimização concluída"; go.isEnabled = true; r1.progress = score() }
        }
    }

    // ---------- RAM ----------
    private fun ramTab() {
        val c = card("RAM BOOST")
        val info = label("", 15f, "#FFFFFF", false); val pb = bar(); c.addView(info); c.addView(pb)
        live("RAM", 2000) { val m = mem(); val p = (100 - m.availMem * 100 / m.totalMem).toInt(); pb.progress = p; info.text = "Total: ${gb(m.totalMem)} GB\nUsada: ${gb(m.totalMem - m.availMem)} GB ($p%)\nDisponível: ${gb(m.availMem)} GB" }
        val rep = label("", 14f, "#FFFFFF", false)
        c.addView(btn("LIBERAR RAM", RED) { rep.text = "Liberando…"; boost { rep.text = it } }); c.addView(rep)
        c.addView(label("O Android só permite encerrar processos em segundo plano autorizados pelo sistema. O relatório mostra a RAM medida antes e depois, sem estimativas.", 12f, "#9CA3AF", false).apply { setPadding(0, dp(10), 0, 0) })
    }

    private fun boost(step: (String) -> Unit = {}, done: (String) -> Unit) {
        Thread {
            fun s(t: String) = runOnUiThread { step(t) }
            s("Analisando o aparelho…")
            val ram0 = mem().availMem; val c0 = cacheStats().sumOf { it.second }; val sto0 = StatFs(filesDir.path).availableBytes
            Thread.sleep(500)
            s("Limpando cache de todos os apps…")
            deepClean(); cleanCache()
            s("Liberando memória em segundo plano…")
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            packageManager.getInstalledApplications(0).filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != packageName }.forEach { am.killBackgroundProcesses(it.packageName) }
            Thread.sleep(1500)
            val ram1 = mem().availMem; val sto1 = StatFs(filesDir.path).availableBytes; val c1 = cacheStats().sumOf { it.second }
            val r = "Cache liberado: ${fmtB(if (usageOk()) Math.max(0L, c0 - c1) else Math.max(0L, sto1 - sto0))}\nRAM livre: ${ram0 / 1048576} → ${ram1 / 1048576} MB"
            hist(r.replace("\n", " | "))
            runOnUiThread { done(r) }
        }.start()
    }

    // Pede ao sistema o espaço livre máximo: o Android apaga o cache de TODOS os apps para atender (sem root).
    private fun deepClean(): Long = try {
        val sm = getSystemService(STORAGE_SERVICE) as StorageManager
        val uuid = sm.getUuidForPath(filesDir)
        val before = StatFs(filesDir.path).availableBytes
        val f = File(filesDir, "th_alloc.tmp")
        try { java.io.FileOutputStream(f).use { sm.allocateBytes(it.fd, sm.getAllocatableBytes(uuid)) } } catch (e: Exception) { }
        f.delete()
        Thread.sleep(800)
        (StatFs(filesDir.path).availableBytes - before).coerceAtLeast(0L)
    } catch (e: Exception) { 0L }

    private fun fmtB(b: Long) = if (b >= 1048576L) "%.1f MB".format(b / 1048576.0) else "${b / 1024} KB"

    // Limpeza com animação: anel de progresso, etapas e resultado medido de verdade (antes/depois)
    private fun usageOk() = (getSystemService(APP_OPS_SERVICE) as AppOpsManager).unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED

    // Tamanho real do cache de cada app (o Android só informa isso com o acesso ao uso ligado)
    private fun cacheStats(): List<Pair<String, Long>> {
        if (!usageOk()) return emptyList()
        val ss = getSystemService(STORAGE_STATS_SERVICE) as StorageStatsManager
        val pm = packageManager
        return pm.getInstalledApplications(0).mapNotNull { ai ->
            try { Pair(pm.getApplicationLabel(ai).toString(), ss.queryStatsForPackage(ai.storageUuid, ai.packageName, Process.myUserHandle()).cacheBytes) } catch (e: Exception) { null }
        }
    }

    private fun countUp(tv: TextView, to: Long, prefix: String, ms: Long = 1400) {
        ValueAnimator.ofFloat(0f, to.toFloat()).apply { duration = ms; addUpdateListener { tv.text = prefix + fmtB((it.animatedValue as Float).toLong()) }; start() }
    }

    private fun cleanVisual(host: LinearLayout, withRam: Boolean, done: () -> Unit = {}) {
        host.removeAllViews()
        val ring = RingView(this)
        host.addView(ring, LinearLayout.LayoutParams(dp(120), dp(120)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(8) })
        val status = label("Preparando…", 14f, "#FFFFFF", true).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(6)) }
        host.addView(status, LinearLayout.LayoutParams(-1, -2))
        val found = label("", 22f, RED, true).apply { gravity = Gravity.CENTER }
        host.addView(found, LinearLayout.LayoutParams(-1, -2))
        val log = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        host.addView(log, LinearLayout.LayoutParams(-1, -2))
        fun step(p: Int, t: String) = runOnUiThread { ring.progress = p; ring.centerText = "$p%"; status.text = t }
        fun ok(t: String) = runOnUiThread { log.addView(label("OK   $t", 12f, "#00E676", false).apply { setPadding(0, dp(2), 0, dp(2)) }) }
        Thread {
            val path = Environment.getDataDirectory().path
            val sto0 = StatFs(path).availableBytes; val ram0 = mem().availMem
            step(8, "Analisando o aparelho…"); Thread.sleep(500)
            val statsOn = usageOk()
            val before = cacheStats(); val cache0 = before.sumOf { it.second }
            step(25, "Procurando cache nos apps…")
            if (statsOn) runOnUiThread { countUp(found, cache0, "Cache encontrado: ") }
            Thread.sleep(1500)
            ok(if (statsOn) "${before.count { it.second > 0 }} apps com cache (${fmtB(cache0)})" else "Acesso ao uso desligado: tamanho por app indisponível")
            step(45, "Limpando arquivos temporários…")
            val own = cleanCache(); Thread.sleep(500)
            ok("Cache do app removido: ${fmtB(own)}")
            step(65, "Pedindo ao sistema para limpar o cache dos apps…")
            deepClean(); Thread.sleep(800)
            ok("Pedido enviado ao sistema")
            if (withRam) {
                step(85, "Liberando memória em segundo plano…")
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val apps = packageManager.getInstalledApplications(0).filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != packageName }
                apps.forEach { am.killBackgroundProcesses(it.packageName) }
                Thread.sleep(1200)
                ok("Segundo plano: ${apps.size} apps verificados")
            }
            step(100, "Concluído"); Thread.sleep(500)
            val cache1 = cacheStats().sumOf { it.second }
            val sf = StatFs(path); val sto1 = sf.availableBytes; val ram1 = mem().availMem
            val freed = if (statsOn) maxOf(cache0 - cache1, 0L) else maxOf(sto1 - sto0, own, 0L)
            hist("Limpeza: ${fmtB(freed)} liberados" + if (withRam) " | RAM livre ${ram0 / 1048576} → ${ram1 / 1048576} MB" else "")
            runOnUiThread {
                found.visibility = View.GONE
                val big = label("", 28f, RED, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(14), 0, dp(2)) }
                host.addView(big, LinearLayout.LayoutParams(-1, -2))
                host.addView(label("LIBERADO", 11f, "#9CA3AF", true).apply { gravity = Gravity.CENTER; letterSpacing = 0.2f }, LinearLayout.LayoutParams(-1, -2))
                countUp(big, freed, "")
                if (statsOn) {
                    host.addView(label("Cache dos apps: ${fmtB(cache0)} → ${fmtB(cache1)}", 13f, "#FFFFFF", false).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, 0) })
                    before.filter { it.second > 0 }.sortedByDescending { it.second }.take(3).forEach { host.addView(label("${it.first}: ${fmtB(it.second)}", 12f, "#9CA3AF", false).apply { gravity = Gravity.CENTER }) }
                }
                host.addView(label("Armazenamento livre: ${fmtB(sto0)} → ${fmtB(sto1)}", 13f, "#FFFFFF", false).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) })
                host.addView(bar().apply { progress = (100 - sto1 * 100 / sf.totalBytes).toInt() })
                if (withRam) host.addView(label("RAM livre: ${ram0 / 1048576} MB → ${ram1 / 1048576} MB", 13f, "#FFFFFF", false).apply { gravity = Gravity.CENTER })
                if (!statsOn) {
                    host.addView(label("Para ver e limpar o cache real de cada app, libere o acesso ao uso.", 11f, "#9CA3AF", false).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) })
                    host.addView(btn("Liberar acesso ao uso", RED) { open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
                } else if (freed == 0L) {
                    host.addView(label("Nenhum cache sobrando: o aparelho já está limpo agora.", 11f, "#9CA3AF", false).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) })
                }
                done()
            }
        }.start()
    }

    private fun cleanCache(): Long {
        var freed = 0L
        listOfNotNull(cacheDir, externalCacheDir).forEach { d -> d.walkBottomUp().forEach { f -> if (f != d) { freed += f.length(); f.delete() } } }
        return freed
    }

    // ---------- MAX HZ ----------
    @Suppress("DEPRECATION")
    private fun disp() = windowManager.defaultDisplay

    private fun hzTab() {
        val d = disp(); val modes = d.supportedModes
        val rates = modes.map { Math.round(it.refreshRate) }.distinct().sortedDescending()
        val c = card("MAX HZ")
        val cur = label("", 28f, RED, true); c.addView(cur)
        live("HZ", 1000) { cur.text = "${"%.0f".format(disp().refreshRate)} Hz" }
        c.addView(label("Taxas suportadas: ${rates.joinToString(" / ") { "$it Hz" }}", 14f, "#FFFFFF", false))
        if (rates.size <= 1) c.addView(label("Este aparelho só suporta ${rates[0]} Hz.", 13f, "#FFC107", false))
        c.addView(btn("ATIVAR MAX HZ", RED) {
            val m = d.mode
            val best = modes.filter { it.physicalWidth == m.physicalWidth && it.physicalHeight == m.physicalHeight }.maxByOrNull { it.refreshRate } ?: m
            window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
            toast("Solicitado: ${Math.round(best.refreshRate)} Hz (vale enquanto o app estiver aberto)")
        })
        c.addView(btn("Configurações de tela", CARD) { open(Intent(Settings.ACTION_DISPLAY_SETTINGS)) })
    }

    // ---------- helpers de UI ----------
    private fun rnd(fill: String, stroke: String, r: Int) = GradientDrawable().apply { setColor(Color.parseColor(fill)); cornerRadius = dp(r).toFloat(); setStroke(dp(1), Color.parseColor(stroke)) }
    private fun box(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)); background = rnd("#14000A", "#33FF1744", 18) }
    private fun section(t: String) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(22), 0, dp(8))
        addView(View(this@MainActivity).apply { setBackgroundColor(Color.parseColor(RED)) }, LinearLayout.LayoutParams(dp(3), dp(14)).apply { marginEnd = dp(8) })
        addView(label(t, 12f, "#9CA3AF", true).apply { letterSpacing = 0.15f })
    }
    private fun gradBtn(t: String, f: () -> Unit) = TextView(this).apply {
        text = t; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize = 13f; setPadding(0, dp(12), 0, dp(12))
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor("#FF1744"), Color.parseColor("#B0002A"))).apply { cornerRadius = dp(12).toFloat() }
        setOnClickListener { f() }
    }
    private fun iconBox(ic: String) = label(ic, 18f, "#FFFFFF", false).apply { gravity = Gravity.CENTER; background = rnd("#2A0A10", "#55FF1744", 10); setPadding(dp(10), dp(6), dp(10), dp(6)) }
    private fun netName(): String {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val c = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return "Sem conexão"
        return if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) "Wi-Fi" else if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) "Dados móveis" else "Outra"
    }
    @Volatile private var pingMs = -1
    private fun pingAsync() = Thread {
        pingMs = try { val t0 = System.nanoTime(); java.net.Socket().use { it.connect(java.net.InetSocketAddress("1.1.1.1", 53), 1500) }; ((System.nanoTime() - t0) / 1_000_000).toInt() } catch (e: Exception) { -1 }
    }.start()

    // ---------- OTIMIZAR ----------
    private fun otimizarTab() {
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
        head.addView(label("RECURSOS EXCLUSIVOS DO ANDROID", 12f, "#9CA3AF", true).apply { letterSpacing = 0.12f }, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(label("ANDROID", 10f, RED, true).apply { setPadding(dp(10), dp(3), dp(10), dp(3)); background = rnd("#14000A", "#FF1744", 12) })
        content.addView(head)
        fun feature(ic: String, title: String, desc: String, f: () -> Unit) = box().apply {
            addView(iconBox(ic), LinearLayout.LayoutParams(-2, -2))
            addView(label(title, 15f, "#FFFFFF", true).apply { setPadding(0, dp(10), 0, dp(4)) })
            addView(label(desc, 11f, "#9CA3AF", false).apply { setPadding(0, 0, 0, dp(12)) })
            addView(gradBtn("Abrir Configuração", f))
        }
        val row = LinearLayout(this)
        row.addView(feature("BT", "Ignorar Otimização de Bateria", "Abre direto a tela real do sistema, sem precisar procurar") { open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        row.addView(feature("DV", "Opções do Desenvolvedor", "Acesso rápido para ativar renderização GPU e limites de processo") { open(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        content.addView(row)

        val dg = box()
        val sc = score()
        dg.addView(RingView(this).apply { progress = sc; centerText = "$sc%" }, LinearLayout.LayoutParams(dp(56), dp(56)))
        dg.addView(label("Diagnóstico Real do Aparelho", 16f, "#FFFFFF", true).apply { setPadding(0, dp(10), 0, dp(2)) })
        dg.addView(label("Lidos agora do seu Android, sem número inventado", 11f, "#9CA3AF", false).apply { setPadding(0, 0, 0, dp(10)) })
        fun line(k: String, v: String) { val l = LinearLayout(this).apply { setPadding(0, dp(3), 0, dp(3)) }; l.addView(label(k, 12f, "#9CA3AF", false), LinearLayout.LayoutParams(0, -2, 1f)); l.addView(label(v, 12f, "#FFFFFF", true)); dg.addView(l) }
        line("Núcleos de CPU", "${Runtime.getRuntime().availableProcessors()} núcleos"); line("RAM total", "${gb(mem().totalMem)} GB"); line("Rede atual", netName())
        content.addView(dg, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })


        val res = box().apply { visibility = View.GONE }; val rl = label("", 14f, "#FFFFFF", false); res.addView(rl)
        val big = gradBtn("OTIMIZAR AGORA") { }.apply { textSize = 16f; letterSpacing = 0.1f; setPadding(0, dp(18), 0, dp(18)) }
        big.setOnClickListener {
            big.isEnabled = false; res.visibility = View.VISIBLE
            cleanVisual(res, true) { big.isEnabled = true }
        }
        content.addView(big, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        content.addView(res, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
    }

    // ---------- MODO GAMER ----------
    private fun gamerTab() {
        val on = prefs.getBoolean("gamer", false)
        val hero = box().apply { gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(16), dp(24), dp(16), dp(20)) }
        val circle = label("GM", 30f, "#FFFFFF", true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#2A0A10")); setStroke(dp(2), Color.parseColor(if (on) "#00E676" else RED)) }
        }
        hero.addView(circle, LinearLayout.LayoutParams(dp(96), dp(96)))
        pulse = ValueAnimator.ofFloat(1f, 1.06f).apply { duration = 1100; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE; addUpdateListener { circle.scaleX = it.animatedValue as Float; circle.scaleY = it.animatedValue as Float }; start() }
        hero.addView(label(if (on) "Modo Gamer Ativo" else "Modo Gamer Inativo", 20f, "#FFFFFF", true).apply { gravity = Gravity.CENTER; setPadding(0, dp(14), 0, dp(4)) })
        val sub = label(if (on) "Foco total no seu jogo" else "Ative para focar 100% no seu jogo", 12f, "#9CA3AF", false).apply { gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(16)) }
        hero.addView(sub)
        val bt = gradBtn(if (on) "DESATIVAR MODO GAMER" else "ATIVAR MODO GAMER") { }.apply { setPadding(0, dp(16), 0, dp(16)); letterSpacing = 0.08f }
        hero.addView(bt, LinearLayout.LayoutParams(-1, -2))
        content.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        bt.setOnClickListener {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (!prefs.getBoolean("gamer", false)) {
                bt.isEnabled = false
                boost({ sub.text = it }) { r ->
                    if (nm.isNotificationPolicyAccessGranted) { prefs.edit().putInt("prevFilter", nm.currentInterruptionFilter).apply(); nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
                    else toast("Para silenciar notificações, permita o acesso em Opções > Não Perturbe")
                    prefs.edit().putBoolean("gamer", true).apply(); toast(r); show("GAMER")
                }
            } else {
                if (nm.isNotificationPolicyAccessGranted) nm.setInterruptionFilter(prefs.getInt("prevFilter", NotificationManager.INTERRUPTION_FILTER_ALL))
                prefs.edit().putBoolean("gamer", false).apply(); show("GAMER")
            }
        }

        fun tile(cap: String, ic: String): Triple<LinearLayout, TextView, ProgressBar> {
            val t = box(); val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            top.addView(label(cap, 11f, "#9CA3AF", true).apply { letterSpacing = 0.1f }, LinearLayout.LayoutParams(0, -2, 1f)); top.addView(iconBox(ic))
            val v = label("--", 24f, "#FFFFFF", true).apply { setPadding(0, dp(8), 0, dp(6)) }; val pb = bar()
            t.addView(top); t.addView(v); t.addView(pb); return Triple(t, v, pb)
        }
        val a = tile("TELA", "HZ"); val b = tile("MEMÓRIA", "RAM"); val c = tile("REDE", "NET"); val d = tile("TEMPERATURA", "°C")
        val r1 = LinearLayout(this); val r2 = LinearLayout(this)
        r1.addView(a.first, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) }); r1.addView(b.first, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        r2.addView(c.first, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) }); r2.addView(d.first, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        content.addView(r1, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }); content.addView(r2, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        content.addView(section("OPÇÕES DO MODO GAMER"))
        fun opt(ic: String, t: String, f: () -> Unit) { val o = box().apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setOnClickListener { f() } }
            o.addView(iconBox(ic)); o.addView(label(t, 14f, "#FFFFFF", true).apply { setPadding(dp(12), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f)); o.addView(label("›", 22f, "#9CA3AF", false))
            content.addView(o, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }) }
        opt("DND", "Não Perturbe (permitir acesso)") { open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
        opt("HZ", "Taxa de atualização máxima (MAX HZ)") { show("HZ") }
        opt("RAM", "Liberar RAM agora") { boost { toast(it) } }
        opt("BT", "Economia de bateria") { open(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }
        val tb = box(); val tl = label("", 12f, "#9CA3AF", false); tb.addView(tl); content.addView(tb, LinearLayout.LayoutParams(-1, -2))
        val names = listOf("Normal", "Leve", "Moderado", "Severo", "Crítico", "Emergência", "Desligamento")
        live("GAMER", 2000) {
            val m = mem(); val dsp = disp(); val maxHz = dsp.supportedModes.maxOf { it.refreshRate }
            a.second.text = "${Math.round(dsp.refreshRate)} Hz"; a.third.progress = (dsp.refreshRate / maxHz * 100).toInt()
            val ram = (100 - m.availMem * 100 / m.totalMem).toInt(); b.second.text = "$ram%"; b.third.progress = ram
            pingAsync(); val p = pingMs
            c.second.text = if (p < 0) "-- ms" else "$p ms"; c.third.progress = if (p < 0) 0 else (100 - p / 2).coerceIn(5, 100)
            d.second.text = "${temp()}°C"; d.third.progress = (temp() / 60 * 100).toInt().coerceIn(0, 100)
            tl.text = "Estado térmico: ${names.getOrElse((getSystemService(POWER_SERVICE) as PowerManager).currentThermalStatus) { "?" }}\n${cpuFreqs()}"
        }
    }

    private fun cpuFreqs() = (0 until Runtime.getRuntime().availableProcessors()).joinToString("\n") { i ->
        val f = try { File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq").readText().trim().toLong() / 1000 } catch (e: Exception) { -1L }
        "Núcleo $i: ${if (f < 0) "indisponível" else "$f MHz"}"
    }

    // ---------- LIMPEZA ----------
    private fun cleanTab() {
        val sf = StatFs(Environment.getDataDirectory().path)
        val c = card("JUNK CLEANER")
        val used = (100 - sf.availableBytes * 100 / sf.totalBytes).toInt()
        c.addView(label("Livre: ${gb(sf.availableBytes)} GB de ${gb(sf.totalBytes)} GB ($used% usado)", 14f, "#FFFFFF", false))
        c.addView(bar().apply { progress = used })
        val vis = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.addView(btn("LIMPEZA PROFUNDA", RED) { cleanVisual(vis, false) })
        c.addView(vis)
        c.addView(btn("Liberar espaço (sistema)", CARD) { open(Intent("android.os.storage.action.MANAGE_STORAGE")) })
        val lg = card("Arquivos grandes (> 20 MB)")
        lg.addView(btn("Procurar", CARD) { scanLarge() })
        largeBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; lg.addView(largeBox)
        val un = card("APP MANAGER – apps sem uso (30+ dias)")
        un.addView(btn("Procurar", CARD) { scanUnused() })
        unusedBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; un.addView(unusedBox)
    }

    private fun scanLarge() {
        val perms = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (perms.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) { requestPermissions(perms, 1); return }
        Thread {
            val items = mutableListOf<Triple<Uri, String, Long>>()
            val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (1,3) AND ${MediaStore.MediaColumns.SIZE} > ?"
            contentResolver.query(MediaStore.Files.getContentUri("external"), arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.Files.FileColumns.MEDIA_TYPE),
                sel, arrayOf("20971520"), "${MediaStore.MediaColumns.SIZE} DESC")?.use { c ->
                while (c.moveToNext() && items.size < 15) {
                    val base = if (c.getInt(3) == 1) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    items.add(Triple(ContentUris.withAppendedId(base, c.getLong(0)), c.getString(1) ?: "arquivo", c.getLong(2)))
                }
            }
            runOnUiThread { showLarge(items) }
        }.start()
    }

    private fun showLarge(items: List<Triple<Uri, String, Long>>) {
        val box = largeBox ?: return; box.removeAllViews()
        if (items.isEmpty()) { box.addView(label("Nenhum arquivo grande", 13f, "#00E676", false)); return }
        val boxes = items.map { (u, n, s) -> CheckBox(this).apply { text = "$n — ${s / 1048576} MB"; setTextColor(Color.WHITE); tag = u }.also { box.addView(it) } }
        box.addView(btn("Apagar selecionados", RED) {
            val uris = boxes.filter { it.isChecked }.map { it.tag as Uri }
            if (uris.isEmpty()) return@btn
            if (Build.VERSION.SDK_INT >= 30) startIntentSenderForResult(MediaStore.createDeleteRequest(contentResolver, uris).intentSender, 2, null, 0, 0, 0)
            else toast("Exclusão em lote exige Android 11+. Apague pela Galeria.")
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, g: IntArray) { super.onRequestPermissionsResult(rc, p, g); if (rc == 1) scanLarge() }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(rc: Int, res: Int, d: Intent?) { super.onActivityResult(rc, res, d); if (rc == 2 && res == RESULT_OK) { hist("Arquivos grandes apagados pelo usuário"); scanLarge() } }

    private fun scanUnused() {
        val a = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        if (a.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) != AppOpsManager.MODE_ALLOWED) {
            toast("Ative o acesso ao uso para o TH Optimize e volte"); open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); return
        }
        val now = System.currentTimeMillis()
        val last = (getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager).queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, now - 365 * DAY, now)
            .groupBy { it.packageName }.mapValues { e -> e.value.maxOf { it.lastTimeUsed } }
        val pm = packageManager
        val apps = pm.getInstalledApplications(0).filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { it to (last[it.packageName] ?: 0L) }.filter { now - it.second > 30 * DAY }.sortedBy { it.second }.take(15)
        val box = unusedBox ?: return; box.removeAllViews()
        if (apps.isEmpty()) box.addView(label("Nenhum app parado", 13f, "#00E676", false))
        apps.forEach { (ai, t) ->
            val days = if (t == 0L) "+1 ano" else "${(now - t) / DAY} dias"
            box.addView(label("${pm.getApplicationLabel(ai)} — $days", 14f, "#FFFFFF", false).apply {
                setPadding(0, dp(10), 0, dp(10)); setOnClickListener { open(Intent(Intent.ACTION_DELETE, Uri.parse("package:${ai.packageName}"))) }
            })
        }
    }

    // ---------- BATERIA ----------
    private fun batTab() {
        val c = card("BATTERY SAVER")
        val info = label("", 15f, "#FFFFFF", false); c.addView(info)
        live("BATERIA", 2000) {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val st = when (bat(BatteryManager.EXTRA_STATUS)) { 2 -> "Carregando"; 5 -> "Completa"; 3 -> "Descarregando"; else -> "Não carregando" }
            val pl = when (bat(BatteryManager.EXTRA_PLUGGED)) { 1 -> "Tomada (AC)"; 2 -> "USB"; 4 -> "Sem fio"; else -> "—" }
            val hl = when (bat(BatteryManager.EXTRA_HEALTH)) { 2 -> "Boa"; 3 -> "Superaquecida"; 4 -> "Ruim"; 5 -> "Sobretensão"; 7 -> "Fria"; else -> "Desconhecida" }
            val ma = Math.abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)) / 1000
            info.text = "Nível: ${level()}%\nTemperatura: ${temp()} °C\nEstado: $st\nFonte: $pl\nSaúde: $hl\nTensão: ${bat(BatteryManager.EXTRA_VOLTAGE)} mV\nCorrente: $ma mA"
        }
        c.addView(btn("Modo economia de energia", RED) { open(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) })
        c.addView(btn("Consumo por aplicativo", CARD) { open(Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) })
    }

    // ---------- INFO ----------
    private fun infoTab() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val c = card("DEVICE INFO")
        c.addView(label("${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nHardware: ${Build.HARDWARE}\nNúcleos: ${Runtime.getRuntime().availableProcessors()}\nOpenGL ES: ${am.deviceConfigurationInfo.glEsVersion}\nRAM: ${gb(mem().totalMem)} GB", 14f, "#FFFFFF", false))
        val n = card("NETWORK MONITOR"); val nl = label("", 14f, "#FFFFFF", false); n.addView(nl)
        live("INFO", 3000) {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            nl.text = if (caps == null) "Sem conexão" else "Tipo: ${if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) "Wi-Fi" else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) "Dados móveis" else "Outro"}\nDownload estimado: ${caps.linkDownstreamBandwidthKbps / 1000} Mbps\nUpload estimado: ${caps.linkUpstreamBandwidthKbps / 1000} Mbps"
        }
        val hc = card("HISTÓRICO")
        hc.addView(label(prefs.getString("hist", "")!!.ifEmpty { "Nenhuma operação ainda." }, 12f, "#9CA3AF", false))
        hc.addView(btn("Limpar histórico", CARD) { prefs.edit().remove("hist").apply(); show("INFO") })
    }

    // ---------- ROOT ----------
    private fun rootTab() {
        val c = card("MODO ROOT")
        val st = label("Verificando root… (aceite o pedido do gerenciador de root)", 14f, "#FFFFFF", false); c.addView(st)
        val out = label("", 12f, "#9CA3AF", false).apply { setPadding(0, dp(10), 0, 0) }
        fun go(cmd: String, confirm: Boolean = false) {
            val doIt = {
                out.text = "Executando…"
                Thread {
                    val before = mem().availMem
                    val r = RootTools.run(cmd)
                    Thread.sleep(1200)
                    val after = mem().availMem
                    val msg = "RAM livre: ${before / 1048576} → ${after / 1048576} MB\nResultado: ${r.first.take(300).ifEmpty { "ok" }} (código ${r.second})"
                    hist("ROOT: " + msg.replace("\n", " | "))
                    runOnUiThread { out.text = msg }
                }.start()
            }
            if (!confirm) doIt() else AlertDialog.Builder(this).setTitle("Confirmar")
                .setMessage("Isso fecha à força todos os apps instalados por você (alarmes e mensagens podem parar até serem abertos de novo). Continuar?")
                .setPositiveButton("Continuar") { _, _ -> doIt() }.setNegativeButton("Cancelar", null).show()
        }
        Thread {
            val ok = RootTools.hasRoot()
            runOnUiThread {
                if (tab != "ROOT") return@runOnUiThread
                if (!ok) { st.text = "Root não detectado neste aparelho. Sem root, use o método ADB pelo PC (veja o tutorial)."; return@runOnUiThread }
                st.text = "Root ativo"
                c.addView(btn("LIMPEZA COMPLETA (ROOT)", RED) { go("pm trim-caches 999999999999; sync; echo 3 > /proc/sys/vm/drop_caches; am kill-all") })
                c.addView(btn("Limpar cache de TODOS os apps", CARD) { go("pm trim-caches 999999999999") })
                c.addView(btn("Limpar cache da RAM do sistema", CARD) { go("sync; echo 3 > /proc/sys/vm/drop_caches") })
                c.addView(btn("Forçar parada dos apps instalados", CARD) { go("am kill-all; for p in \$(pm list packages -3 | cut -d: -f2 | grep -v com.th.optimizer); do am force-stop \$p; done", true) })
                c.addView(btn("Limpar logs do sistema", CARD) { go("logcat -c") })
                c.addView(btn("TRIM do armazenamento", CARD) { go("fstrim -v /data") })
                c.addView(out)
            }
        }.start()
    }

    // ---------- helpers ----------
    private fun hist(m: String) {
        val line = "${SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date())} — $m"
        prefs.edit().putString("hist", (listOf(line) + prefs.getString("hist", "")!!.split("\n").filter { it.isNotBlank() }).take(20).joinToString("\n")).apply()
    }
    private fun live(name: String, ms: Long, f: () -> Unit) { f(); h.postDelayed(object : Runnable { override fun run() { if (tab == name) { f(); h.postDelayed(this, ms) } } }, ms) }
    private fun mem() = ActivityManager.MemoryInfo().also { (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it) }
    private fun bat(k: String, d: Int = 0) = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(k, d) ?: d
    private fun level() = bat(BatteryManager.EXTRA_LEVEL) * 100 / bat(BatteryManager.EXTRA_SCALE, 100)
    private fun temp() = bat(BatteryManager.EXTRA_TEMPERATURE) / 10.0
    private fun score(): Int {
        val m = mem(); val sf = StatFs(Environment.getDataDirectory().path)
        val ram = (100 - m.availMem * 100 / m.totalMem).toInt(); val sto = (100 - sf.availableBytes * 100 / sf.totalBytes).toInt()
        return (100 - ram / 3 - sto / 3 - (if (temp() > 40) 15 else 0)).coerceIn(0, 100)
    }
    private fun open(i: Intent) { try { startActivity(i) } catch (e: Exception) { try { startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) { toast("Não disponível neste aparelho") } } }
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun gb(v: Long) = "%.1f".format(v / 1073741824.0)
    private fun label(t: String, s: Float, c: String, bold: Boolean) = TextView(this).apply { text = t; textSize = s; setTextColor(Color.parseColor(c)); if (bold) setTypeface(null, Typeface.BOLD) }
    private fun bar() = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 100; progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(RED)); layoutParams = LinearLayout.LayoutParams(-1, dp(10)).apply { topMargin = dp(6); bottomMargin = dp(6) }
    }
    private fun card(title: String): LinearLayout {
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply { setColor(Color.parseColor(CARD)); cornerRadius = dp(16).toFloat(); setStroke(dp(1), Color.parseColor("#55FF1744")) }
        }
        l.addView(label(title, 16f, RED, true).apply { setPadding(0, 0, 0, dp(8)) })
        content.addView(l, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        return l
    }
    private fun btn(t: String, color: String, f: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; setTextColor(Color.WHITE); setOnClickListener { f() }
        background = GradientDrawable().apply { setColor(Color.parseColor(color)); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.parseColor(RED)) }
        layoutParams = LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8) }
    }
}
