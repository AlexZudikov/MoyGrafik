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

private val SIZE_SMALL  = DpSize(110.dp,  50.dp)
private val SIZE_MEDIUM = DpSize(110.dp, 110.dp)
private val SIZE_LARGE  = DpSize(230.dp, 110.dp)

class ShiftWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget()
}

data class WidgetShift(val date: String, val typeName: String, val startTime: String?)

class ShiftWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE))

    companion object {
        const val PREFS       = "tabel_widget"
        const val KEY_PROFILE = "profile"
        const val MAX_SHIFTS  = 5
        fun kDate(i: Int) = "date_$i"
        fun kType(i: Int) = "type_$i"
        fun kTime(i: Int) = "time_$i"
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val p       = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val profile = p.getString(KEY_PROFILE, "Мой График") ?: "Мой График"
        val shifts  = (0 until MAX_SHIFTS).mapNotNull { i ->
            val date = p.getString(kDate(i), null) ?: return@mapNotNull null
            val type = p.getString(kType(i), null) ?: return@mapNotNull null
            WidgetShift(date, type, p.getString(kTime(i), null))
        }
        provideContent {
            val size = LocalSize.current
            when {
                size.width  >= SIZE_LARGE.width   -> LargeWidget(shifts, profile)
                size.height >= SIZE_MEDIUM.height -> MediumWidget(shifts, profile)
                else                              -> SmallWidget(shifts)
            }
        }
    }
}

// ── Утилиты ───────────────────────────────────────────────────
private val MONTHS_SHORT = listOf(
    "янв","фев","мар","апр","май","июн",
    "июл","авг","сен","окт","ноя","дек"
)
private val MONTHS_FULL = listOf(
    "января","февраля","марта","апреля","мая","июня",
    "июля","августа","сентября","октября","ноября","декабря"
)
// DayOfWeek: 1=Пн … 7=Вс → индекс 0..6
private val DAYS_SHORT = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")

private val todayStr    get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
private val tomorrowStr get() = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun dateLabel(date: String) = when (date) {
    todayStr    -> "Сегодня"
    tomorrowStr -> "Завтра"
    else -> runCatching {
        LocalDate.parse(date).let { "${it.dayOfMonth} ${MONTHS_SHORT[it.monthValue - 1]}" }
    }.getOrDefault(date)
}

private fun shiftColor(t: String) =
    Color(runCatching { ShiftType.valueOf(t).color }.getOrDefault(0xFF555577))
private fun shiftIcon(t: String) =
    runCatching { ShiftType.valueOf(t).icon }.getOrDefault("📋")
private fun shiftLabel(t: String) =
    runCatching { ShiftType.valueOf(t).label }.getOrDefault(t)

// Короткое название для маленького виджета (макс 4 символа)
private fun shiftShortLabel(t: String) = when (
    runCatching { ShiftType.valueOf(t) }.getOrNull()
) {
    ShiftType.DAY      -> "День"
    ShiftType.NIGHT    -> "Ночь"
    ShiftType.SLEEP    -> "Отсып"
    ShiftType.OFF      -> "Вых"
    ShiftType.HOLIDAY  -> "Празд"
    ShiftType.SICK     -> "Болн"
    ShiftType.VACATION -> "Отпуск"
    null               -> t.take(5)
}

// Палитра
private val BG    = Color(0xFF0D0D1C)
private val CARD  = Color(0xFF181828)
private val WHITE = Color(0xFFFFFFFF)
private val GREY  = Color(0xFF9090AA)
private val MUTED = Color(0xFF505068)
private val BLUE  = Color(0xFF4F6EF7)

// ── Общий заголовок с датой ────────────────────────────────────
// "13" большое + "Пт" + "марта 2026"
@Composable
private fun DateHeader(compact: Boolean = false) {
    val d   = LocalDate.now()
    val dow = DAYS_SHORT[d.dayOfWeek.value - 1]
    val mon = MONTHS_FULL[d.monthValue - 1]

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            d.dayOfMonth.toString(),
            style = TextStyle(
                fontSize   = if (compact) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color      = ColorProvider(WHITE)
            )
        )
        Spacer(GlanceModifier.width(5.dp))
        Column {
            Text(
                dow,
                style = TextStyle(
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ColorProvider(BLUE)
                )
            )
            Text(
                "$mon ${d.year}",
                style = TextStyle(fontSize = 8.sp, color = ColorProvider(GREY))
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  SMALL (2×1) — дата + ближайшая смена
// ══════════════════════════════════════════════════════════════
@Composable
private fun SmallWidget(shifts: List<WidgetShift>) {
    val next  = shifts.firstOrNull()
    val color = if (next != null) shiftColor(next.typeName) else BLUE

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BG))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.CenterStart
    ) {
        // Цветная полоса слева
        Box(GlanceModifier.fillMaxHeight().width(4.dp).background(ColorProvider(color))) {}

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Дата
            DateHeader(compact = true)

            Spacer(GlanceModifier.width(10.dp))

            // Разделитель
            Box(GlanceModifier.width(1.dp).height(30.dp).background(ColorProvider(MUTED))) {}

            Spacer(GlanceModifier.width(10.dp))

            // Смена
            if (next == null) {
                Text("Нет смен", style = TextStyle(fontSize = 11.sp, color = ColorProvider(GREY)))
            } else {
                Column {
                    // Иконка + короткое название в одну строку — не переносится
                    Text(
                        "${shiftIcon(next.typeName)} ${shiftShortLabel(next.typeName)}",
                        style = TextStyle(
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = ColorProvider(WHITE)
                        )
                    )
                    Text(
                        dateLabel(next.date) + (next.startTime?.let { " $it" } ?: ""),
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = ColorProvider(if (next.date == todayStr) color else GREY)
                        )
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  MEDIUM (2×2) — дата + 3 смены
// ══════════════════════════════════════════════════════════════
@Composable
private fun MediumWidget(shifts: List<WidgetShift>, profile: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BG))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(GlanceModifier.fillMaxSize().padding(12.dp)) {

            // Заголовок: дата + профиль
            Row(
                GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateHeader(compact = true)
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    profile,
                    style = TextStyle(fontSize = 9.sp, color = ColorProvider(MUTED))
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            if (shifts.isEmpty()) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Нет предстоящих смен",
                        style = TextStyle(fontSize = 11.sp, color = ColorProvider(MUTED))
                    )
                }
            } else {
                shifts.take(3).forEach { s ->
                    val color   = shiftColor(s.typeName)
                    val isToday = s.date == todayStr
                    Spacer(GlanceModifier.height(4.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(10.dp)
                            .background(ColorProvider(
                                if (isToday) color.copy(alpha = 0.20f) else CARD
                            ))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            GlanceModifier.width(7.dp).height(7.dp)
                                .cornerRadius(4.dp).background(ColorProvider(color))
                        ) {}
                        Spacer(GlanceModifier.width(7.dp))
                        Text(
                            shiftLabel(s.typeName),
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                fontSize   = 11.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color      = ColorProvider(WHITE)
                            )
                        )
                        Text(
                            dateLabel(s.date),
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = ColorProvider(if (isToday) color else GREY)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  LARGE (4×2) — дата-панель + список смен
// ══════════════════════════════════════════════════════════════
@Composable
private fun LargeWidget(shifts: List<WidgetShift>, profile: String) {
    val hero      = shifts.firstOrNull { it.date == todayStr } ?: shifts.firstOrNull()
    val heroColor = if (hero != null) shiftColor(hero.typeName) else BLUE
    val listItems = shifts.filter { it != hero }.take(4)
    val today     = LocalDate.now()
    val dow       = DAYS_SHORT[today.dayOfWeek.value - 1]

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BG))
            .cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(GlanceModifier.fillMaxSize()) {

            // ── Левая панель: дата + сегодняшняя/ближайшая смена ──
            Column(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(ColorProvider(heroColor.copy(alpha = 0.15f)))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Число крупно
                Text(
                    today.dayOfMonth.toString(),
                    style = TextStyle(
                        fontSize = 36.sp, fontWeight = FontWeight.Bold,
                        color = ColorProvider(WHITE)
                    )
                )
                // День недели
                Text(
                    dow,
                    style = TextStyle(
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = ColorProvider(BLUE)
                    )
                )
                // Месяц + год
                Text(
                    "${MONTHS_FULL[today.monthValue - 1]} ${today.year}",
                    style = TextStyle(fontSize = 8.sp, color = ColorProvider(GREY))
                )

                Spacer(GlanceModifier.height(10.dp))

                // Смена сегодня
                if (hero != null) {
                    Box(
                        GlanceModifier
                            .width(32.dp).height(1.dp)
                            .background(ColorProvider(heroColor.copy(alpha = 0.4f)))
                    ) {}
                    Spacer(GlanceModifier.height(8.dp))
                    Text(shiftIcon(hero.typeName), style = TextStyle(fontSize = 22.sp))
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        shiftLabel(hero.typeName),
                        style = TextStyle(
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = ColorProvider(WHITE)
                        )
                    )
                    if (hero.startTime != null) {
                        Text(
                            hero.startTime,
                            style = TextStyle(fontSize = 10.sp, color = ColorProvider(heroColor))
                        )
                    }
                } else {
                    Text(
                        "Нет смен",
                        style = TextStyle(fontSize = 10.sp, color = ColorProvider(MUTED))
                    )
                }

                Spacer(GlanceModifier.defaultWeight())
                Text(
                    "📅 $profile",
                    style = TextStyle(fontSize = 8.sp, color = ColorProvider(MUTED))
                )
            }

            // ── Правая панель: список смен ─────────────────────
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    "Расписание",
                    style = TextStyle(
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = ColorProvider(GREY)
                    )
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
                        val color = shiftColor(s.typeName)
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .width(28.dp).height(28.dp)
                                    .cornerRadius(8.dp)
                                    .background(ColorProvider(color.copy(alpha = 0.20f))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(shiftIcon(s.typeName), style = TextStyle(fontSize = 12.sp))
                            }
                            Spacer(GlanceModifier.width(7.dp))
                            Column(GlanceModifier.defaultWeight()) {
                                Text(
                                    shiftLabel(s.typeName),
                                    style = TextStyle(
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        color = ColorProvider(WHITE)
                                    )
                                )
                                if (s.startTime != null) {
                                    Text(
                                        s.startTime,
                                        style = TextStyle(fontSize = 9.sp, color = ColorProvider(color))
                                    )
                                }
                            }
                            Text(
                                dateLabel(s.date),
                                style = TextStyle(fontSize = 9.sp, color = ColorProvider(GREY))
                            )
                        }
                    }
                }
            }
        }
    }
}
