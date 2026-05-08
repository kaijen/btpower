package com.kaijen.btpower.di

import android.content.Context
import androidx.room.Room
import com.kaijen.btpower.data.db.AppDatabase
import com.kaijen.btpower.data.db.DeviceDao
import com.kaijen.btpower.data.db.SampleDao
import com.kaijen.btpower.data.db.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bt-power.db").build()

    @Provides
    fun provideDeviceDao(db: AppDatabase): DeviceDao = db.deviceDao()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideSampleDao(db: AppDatabase): SampleDao = db.sampleDao()
}
