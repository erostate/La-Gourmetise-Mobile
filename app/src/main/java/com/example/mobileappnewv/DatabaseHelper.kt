import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mydatabase.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Créer les tables de la base de données
        db.execSQL("CREATE TABLE ratings (id INTEGER PRIMARY KEY, candidate_id INTEGER, code TEXT, company_rating INTEGER, product_rating INTEGER, price_rating INTEGER, staff_rating INTEGER, exported INTEGER DEFAULT 0, date TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Mettre à jour la base de données si nécessaire
        db.execSQL("DROP TABLE IF EXISTS ratings")
        onCreate(db)
    }
}
