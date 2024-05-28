package com.example.mobileappnewv

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mobileappnewv.DAO.ErrorRating
import com.example.mobileappnewv.DAO.ErrorRatingDAO
import com.example.mobileappnewv.DAO.Rating
import com.example.mobileappnewv.DAO.RatingDAO

@Database(
    entities = [Rating::class, ErrorRating::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ratingDAO(): RatingDAO
    abstract fun errorRatingDAO(): ErrorRatingDAO

    companion object {
        private var INSTANCE: AppDatabase? = null


        // MIGRATION
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ratings ADD COLUMN exported INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE error_ratings (id INTEGER PRIMARY KEY AUTOINCREMENT, candidateId INTEGER NOT NULL, code TEXT NOT NULL, companyRating INTEGER, productRating INTEGER, priceRating INTEGER, staffRating INTEGER, status TEXT NOT NULL)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ratings ADD COLUMN error INTEGER NOT NULL DEFAULT 0")
            }
        }


        // CURRENT MIGRATION
        private val CURRENT_MIGRATION = MIGRATION_3_4


        // INIT
        fun initDb(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java, "mydatabase.db"
            ).addMigrations(this.CURRENT_MIGRATION).fallbackToDestructiveMigration().build()
        }


        // INSTANCE
        fun getInstance(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java, "mydatabase.db"
                    ).addMigrations(CURRENT_MIGRATION).fallbackToDestructiveMigration().build()
                }
            }
            return INSTANCE!!
        }
    }
}