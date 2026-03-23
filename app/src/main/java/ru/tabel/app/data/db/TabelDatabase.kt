package ru.tabel.app.data.db

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.tabel.app.data.model.*

class Converters {
    @TypeConverter fun fromShiftType(t: ShiftType): String = t.name
    @TypeConverter fun toShiftType(v: String): ShiftType =
        ShiftType.entries.find { it.name == v } ?: ShiftType.DAY
}

@Database(
    entities = [ShiftEntry::class, Profile::class, ShiftTime::class, AppSettings::class, ShiftTemplate::class],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TabelDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun profileDao(): ProfileDao
    abstract fun shiftTimeDao(): ShiftTimeDao
    abstract fun settingsDao(): SettingsDao
    abstract fun shiftTemplateDao(): ShiftTemplateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'DARK'")
                db.execSQL("ALTER TABLE settings ADD COLUMN cloudBackupEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE settings ADD COLUMN cloudBackupUri TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) { /* no-op */ }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN sickCoeff REAL NOT NULL DEFAULT 0.6")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN notifDayTime TEXT NOT NULL DEFAULT '19:00'")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS settings_new (
                        id INTEGER NOT NULL PRIMARY KEY,
                        activeProfileId TEXT NOT NULL DEFAULT 'default',
                        themeMode TEXT NOT NULL DEFAULT 'SYSTEM',
                        fontScale REAL NOT NULL DEFAULT 1.0,
                        hourlyRate REAL NOT NULL DEFAULT 300.0,
                        nightCoeff REAL NOT NULL DEFAULT 1.5,
                        holidayCoeff REAL NOT NULL DEFAULT 2.0,
                        sickCoeff REAL NOT NULL DEFAULT 0.6,
                        notifHoursBefore INTEGER NOT NULL DEFAULT 0,
                        notifSound TEXT NOT NULL DEFAULT 'default',
                        dynamicColor INTEGER NOT NULL DEFAULT 0,
                        cloudBackupEnabled INTEGER NOT NULL DEFAULT 0,
                        cloudBackupUri TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO settings_new (
                        id, activeProfileId, themeMode, fontScale,
                        hourlyRate, nightCoeff, holidayCoeff, sickCoeff,
                        notifSound, cloudBackupEnabled, cloudBackupUri
                    )
                    SELECT
                        id, activeProfileId, themeMode, fontScale,
                        hourlyRate, nightCoeff, holidayCoeff,
                        COALESCE(sickCoeff, 0.6),
                        COALESCE(notifSound, 'default'),
                        COALESCE(cloudBackupEnabled, 0),
                        COALESCE(cloudBackupUri, '')
                    FROM settings
                """.trimIndent())
                db.execSQL("DROP TABLE settings")
                db.execSQL("ALTER TABLE settings_new RENAME TO settings")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS settings_new (
                        id INTEGER NOT NULL PRIMARY KEY,
                        activeProfileId TEXT NOT NULL DEFAULT 'default',
                        themeMode TEXT NOT NULL DEFAULT 'SYSTEM',
                        fontScale REAL NOT NULL DEFAULT 1.0,
                        hourlyRate REAL NOT NULL DEFAULT 300.0,
                        nightCoeff REAL NOT NULL DEFAULT 1.5,
                        holidayCoeff REAL NOT NULL DEFAULT 2.0,
                        sickCoeff REAL NOT NULL DEFAULT 0.6,
                        notifHoursBefore INTEGER NOT NULL DEFAULT 0,
                        notifSound TEXT NOT NULL DEFAULT 'default',
                        dynamicColor INTEGER NOT NULL DEFAULT 0,
                        cloudBackupEnabled INTEGER NOT NULL DEFAULT 0,
                        cloudBackupUri TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO settings_new
                    SELECT
                        id, activeProfileId, themeMode, fontScale,
                        hourlyRate, nightCoeff, holidayCoeff,
                        COALESCE(sickCoeff, 0.6),
                        COALESCE(notifHoursBefore, 0),
                        COALESCE(notifSound, 'default'),
                        COALESCE(dynamicColor, 0),
                        COALESCE(cloudBackupEnabled, 0),
                        COALESCE(cloudBackupUri, '')
                    FROM settings
                """.trimIndent())
                db.execSQL("DROP TABLE settings")
                db.execSQL("ALTER TABLE settings_new RENAME TO settings")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN breakMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }
        // v8 → v9: добавляем таблицу шаблонов
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS shift_templates (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        pattern TEXT NOT NULL,
                        workDays INTEGER NOT NULL DEFAULT 0,
                        restDays INTEGER NOT NULL DEFAULT 0,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        profileId TEXT NOT NULL DEFAULT 'default'
                    )
                """.trimIndent())
            }
        }
        // v9 → v10: добавляем поля автобекапа
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "settings", "autoBackupEnabled", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfNotExists(db, "settings", "autoBackupFrequency", "INTEGER NOT NULL DEFAULT 7")
            }
        }
        // v10 → v11: добавляем поле locked для защиты смен
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "shifts", "locked", "INTEGER NOT NULL DEFAULT 0")
            }
        }
        // v11 → v12: добавляем fontLocked
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "settings", "fontLocked", "INTEGER NOT NULL DEFAULT 0")
            }
        }
        // v12 → v13: добавляем isLocked (защита данных)
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "settings", "isLocked", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        private fun addColumnIfNotExists(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
            val cursor = db.query("PRAGMA table_info($table)")
            val columns = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex >= 0) {
                    columns.add(cursor.getString(nameIndex))
                }
            }
            cursor.close()
            if (column !in columns) {
                db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            }
        }
    }
}
