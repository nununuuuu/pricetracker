package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        MonitorRuleEntity::class,
        ProductEntity::class,
        PriceHistoryEntity::class,
        AnomalyReportEntity::class,
        NotificationLogEntity::class,
        PlatformStatusEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitorRuleDao(): MonitorRuleDao
    abstract fun productDao(): ProductDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun anomalyReportDao(): AnomalyReportDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun platformStatusDao(): PlatformStatusDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "deal_radar_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
