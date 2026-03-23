package ru.tabel.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ru.tabel.app.MainActivity
import ru.tabel.app.data.model.ShiftType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SIZE_MINI    = DpSize(100.dp,  50.dp)
private val SIZE_SMALL  = DpSize(140.dp,  70.dp)
private val SIZE_MEDIUM = DpSize(180.dp, 140.dp)
private val SIZE_LARGE  = DpSize(250.dp, 180.dp)
private val SIZE_XLARGE = DpSize(310.dp, 200.dp)

class ShiftWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget()
}

data class WidgetShift(
    val date: String,
    val typeName: String,
    val startTime: String?,
    val endTime: String?
)

class ShiftWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_MINI, SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE, SIZE_XLARGE)
    )

    companion object {
        const val PREFS       = "tabel_widget"
        const val KEY_PROFILE = "profile"
        const val MAX_SHIFTS  = 7
        fun kDate(i: Int) = "date_$i"
        fun kType(i: Int) = "type_$i"
        fun kTime(i: Int) = "time_$i"
        fun kEnd(i: Int) = "end_$i"
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val p       = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val profile = p.getString(KEY_PROFILE, "Мой График") ?: "Мой График"
        val shifts  = (0 until MAX_SHIFTS).mapNotNull { i ->
            val date = p.getString(kDate(i), null) ?: return@mapNotNull null
            val type = p.getString(kType(i), null) ?: return@mapNotNull null
            WidgetShift(date, type, p.getString(kTime(i), null), p.getString(kEnd(i), null))
        }
        provideContent {
            val size = LocalSize.current
            when {
                size.height >= SIZE_XLARGE.height -> XLargeWidget(shifts, profile)
                size.height >= SIZE_LARGE.height  -> LargeWidget(shifts, profile)
                size.height >= SIZE_MEDIUM.height -> MediumWidget(shifts, profile)
                size.height >= SIZE_SMALL.height  -> SmallWidget(shifts)
                else                            -> MiniWidget(shifts)
            }
        }
    }
}

// ── Утилиты ───────────────────────────────────────────────────
private val MONTHS_SHORT = listOf("янв","фев","мар","апр","май","июн","июл","авг","сен","окт","ноя","дек")
private val MONTHS_FULL  = listOf("января","февраля","марта","апреля","мая","июня","июля","августа","сентября","октября","ноября","декабря")
private val DAYS_SHORT   = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
private val DOW_FULL     = listOf("понедельник","вторник","среда","четверг","пятница","суббота","воскресенье")

private val todayStr     get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
private val tomorrowStr  get() = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun dateLabel(date: String, short: Boolean = false) = when (date) {
    todayStr    -> if (short) "Сег." else "Сегодня"
    tomorrowStr -> if (short) "Зав." else "Завтра"
    else -> runCatching {
        LocalDate.parse(date).let {
            if (short) "${it.dayOfMonth} ${MONTHS_SHORT[it.monthValue - 1]}"
            else "${it.dayOfMonth} ${MONTHS_FULL[it.monthValue - 1]}"
        }
    }.getOrDefault(date)
}

private fun shiftColor(t: String) = Color(
    runCatching { ShiftType.valueOf(t).color }.getOrDefault(0xFF6B7280)
)

private fun shiftIcon(t: String) = runCatching { ShiftType.valueOf(t).icon }.getOrDefault("📋")

private fun shiftLabel(t: String) = runCatching { ShiftType.valueOf(t).label }.getOrDefault(t)

private fun shiftShortLabel(t: String) = when (runCatching { ShiftType.valueOf(t) }.getOrNull()) {
    ShiftType.DAY      -> "День"
    ShiftType.NIGHT    -> "Ночь"
    ShiftType.SLEEP    -> "Отсып"
    ShiftType.OFF      -> "Вых"
    ShiftType.HOLIDAY  -> "Празд"
    ShiftType.SICK     -> "Болн"
    ShiftType.VACATION -> "Отпуск"
    null               -> t.take(4)
}

private fun shiftAbbr(t: String) = when (runCatching { ShiftType.valueOf(t) }.getOrNull()) {
    ShiftType.DAY      -> "Д"
    ShiftType.NIGHT    -> "Н"
    ShiftType.SLEEP    -> "От"
    ShiftType.OFF      -> "В"
    ShiftType.HOLIDAY  -> "Пр"
    ShiftType.SICK     -> "Б"
    ShiftType.VACATION -> "О"
    null               -> "?"
}

// Палитра
private val BG_DARK    = Color(0xFF0D0D1C)
private val BG_LIGHT  = Color(0xFF1A1A2E)
private val CARD      = Color(0xFF181828)
private val CARD_LIGHT= Color(0xFF252540)
private val WHITE     = Color(0xFFFFFFFF)
private val GREY      = Color(0xFF9090AA)
private val MUTED     = Color(0xFF505068)
private val BLUE      = Color(0xFF4F6EF7)
private val PURPLE    = Color(0xFF8B5CF6)
private val GREEN     = Color(0xFF22c55e)
private val ORANGE    = Color(0xFFf97316)
private val RED       = Color(0xFFef4444)

// ── Общий компонент: дата-панель ───────────────────────────────
@Composable
private fun DatePanel(dayOfMonth: Int, monthIndex: Int, year: Int, dow: String, compact: Boolean = false) {
    Column {
        Text(
            dayOfMonth.toString(),
            style = TextStyle(
                fontSize   = if (compact) 18.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color      = ColorProvider(WHITE)
            )
        )
        Text(
            dow,
            style = TextStyle(
                fontSize   = 9.sp,
                fontWeight = FontWeight.Bold,
                color      = ColorProvider(BLUE)
            )
        )
        if (!compact) {
            Text(
                "${MONTHS_FULL[monthIndex]} $year",
                style = TextStyle(fontSize = 8.sp, color = ColorProvider(GREY))
            )
        }
    }
}

// ── MINI (2×1) — только следующая смена ───────────────────────
@Composable
private fun MiniWidget(shifts: List<WidgetShift>) {
    val next  = shifts.firstOrNull()
    val color = if (next != null) shiftColor(next.typeName) else BLUE

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BG_DARK))
            .cornerRadius(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                shiftAbbr(next?.typeName ?: "DAY"),
                style = TextStyle(
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ColorProvider(color)
                )
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                dateLabel(next?.date ?: "", short = true),
                style = TextStyle(fontSize = 10.sp, color = ColorProvider(GREY))
            )
        }
    }
}

// ── SMALL (2×1) — дата + ближайшая смена ────────────────────
@Composable
private fun SmallWidget(shifts: List<WidgetShift>) {
    val next  = shifts.firstOrNull()
    val color = if (next != null) shiftColor(next.typeName) else BLUE

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BG_DARK))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(GlanceModifier.fillMaxHeight().width(4.dp).background(ColorProvider(color))) {}

        Row(
            modifier = GlanceModifier.fillMaxSize().padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val today = LocalDate.now()
            Text(
                today.dayOfMonth.toString(),
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WHITE))
            )
            Spacer(GlanceModifier.width(4.dp))
            Column {
                Text(
                    DAYS_SHORT[today.dayOfWeek.value - 1],
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorProvider(BLUE))
                )
                Text(
                    "${MONTHS_SHORT[today.monthValue - 1]}",
                    style = TextStyle(fontSize = 8.sp, color = ColorProvider(GREY))
                )
            }

            Spacer(GlanceModifier.width(8.dp))
            Box(GlanceModifier.width(1.dp).height(28.dp).background(ColorProvider(MUTED))) {}
            Spacer(GlanceModifier.width(8.dp))

            if (next == null) {
                Text("Нет смен", style = TextStyle(fontSize = 10.sp, color = ColorProvider(GREY)))
            } else {
                Column {
                    Text(
                        "${shiftIcon(next.typeName)} ${shiftShortLabel(next.typeName)}",
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WHITE))
                    )
                    Text(
                        dateLabel(next.date) + (next.startTime?.let { " $it" } ?: ""),
                        style = TextStyle(fontSize = 9.sp, color = ColorProvider(if (next.date == todayStr) color else GREY))
                    )
                }
            }
        }
    }
}

// ── MEDIUM (3×2) — дата + 3-4 смены ─────────────────────────
@Composable
private fun MediumWidget(shifts: List<WidgetShift>, profile: String) {
    val today = LocalDate.now()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BG_DARK))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(GlanceModifier.fillMaxSize().padding(12.dp)) {

            // Заголовок: дата + профиль
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DatePanel(today.dayOfMonth, today.monthValue - 1, today.year, DAYS_SHORT[today.dayOfWeek.value - 1], compact = true)
                Spacer(GlanceModifier.defaultWeight())
                Text(profile, style = TextStyle(fontSize = 9.sp, color = ColorProvider(MUTED)))
            }

            Spacer(GlanceModifier.height(10.dp))

            if (shifts.isEmpty()) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет предстоящих смен", style = TextStyle(fontSize = 11.sp, color = ColorProvider(MUTED)))
                }
            } else {
                shifts.take(4).forEach { s ->
                    val color   = shiftColor(s.typeName)
                    val isToday = s.date == todayStr
                    Spacer(GlanceModifier.height(4.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(10.dp)
                            .background(ColorProvider(if (isToday) color.copy(alpha = 0.18f) else CARD))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(GlanceModifier.width(6.dp).height(6.dp).cornerRadius(3.dp).background(ColorProvider(color))) {}
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            "${shiftIcon(s.typeName)} ${shiftLabel(s.typeName)}",
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                fontSize   = 11.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color      = ColorProvider(WHITE)
                            )
                        )
                        Text(
                            dateLabel(s.date, short = true),
                            style = TextStyle(fontSize = 9.sp, color = ColorProvider(if (isToday) color else GREY))
                        )
                    }
                }
            }
        }
    }
}

// ── LARGE (4×2) — дата-панель + список смен ──────────────────
@Composable
private fun LargeWidget(shifts: List<WidgetShift>, profile: String) {
    val hero      = shifts.firstOrNull { it.date == todayStr } ?: shifts.firstOrNull()
    val heroColor = if (hero != null) shiftColor(hero.typeName) else BLUE
    val listItems = shifts.filter { it != hero }.take(5)
    val today     = LocalDate.now()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BG_DARK))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(GlanceModifier.fillMaxSize()) {

            // ── Левая панель: дата + сегодняшняя/ближайшая смена ──
            Column(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .background(ColorProvider(heroColor.copy(alpha = 0.12f)))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    today.dayOfMonth.toString(),
                    style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WHITE))
                )
                Text(
                    DAYS_SHORT[today.dayOfWeek.value - 1],
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorProvider(BLUE))
                )
                Text(
                    "${MONTHS_SHORT[today.monthValue - 1]} ${today.year}",
                    style = TextStyle(fontSize = 8.sp, color = ColorProvider(GREY))
                )

                Spacer(GlanceModifier.height(8.dp))
                Box(GlanceModifier.width(30.dp).height(1.dp).background(ColorProvider(heroColor.copy(alpha = 0.4f)))) {}
                Spacer(GlanceModifier.height(6.dp))

                if (hero != null) {
                    Text(shiftIcon(hero.typeName), style = TextStyle(fontSize = 20.sp))
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        shiftLabel(hero.typeName),
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WHITE))
                    )
                    if (hero.startTime != null) {
                        Text(
                            "${hero.startTime} – ${hero.endTime ?: ""}",
                            style = TextStyle(fontSize = 9.sp, color = ColorProvider(heroColor))
                        )
                    }
                } else {
                    Text("Нет смен", style = TextStyle(fontSize = 9.sp, color = ColorProvider(MUTED)))
                }

                Spacer(GlanceModifier.defaultWeight())
                Text(
                    "📅 $profile",
                    style = TextStyle(fontSize = 7.sp, color = ColorProvider(MUTED))
                )
            }

            // ── Правая панель: список смен ─────────────────────
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    "Расписание",
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorProvider(GREY))
                )
                Spacer(GlanceModifier.height(6.dp))

                if (listItems.isEmpty()) {
                    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (hero == null) "Добавьте смены\nв приложении" else "Больше смен нет",
                            style = TextStyle(fontSize = 10.sp, color = ColorProvider(MUTED))
                        )
                    }
                } else {
                    listItems.forEach { s ->
                        val color   = shiftColor(s.typeName)
                        val isToday = s.date == todayStr
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .width(26.dp).height(26.dp)
                                    .cornerRadius(6.dp)
                                    .background(ColorProvider(color.copy(alpha = 0.18f))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(shiftIcon(s.typeName), style = TextStyle(fontSize = 12.sp))
                            }
                            Spacer(GlanceModifier.width(6.dp))
                            Column(GlanceModifier.defaultWeight()) {
                                Text(
                                    shiftLabel(s.typeName),
                                    style = TextStyle(
                                        fontSize   = 10.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color      = ColorProvider(WHITE)
                                    )
                                )
                                if (s.startTime != null) {
                                    Text(
                                        "${s.startTime} – ${s.endTime ?: ""}",
                                        style = TextStyle(fontSize = 8.sp, color = ColorProvider(color))
                                    )
                                }
                            }
                            Text(
                                dateLabel(s.date, short = true),
                                style = TextStyle(fontSize = 8.sp, color = ColorProvider(if (isToday) color else GREY))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── EXTRA LARGE (5×3) — полная информация + действия ─────────────
@Composable
private fun XLargeWidget(shifts: List<WidgetShift>, profile: String) {
    val today     = LocalDate.now()
    val hero      = shifts.firstOrNull { it.date == todayStr } ?: shifts.firstOrNull()
    val heroColor = if (hero != null) shiftColor(hero.typeName) else BLUE
    val weekDays  = shifts.filter { it.date != todayStr }.take(5)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BG_DARK))
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(GlanceModifier.fillMaxSize()) {
            // ── Верхняя часть: дата + герой ──
            Row(GlanceModifier.fillMaxWidth().height(140.dp)) {
                Column(
                    modifier = GlanceModifier
                        .width(130.dp)
                        .fillMaxHeight()
                        .background(ColorProvider(heroColor.copy(alpha = 0.10f)))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        today.dayOfMonth.toString(),
                        style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WHITE))
                    )
                    Text(
                        DAYS_SHORT[today.dayOfWeek.value - 1],
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(BLUE))
                    )
                    Text(
                        "${MONTHS_FULL[today.monthValue - 1]} ${today.year}",
                        style = TextStyle(fontSize = 9.sp, color = ColorProvider(GREY))
                    )

                    Spacer(GlanceModifier.height(8.dp))
                    Box(GlanceModifier.width(40.dp).height(1.dp).background(ColorProvider(heroColor.copy(alpha = 0.4f)))) {}

                    if (hero != null) {
                        Spacer(GlanceModifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.Start) {
                            Text(shiftIcon(hero.typeName), style = TextStyle(fontSize = 20.sp))
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                shiftLabel(hero.typeName),
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WHITE))
                            )
                        }
                        if (hero.startTime != null) {
                            Text(
                                "${hero.startTime} – ${hero.endTime ?: ""}",
                                style = TextStyle(fontSize = 10.sp, color = ColorProvider(heroColor))
                            )
                        }
                    } else {
                        Spacer(GlanceModifier.height(4.dp))
                        Text("Нет смены", style = TextStyle(fontSize = 10.sp, color = ColorProvider(MUTED)))
                    }
                }

                // ── Правая часть: быстрые смены ──
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    weekDays.take(5).forEach { s ->
                        val color = shiftColor(s.typeName)
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(shiftIcon(s.typeName), style = TextStyle(fontSize = 14.sp))
                            Spacer(GlanceModifier.width(6.dp))
                            Text(
                                shiftLabel(s.typeName),
                                modifier = GlanceModifier.defaultWeight(),
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = ColorProvider(WHITE))
                            )
                            Text(
                                dateLabel(s.date, short = true),
                                style = TextStyle(fontSize = 9.sp, color = ColorProvider(GREY))
                            )
                        }
                    }
                }
            }

            // ── Нижняя часть: кнопки действий ──
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(ColorProvider(CARD))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка: Добавить смену
                ActionButton(
                    icon = "➕",
                    label = "Добавить",
                    color = GREEN,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(GlanceModifier.width(8.dp))
                // Кнопка: Открыть приложение
                ActionButton(
                    icon = "📅",
                    label = "Календарь",
                    color = BLUE,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(GlanceModifier.width(8.dp))
                // Кнопка: Статистика
                ActionButton(
                    icon = "📊",
                    label = "Статистика",
                    color = PURPLE,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: String,
    label: String,
    color: Color,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .cornerRadius(10.dp)
            .background(ColorProvider(color.copy(alpha = 0.15f)))
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, style = TextStyle(fontSize = 14.sp))
            Spacer(GlanceModifier.width(4.dp))
            Text(
                label,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorProvider(color))
            )
        }
    }
}
