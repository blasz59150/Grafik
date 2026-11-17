package pl.sparta.kalendarz

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val Navy = Color(0xFF23364D)
private val Sand = Color(0xFFF4F0E8)
private val Red = Color(0xFFB83A43)
private val Gold = Color(0xFFC49A45)

class LocalStore(context: android.content.Context) :
    SQLiteOpenHelper(context, "kalendarz.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE day_entry(
            date TEXT PRIMARY KEY, absence TEXT NOT NULL DEFAULT '',
            note TEXT NOT NULL DEFAULT '', override_shift INTEGER NOT NULL DEFAULT 0
        )""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun get(date: String): Triple<String, String, Int> {
        readableDatabase.rawQuery(
            "SELECT absence,note,override_shift FROM day_entry WHERE date=?",
            arrayOf(date)
        ).use { c ->
            if (c.moveToFirst()) return Triple(c.getString(0), c.getString(1), c.getInt(2))
        }
        return Triple("", "", 0)
    }
    fun save(date: String, absence: String, note: String, overrideShift: Int) {
        val v = ContentValues().apply {
            put("date", date); put("absence", absence); put("note", note)
            put("override_shift", overrideShift)
        }
        writableDatabase.insertWithOnConflict("day_entry", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LocalStore(this)
        setContent { MaterialTheme { CalendarApp(store) } }
    }
}

private fun shiftFor(date: LocalDate, base: LocalDate, squad: Int): Int {
    // Potwierdzony cykl: 1-4 zm.1, 5 wolne, 6-9 zm.3,
    // 10-11 wolne, 12-15 zm.2, 16 wolne.
    val start = base.plusDays((squad - 1L) * 4L)
    val day = Math.floorMod(ChronoUnit.DAYS.between(start, date), 16L).toInt() + 1
    return when (day) {
        in 1..4 -> 1
        in 6..9 -> 3
        in 12..15 -> 2
        else -> 0
    }
}

@Composable
fun CalendarApp(store: LocalStore) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var squad by remember { mutableIntStateOf(1) }
    var view by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val base = LocalDate.of(2026, 11, 13)

    Surface(Modifier.fillMaxSize(), color = Sand) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("KALENDARZ BRYGADOWY", color = Navy, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text("Wersja testowa 0.1 • dane lokalne", color = Navy.copy(alpha = .65f))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, "Poprzedni miesiąc")
                }
                Text("${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}",
                    Modifier.weight(1f), color = Navy, fontWeight = FontWeight.Bold)
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, "Następny miesiąc")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Moja brygada", "Wszystkie", "Z notatkami").forEachIndexed { i, label ->
                    FilterChip(selected = view == i, onClick = { view = i }, label = { Text(label) })
                }
            }
            if (view != 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brygada:", color = Navy)
                    Spacer(Modifier.width(8.dp))
                    (1..4).forEach { n ->
                        FilterChip(selected = squad == n, onClick = { squad = n },
                            label = { Text(n.toString()) })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround) {
                listOf("Pn", "Wt", "Śr", "Cz", "Pt", "Sb", "Nd").forEach {
                    Text(it, color = Navy, fontWeight = FontWeight.Bold)
                }
            }
            val first = month.atDay(1)
            val offset = first.dayOfWeek.value - 1
            val cells = buildList<LocalDate?> {
                repeat(offset) { add(null) }
                for (d in 1..month.lengthOfMonth()) add(month.atDay(d))
            }
            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items(cells) { date ->
                    if (date == null) Spacer(Modifier.height(62.dp))
                    else {
                        val squads = if (view == 1) 1..4 else squad..squad
                        val info = store.get(date.toString())
                        val original = shiftFor(date, base, squad)
                        val actual = if (info.third in 1..3) info.third else original
                        val label = when {
                            info.first.isNotBlank() -> info.first + if (original > 0) " ($original)" else " (-)"
                            actual > 0 -> actual.toString()
                            else -> "·"
                        }
                        Column(
                            Modifier.fillMaxWidth().height(62.dp)
                                .background(if (date == LocalDate.now()) Gold.copy(alpha=.28f) else Color.White,
                                    RoundedCornerShape(10.dp))
                                .clickable { selected = date }.padding(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(date.dayOfMonth.toString(), color = Navy)
                            Text(if (view == 1) (1..4).joinToString("/") {
                                val s = shiftFor(date, base, it)
                                if (s == 0) "–" else s.toString()
                            } else label, color = if (info.first.isNotBlank()) Red else Navy,
                                fontWeight = FontWeight.Bold)
                            if (view == 2 && info.second.isNotBlank())
                                Text("📝", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Text("Dotknij dnia, aby dodać nieobecność lub notatkę.", color = Navy.copy(alpha=.7f))
        }
    }
    selected?.let { date ->
        val old = store.get(date.toString())
        var absence by remember(date) { mutableStateOf(old.first) }
        var note by remember(date) { mutableStateOf(old.second) }
        var override by remember(date) { mutableStateOf(old.third.toString()) }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(date.toString()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rodzaj nieobecności")
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(absence.ifBlank { "Brak" })
                        }
                        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                            listOf("", "U", "UŻ", "CH", "OP", "SW", "SZ", "DW").forEach {
                                DropdownMenuItem(text = { Text(it.ifBlank { "Brak" }) },
                                    onClick = { absence = it; expanded = false })
                            }
                        }
                    }
                    OutlinedTextField(value = override, onValueChange = { override = it.filter(Char::isDigit).take(1) },
                        label = { Text("Zmiana wyjątkowa (1–3, puste = bez zmiany)") })
                    OutlinedTextField(value = note, onValueChange = { note = it },
                        label = { Text("Notatka") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.save(date.toString(), absence, note, override.toIntOrNull() ?: 0)
                    refresh++; selected = null
                }) { Text("Zapisz") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Anuluj") } }
        )
    }
}
