package com.example.bedtime

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ComponentActivity
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.bedtime.ui.theme.BedTimeTheme
@Entity
data class Bedtime(
    @PrimaryKey(autoGenerate = false) val id: Int,
    val hour: Int,
    val minute: Int,
    val wifiBlocking: Boolean
)
@Dao
interface BedtimeDao{
    @Insert
    fun insert(bedtime: Bedtime)
    @Query("SELECT * FROM Bedtime")
    suspend fun getbedtime():Bedtime
    @Update
    fun update(bedtime: Bedtime)
    @Query("DELETE FROM Bedtime")
    suspend fun clearTable()
}

@Database(entities = [Bedtime::class], version = 2)
abstract class Changebedtime: RoomDatabase(){
    abstract fun bedtimeDao(): BedtimeDao
}

