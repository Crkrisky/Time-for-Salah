package com.crk.timeforsalah.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crk.timeforsalah.data.JamaatRule
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.*

@Composable
fun BlackChip(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F1A12))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun ColorChip(
    backgroundColor: Color = Color(0xFF0F1A12), // default if none provided
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor) // use the parameter
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
@Composable
fun SmallBlackCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A12)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) { Column(Modifier.padding(12.dp), content = content) }
}

@Composable
fun BlackTableCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A12)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun CircularCard(
    titleTop: String,
    prayerName: String,
    target: LocalDateTime?,
    ringColor: Color,
    size: Dp = 96.dp,
    stroke: Dp = 10.dp
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    val remaining = target?.let { Duration.between(now, it) }

    // Build the two-line display with unit suffixes
    val display: Pair<String, String> =
        if (remaining != null && !remaining.isNegative && !remaining.isZero) {
            val totalSec = remaining.seconds.coerceAtLeast(0)
            val h = (totalSec / 3600).toInt()
            val m = ((totalSec % 3600) / 60).toInt()
            val s = (totalSec % 60).toInt()

            if (h >= 1) {
                "${h} hr" to "${m} min"     // e.g., "1 hr" / "3 min"
            } else {
                "${m} min" to "${s} sec"    // e.g., "5 min" / "55 sec"
            }
        } else {
            "--" to "--"
        }

    // Keep your existing 0–60 min ring behavior
    val progress = if (remaining != null && !remaining.isNegative)
        1f - (remaining.seconds.coerceAtLeast(0).coerceAtMost(3600) / 3600f)
    else 1f

    Column(horizontalAlignment = Alignment.Start) {
        Text(titleTop, color = Color(0xFFB2F3B7), style = MaterialTheme.typography.labelMedium)
        Text(prayerName, color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 2.dp))
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawArc(
                    color = Color(0xFF223126),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(display.first, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(display.second, color = Color(0xFFB2F3B7), fontSize = 12.sp)
            }
        }
    }
}

@Composable fun TableHeader(text: String) =
    Text(text, color = Color(0xFFB2F3B7), style = MaterialTheme.typography.labelLarge,fontWeight = FontWeight.Bold,fontSize = 20.sp)

@Composable fun DividerTiny() =
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF26352B)))

@Composable
fun TableRow(name: String, nameColor: Color, azan: String, jamaat: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = nameColor, fontWeight = FontWeight.SemiBold)
        Text(azan, color = Color(0xFFE7FFE9))
        Text(jamaat, color = Color(0xFFE7FFE9))
    }
}

/* ---- Time helpers ---- */

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

fun formatTime(t: LocalDateTime?): String =
    t?.format(timeFormatter) ?: "—"

fun formatFixed(h12: Int, m: Int, pm: Boolean): String {
    val h = h12.coerceIn(1, 12)
    val min = m.coerceIn(0, 59)
    val ampm = if (pm) "PM" else "AM"
    return "%d:%02d %s".format(h, min, ampm)
}

fun fixedToDateTime(rule: JamaatRule): LocalDateTime {
    val h12 = rule.fixedHour12.coerceIn(1, 12)
    val h24 = (h12 % 12) + if (rule.fixedIsPm) 12 else 0
    val minute = rule.fixedMinute.coerceIn(0, 59)
    return LocalDate.now().atTime(h24, minute)
}

fun formatHijri(hijriAdjustDays: Int): String {
    val base = java.time.chrono.HijrahChronology.INSTANCE.dateNow()
    val adjusted = base.plus(hijriAdjustDays.toLong(), ChronoUnit.DAYS)
    val d = adjusted.get(ChronoField.DAY_OF_MONTH)
    val m = adjusted.get(ChronoField.MONTH_OF_YEAR)
    val y = adjusted.get(ChronoField.YEAR)
    val month = listOf(
        "", "Muharram","Safar","Rabiʿ I","Rabiʿ II",
        "Jumada I","Jumada II","Rajab","Shaʿban",
        "Ramadan","Shawwal","Dhu al-Qaʿdah","Dhu al-Ḥijjah"
    )[m]
    return "$d $month, $y"
}
