package ru.tabel.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.tabel.app.data.model.*

// ── DAO для смен ──────────────────────────────────────────────
@Dao
interface ShiftDao {

    @Query("SELECT * FROM shifts WHERE profileId = :profileId ORDER BY date ASC")
    fun getShiftsForProfile(profileId: String): Flow<List<ShiftEntry>>

    @Query("SELECT * FROM shifts WHERE profileId = :profileId AND date LIKE :monthPrefix || '%'")
    fun getShiftsForMonth(profileId: String, monthPrefix: String): Flow<List<ShiftEntry>>

    @Query("SELECT * FROM shifts WHERE profileId = :profileId AND date = :date")
    suspend fun getShiftForDate(profileId: String, date: String): ShiftEntry?

    @Query("SELECT * FROM shifts WHERE profileId = :profileId AND date >= :fromDate ORDER BY date ASC")
    fun getUpcomingShifts(profileId: String, fromDate: String): Flow<List<ShiftEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<ShiftEntry>)

    @Delete
    suspend fun deleteShift(shift: ShiftEntry)

    @Query("DELETE FROM shifts WHERE profileId = :profileId AND date LIKE :monthPrefix || '%'")
    suspend fun deleteShiftsForMonth(profileId: String, monthPrefix: String)

    @Query("DELETE FROM shifts WHERE profileId = :profileId")
    suspend fun deleteAllShiftsForProfile(profileId: String)
}

// ── DAO для профилей ──────────────────────────────────────────
@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun activateProfile(id: String)
}

// ── DAO для настроек времени ──────────────────────────────────
@Dao
interface ShiftTimeDao {

    @Query("SELECT * FROM shift_times")
    fun getAllTimes(): Flow<List<ShiftTime>>

    @Query("SELECT * FROM shift_times WHERE type = :type")
    suspend fun getTimeForType(type: ShiftType): ShiftTime?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTime(time: ShiftTime)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimes(times: List<ShiftTime>)
}

// ── DAO для настроек приложения ───────────────────────────────
@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)

    @Query("UPDATE settings SET activeProfileId = :id WHERE id = 1")
    suspend fun setActiveProfile(id: String)
}
