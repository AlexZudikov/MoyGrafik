package ru.tabel.app.ui.stats

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import ru.tabel.app.data.model.ShiftEntry
import ru.tabel.app.data.model.ShiftType
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PdfExporter {

    private val MONTHS = listOf(
        "Январь","Февраль","Март","Апрель","Май","Июнь",
        "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь"
    )
    private val DOW_SHORT = listOf("Вс","Пн","Вт","Ср","Чт","Пт","Сб")

    fun exportAndShare(
        context: Context,
        shifts: List<ShiftEntry>,
        profileName: String,
        year: Int,
        month: Int
    ) {
        val uri = buildPdf(context, shifts, profileName, year, month) ?: run {
            android.widget.Toast.makeText(context, "Ошибка создания PDF", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "График смен ${MONTHS[month-1]} $year — $profileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Экспорт PDF"))
    }

    fun buildPdf(
        context: Context,
        shifts: List<ShiftEntry>,
        profileName: String,
        year: Int,
        month: Int
    ): Uri? = runCatching {
        val pageW = 595; val pageH = 842
        val doc  = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = doc.startPage(info)
        draw(page.canvas, shifts.sortedBy { it.date }, profileName, year, month, pageW)
        doc.finishPage(page)

        // Используем getExternalFilesDir — не требует разрешений и всегда доступна
        val dir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
            "МойГрафик"
        ).also { it.mkdirs() }

        // Если внешнее хранилище недоступно — fallback на cacheDir
        val saveDir = if (dir.exists()) dir
                      else File(context.cacheDir, "pdf_export").also { it.mkdirs() }

        val safeName = profileName.replace(Regex("[^а-яёА-ЯЁa-zA-Z0-9_]"), "_")
        val name = "МойГрафик_${safeName}_${year}_${month.toString().padStart(2,'0')}.pdf"
        val file = File(saveDir, name)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()

        // URI через FileProvider для безопасной передачи другим приложениям
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }.onFailure {
        android.util.Log.e("PdfExporter", "buildPdf failed: ${it.message}", it)
    }.getOrNull()

    private fun draw(c: Canvas, shifts: List<ShiftEntry>, profile: String, year: Int, month: Int, w: Int) {
        val mg = 36f  // margin

        // ── Краски ────────────────────────────────────────────
        fun boldPaint(size: Float, color: Int = Color.parseColor("#1A1A2E")) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface  = Typeface.DEFAULT_BOLD; textSize = size; this.color = color
            }
        fun normalPaint(size: Float, color: Int = Color.parseColor("#333355")) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface  = Typeface.DEFAULT; textSize = size; this.color = color
            }
        fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL; this.color = color
        }

        val blue   = Color.parseColor("#4F6EF7")
        val muted  = Color.parseColor("#9090AA")
        val light  = Color.parseColor("#F0F0FF")
        val divClr = Color.parseColor("#E0E0EE")

        // ── Шапка ─────────────────────────────────────────────
        c.drawRect(0f, 0f, w.toFloat(), 72f, fillPaint(blue))
        c.drawText("Мой График", mg, 36f, boldPaint(20f, Color.WHITE))
        c.drawText("$profile  ·  ${MONTHS[month-1]} $year", mg, 56f, normalPaint(11f, Color.parseColor("#C0C8FF")))
        // Дата экспорта справа
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val datePaint = normalPaint(9f, Color.parseColor("#C0C8FF"))
        c.drawText("Экспорт: $today", w - mg - 100f, 56f, datePaint)

        // ── Сводка ────────────────────────────────────────────
        var y = 90f
        val work  = shifts.count { it.type in listOf(ShiftType.DAY, ShiftType.NIGHT, ShiftType.SLEEP, ShiftType.HOLIDAY) }
        val off   = shifts.count { it.type == ShiftType.OFF }
        val night = shifts.count { it.type == ShiftType.NIGHT }
        val sick  = shifts.count { it.type == ShiftType.SICK || it.type == ShiftType.VACATION }

        c.drawRoundRect(mg, y, w - mg, y + 44f, 8f, 8f, fillPaint(light))
        c.drawText("Рабочих: $work", mg + 12f, y + 28f, boldPaint(10f, blue))
        c.drawText("Ночных: $night", mg + 115f, y + 28f, boldPaint(10f, Color.parseColor("#7C3AED")))
        c.drawText("Выходных: $off", mg + 215f, y + 28f, boldPaint(10f, Color.parseColor("#16A34A")))
        c.drawText("Больн./Отпуск: $sick", mg + 325f, y + 28f, boldPaint(10f, Color.parseColor("#EA580C")))
        c.drawText("Всего: ${shifts.size}", mg + 450f, y + 28f, boldPaint(10f, Color.parseColor("#1A1A2E")))
        y += 56f

        // ── Заголовок таблицы ─────────────────────────────────
        c.drawRect(mg, y, w - mg, y + 22f, fillPaint(blue))
        val hdr = boldPaint(9f, Color.WHITE)
        c.drawText("ДАТА",       mg + 4f,   y + 15f, hdr)
        c.drawText("ДН",         mg + 60f,  y + 15f, hdr)
        c.drawText("ТИП СМЕНЫ",  mg + 82f,  y + 15f, hdr)
        c.drawText("ВРЕМЯ",      mg + 210f, y + 15f, hdr)
        c.drawText("ЗАМЕТКА",    mg + 295f, y + 15f, hdr)
        y += 22f

        // ── Строки ────────────────────────────────────────────
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        shifts.forEachIndexed { i, e ->
            if (y > 800f) return

            // Зебра
            if (i % 2 == 0) c.drawRect(mg, y, w - mg, y + 18f, fillPaint(Color.parseColor("#F8F8FF")))

            val date = runCatching { LocalDate.parse(e.date, fmt) }.getOrNull()
            val dow  = date?.let { DOW_SHORT[it.dayOfWeek.value % 7] } ?: ""

            // Цвет типа
            val typeColor = runCatching {
                (0xFF000000L or (e.type.color and 0xFFFFFFFFL)).toInt()
            }.getOrDefault(blue)

            val rowPaint = normalPaint(9f)
            c.drawText(e.date,       mg + 4f,   y + 13f, rowPaint)
            c.drawText(dow,          mg + 60f,  y + 13f, rowPaint)

            // Цветной квадратик
            c.drawRoundRect(mg + 80f, y + 4f, mg + 90f, y + 14f, 2f, 2f, fillPaint(typeColor))
            c.drawText(e.type.label, mg + 94f,  y + 13f, normalPaint(9f, typeColor))

            val timeStr = when {
                e.customStartTime != null -> e.customStartTime!!
                e.type == ShiftType.DAY     -> "08:00"
                e.type == ShiftType.NIGHT   -> "20:00"
                e.type == ShiftType.HOLIDAY -> "08:00"
                else -> "—"
            }
            c.drawText(timeStr, mg + 210f, y + 13f, rowPaint)

            val note = if (e.note.length > 35) e.note.take(35) + "…" else e.note
            c.drawText(note, mg + 295f, y + 13f, normalPaint(8f, Color.parseColor("#666688")))

            // Разделитель
            c.drawLine(mg, y + 18f, w - mg, y + 18f,
                Paint().apply { color = divClr; strokeWidth = 0.5f })
            y += 18f
        }

        // ── Итоговая линия ────────────────────────────────────
        c.drawLine(mg, y + 4f, w - mg, y + 4f,
            Paint().apply { color = blue; strokeWidth = 1f })
        c.drawText("Итого записей: ${shifts.size}", mg, y + 18f, boldPaint(9f, blue))

        // ── Подвал ────────────────────────────────────────────
        c.drawRect(0f, 820f, w.toFloat(), 842f, fillPaint(Color.parseColor("#F0F0FF")))
        c.drawText("Сгенерировано приложением «Мой График»", mg, 834f, normalPaint(8f, muted))
    }
}
