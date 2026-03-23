package ru.tabel.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.tabel.app.data.db.*
import ru.tabel.app.data.repository.TabelRepository
import ru.tabel.app.notifications.TabelNotificationManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): TabelDatabase =
        Room.databaseBuilder(ctx, TabelDatabase::class.java, "tabel.db")
            .addMigrations(TabelDatabase.MIGRATION_1_2, TabelDatabase.MIGRATION_2_3, TabelDatabase.MIGRATION_3_4, TabelDatabase.MIGRATION_4_5, TabelDatabase.MIGRATION_5_6, TabelDatabase.MIGRATION_6_7, TabelDatabase.MIGRATION_7_8, TabelDatabase.MIGRATION_8_9, TabelDatabase.MIGRATION_9_10, TabelDatabase.MIGRATION_10_11, TabelDatabase.MIGRATION_11_12, TabelDatabase.MIGRATION_12_13)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideShiftDao(db: TabelDatabase)     = db.shiftDao()
    @Provides fun provideProfileDao(db: TabelDatabase)   = db.profileDao()
    @Provides fun provideShiftTimeDao(db: TabelDatabase) = db.shiftTimeDao()
    @Provides fun provideSettingsDao(db: TabelDatabase)  = db.settingsDao()
    @Provides fun provideShiftTemplateDao(db: TabelDatabase) = db.shiftTemplateDao()

    @Provides @Singleton
    fun provideRepository(
        shiftDao: ShiftDao, profileDao: ProfileDao,
        shiftTimeDao: ShiftTimeDao, settingsDao: SettingsDao,
        shiftTemplateDao: ShiftTemplateDao
    ) = TabelRepository(shiftDao, profileDao, shiftTimeDao, settingsDao, shiftTemplateDao)

    @Provides @Singleton
    fun provideNotificationManager(@ApplicationContext ctx: Context) =
        TabelNotificationManager(ctx)
}
