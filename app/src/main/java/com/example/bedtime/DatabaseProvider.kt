package com.example.bedtime

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: Changebedtime? = null

    @RequiresApi(Build.VERSION_CODES.O)
    fun getDatabase(context: Context): Changebedtime {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                Changebedtime::class.java,
                "Bedtime"
            )
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}
