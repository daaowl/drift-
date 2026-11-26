package app.sukoon.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.sukoon.data.backup.BackupRepositoryImpl
import app.sukoon.data.local.ContentDao
import app.sukoon.data.local.DuaDao
import app.sukoon.data.local.Migrations
import app.sukoon.data.local.SukoonDatabase
import app.sukoon.data.repository.DuaRepositoryImpl
import app.sukoon.data.repository.SecurityRepositoryImpl
import app.sukoon.data.repository.SettingsRepositoryImpl
import app.sukoon.data.repository.UserDataRepositoryImpl
import app.sukoon.domain.repository.BackupRepository
import app.sukoon.domain.repository.DuaRepository
import app.sukoon.domain.repository.SecurityRepository
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.domain.repository.UserDataRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton @ApplicationScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }
}

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SukoonDatabase =
        Room.databaseBuilder(context, SukoonDatabase::class.java, SukoonDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides fun provideContentDao(db: SukoonDatabase): ContentDao = db.contentDao()
    @Provides fun provideDuaDao(db: SukoonDatabase): DuaDao = db.duaDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("sukoon_settings") },
        )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindDuaRepository(impl: DuaRepositoryImpl): DuaRepository
    @Binds abstract fun bindUserDataRepository(impl: UserDataRepositoryImpl): UserDataRepository
    @Binds abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository
    @Binds abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
