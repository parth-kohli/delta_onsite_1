package com.example.bedtime

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.FontScaling
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.bedtime.ui.theme.BedTimeTheme
import com.example.bedtime.ui.theme.Typography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Alarm(this)
        permissionChecker(this)
        setContent {
            BedTimeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
 fun permissionChecker(context: Context) {
    val permissions = mutableListOf(
        android.Manifest.permission.ACCESS_WIFI_STATE,
        android.Manifest.permission.CHANGE_WIFI_STATE,
        android.Manifest.permission.POST_NOTIFICATIONS
    )
    val toRequest = permissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
    if (toRequest.isNotEmpty()) {
        ActivityCompat.requestPermissions(context as Activity, toRequest.toTypedArray(), 1001)
    }
}
fun Alarm(context: Context) {
    val intent = Intent(context, BedTimeBackdrop::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.SECOND, 13)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) {
            add(Calendar.MINUTE, 1)
        }
    }
    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        60_000L,
        pendingIntent
    )
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val db = DatabaseProvider.getDatabase(LocalContext.current)
    val refresh = remember { mutableStateOf(false) }
    val selectedDestination = remember { mutableStateOf("home") }
    val wifiblocking = remember { mutableStateOf(false) }
    val bedtimedatabase = remember { mutableStateOf<Bedtime>(Bedtime(0, 22, 10, false)) }
    val bedtime = remember {
        mutableStateOf(
            LocalDateTime.of(
                LocalDateTime.now().year,
                LocalDateTime.now().month,
                LocalDateTime.now().dayOfMonth,
                bedtimedatabase.value!!.hour,
                bedtimedatabase.value!!.minute,
                0
            )
        )
    }
    val context = LocalContext.current
    val isLoading = remember { mutableStateOf(false) }
    val update = remember { mutableStateOf(0) }
    LaunchedEffect(update.value) {
        if (update.value>0) {
            withContext(Dispatchers.IO) {
                try {

                    val database = db.bedtimeDao().getbedtime()
                    isLoading.value = true
                    if (database != null) {
                        db.bedtimeDao().update(database.copy(wifiBlocking = wifiblocking.value))
                    }

                } catch (e: Exception) {
                    println(e)
                }
            }
        }
    }
        LaunchedEffect(refresh.value) {
            withContext(Dispatchers.IO) {
                try {

                    val database = db.bedtimeDao().getbedtime()
                    isLoading.value = true
                    withContext(Dispatchers.Main) {
                        if (database != null) {

                            wifiblocking.value = database.wifiBlocking
                            bedtimedatabase.value = database
                            bedtime.value = LocalDateTime.of(
                                LocalDateTime.now().year,
                                LocalDateTime.now().month,
                                LocalDateTime.now().dayOfMonth,
                                database.hour,
                                database.minute,
                                0
                            )


                        } else {
                            val default = Bedtime(
                                hour = 22,
                                minute = 0,
                                id = 0,
                                wifiBlocking = wifiblocking.value
                            )
                            db.bedtimeDao().insert(default)
                            bedtimedatabase.value = default
                            selectedDestination.value = "changebedtime"

                        }
                    }
                } catch (e: Exception) {
                    val default =
                        Bedtime(hour = 22, minute = 0, id = 0, wifiBlocking = wifiblocking.value)
                    db.bedtimeDao().insert(default)
                    withContext(Dispatchers.Main) {
                        bedtimedatabase.value = default
                        selectedDestination.value = "changebedtime"
                        isLoading.value = true
                    }
                }
            }
        }

        if (bedtime.value < LocalDateTime.now()) {
            bedtime.value = LocalDateTime.of(
                LocalDateTime.now().year, LocalDateTime.now().month,
                LocalDateTime.now().dayOfMonth + 1, bedtime.value.hour, bedtime.value.minute, 0
            )
        }
        val bedtimeLong = remember {
            mutableStateOf(
                bedtime.value.toEpochSecond(
                    ZoneOffset.ofHoursMinutes(
                        5,
                        30
                    )
                ) - Instant.now().epochSecond
            )
        }
        val instant = remember { mutableStateOf(Instant.ofEpochSecond(bedtimeLong.value)) }
        LaunchedEffect(Unit) {
            while (true) {
                bedtimeLong.value = bedtime.value.toEpochSecond(
                    ZoneOffset.ofHoursMinutes(
                        5,
                        30
                    )
                ) - Instant.now().epochSecond
                instant.value = Instant.ofEpochSecond(bedtimeLong.value)
                delay(1000)
            }
        }

        val navController = rememberNavController()
        NavHost(navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    wifiblocking,
                    instant,
                    bedtime.value.toLocalTime(),
                    navController,
                    isLoading,
                    update
                )
            }
            composable("changebedtime") {
                ChangeBedTime(
                    modifier = Modifier,
                    navController,
                    bedtimedatabase,
                    db
                ) { refresh.value = true; navController.navigate("home") }
            }
        }

    }
    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun HomeScreen(
        wifiblocking: MutableState<Boolean>,
        ttbedtime: MutableState<Instant?>,
        bedtime: LocalTime,
        navController: NavController,
        isLoading: MutableState<Boolean>,
        refresh: MutableState<Int>
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painter = painterResource(id = R.drawable.bed), contentDescription = null)
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "Time left till bedtime",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(10.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                if (isLoading.value) {

                    Text(
                        text = "${
                            ttbedtime.value?.truncatedTo(ChronoUnit.SECONDS).toString()
                                .substring(11, 19).replace(":", " : ")
                        }",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 50.sp
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(50.dp))
                }
                Spacer(modifier = Modifier.height(70.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Disable wifi blocking ", fontSize = 25.sp)
                    Switch(
                        checked = wifiblocking.value,
                        onCheckedChange = {
                            wifiblocking.value = !wifiblocking.value; refresh.value +=1
                        })
                }
                Spacer(modifier = Modifier.height(20.dp))
                IconButton(
                    onClick = { navController.navigate("changebedtime") },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Row() {
                        Text("Change bedtime: ", fontSize = 25.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "${bedtime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            fontSize = 25.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun ChangeBedTime(
        modifier: Modifier,
        navController: NavController,
        bedtime: MutableState<Bedtime>,
        db: Changebedtime,
        saveChanges: () -> Unit
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val changedtime = remember { mutableStateOf(bedtime.value) }

                Image(painter = painterResource(id = R.drawable.bed), contentDescription = null)
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "CHANGE BEDTIME",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(10.dp)
                )
                Spacer(modifier = Modifier.height(70.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = {
                            if (changedtime.value.hour > 0) changedtime.value =
                                changedtime.value.copy(hour = changedtime.value.hour - 1) else changedtime.value =
                                changedtime.value.copy(hour = 23)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.minus),
                                contentDescription = "Increase",
                                tint = Color.White
                            )


                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            if (changedtime.value.hour < 10) "0${changedtime.value.hour}" else changedtime.value.hour.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 25.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        IconButton(onClick = {
                            if (changedtime.value.hour < 23) changedtime.value =
                                changedtime.value.copy(hour = changedtime.value.hour + 1) else changedtime.value =
                                changedtime.value.copy(hour = 0)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.plus),
                                contentDescription = "Increase",
                                tint = Color.White
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = {
                            if (changedtime.value.minute > 0) changedtime.value =
                                changedtime.value.copy(minute = changedtime.value.minute - 1) else changedtime.value =
                                changedtime.value.copy(minute = 59)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.minus),
                                contentDescription = "Increase",
                                tint = Color.White
                            )

                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            if (changedtime.value.minute < 10) "0${changedtime.value.minute}" else "${changedtime.value.minute}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 25.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        IconButton(onClick = {
                            if (changedtime.value.minute < 59) changedtime.value =
                                changedtime.value.copy(minute = changedtime.value.minute + 1) else changedtime.value =
                                changedtime.value.copy(minute = 0)
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.plus),
                                contentDescription = "Increase",
                                tint = Color.White
                            )
                        }
                    }

                }
                Spacer(modifier = Modifier.height(70.dp))
                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        db.bedtimeDao().update(changedtime.value)
                        bedtime.value = changedtime.value
                        withContext(Dispatchers.Main) {
                            saveChanges()
                        }

                    }
                }, modifier = Modifier.size(200.dp, 60.dp)) {
                    Text(
                        "Save Changes",
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

            }
        }
    }

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BedTimeTheme {
        Greeting("Android")
    }
}