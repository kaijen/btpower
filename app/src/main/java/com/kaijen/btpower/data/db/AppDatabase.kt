package com.kaijen.btpower.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kaijen.btpower.data.db.entities.DeviceEntity
import com.kaijen.btpower.data.db.entities.SampleEntity
import com.kaijen.btpower.data.db.entities.SessionEntity

@Database(
    entities = [
        DeviceEntity::class,
        SessionEntity::class,
        SampleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun sessionDao(): SessionDao
    abstract fun sampleDao(): SampleDao
}
