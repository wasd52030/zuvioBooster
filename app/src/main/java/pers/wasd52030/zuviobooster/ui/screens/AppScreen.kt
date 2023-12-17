package pers.wasd52030.zuviobooster.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import pers.wasd52030.zuviobooster.R
import pers.wasd52030.zuviobooster.model.course
import pers.wasd52030.zuviobooster.ui.components.TwoArcLoading
import pers.wasd52030.zuviobooster.ui.theme.ZuvioBoosterTheme
import pers.wasd52030.zuviobooster.utils.courseUtils
import java.util.regex.Pattern

val Context.dataStore by preferencesDataStore(name = "userInfo")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(locationManager: LocationManager?) {

    val context = LocalContext.current

    val preferenceAccount = stringPreferencesKey("account")
    val preferencePassword = stringPreferencesKey("password")
    val preferenceIsRemember = booleanPreferencesKey("isRemember")


    val account = remember {
        val flow = context.dataStore.data.map {
            it[preferenceAccount] ?: ""
        }

        val preResult: String
        runBlocking(Dispatchers.IO) {
            preResult = flow.first()
        }

        mutableStateOf(preResult)
    }

    val password = remember {
        val flow = context.dataStore.data.map {
            it[preferencePassword] ?: ""
        }

        val preResult: String
        runBlocking(Dispatchers.IO) {
            preResult = flow.first()
        }

        mutableStateOf(preResult)
    }

    val passwordVisible = remember {
        mutableStateOf(false)
    }


    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val permissionsGranted = permissions.values.reduce { acc, isPermissionGranted ->
                acc && isPermissionGranted
            }

            if (!permissionsGranted) {
                return@rememberLauncherForActivityResult
            }
        }
    )

    // 給定預設經緯度，南海！
    val currentLocation = remember {
        val l = Location("")
        l.latitude = 19.19810
        l.longitude = 114.514

        mutableStateOf(l)
    }

    val isGPSNotOpen = remember {
        mutableStateOf(false)
    }

    val locationListener = LocationListener { location ->
        currentLocation.value = location

        Log.d("6", "latitude= ${location.latitude}")
        Log.d("6", "longitude= ${location.longitude}")
    }

    val results = remember {
        mutableStateOf(listOf<course>())
    }

    val isSignin = remember {
        mutableStateOf(false)
    }

    val isRememberPassword = remember {
        val flow = context.dataStore.data.map {
            it[preferenceIsRemember] ?: false
        }

        val preResult: Boolean
        runBlocking(Dispatchers.IO) {
            preResult = flow.first()
        }

        mutableStateOf(preResult)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(topBar = {
        TopAppBar(colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary
        ), title = {
            Text("Zuvio Booster")
        })
    },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(20.dp)
        ) {
            TextField(
                label = { Text(text = "信箱") },
                value = account.value,
                singleLine = true,
                onValueChange = { a -> account.value = a },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 0.dp, 0.dp, 10.dp),
            )
            TextField(
                label = { Text(text = "密碼") },
                value = password.value,
                singleLine = true,
                onValueChange = { b -> password.value = b },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 0.dp, 0.dp, 10.dp),
                trailingIcon = {
                    val image = if (passwordVisible.value)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    // Please provide localized description for accessibility services
                    val description =
                        if (passwordVisible.value) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Icon(imageVector = image, description)
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 0.dp, 0.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = isRememberPassword.value,
                    onCheckedChange = { isRememberPassword.value = it }
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp, 0.dp, 0.dp, 0.dp),
                    text = "記住帳號密碼"
                )
            }

            // 位置資訊
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(0.dp, 0.dp, 0.dp, 10.dp)
            ) {
                Text(
                    text = "位置資訊",
                    modifier = Modifier
                        .padding(15.dp),
                    textAlign = TextAlign.Center,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "經度 = ${"%.3f".format(currentLocation.value.longitude)}",
                            modifier = Modifier
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "緯度 = ${"%.3f".format(currentLocation.value.latitude)}",
                            modifier = Modifier
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(80.dp)
                            .padding(5.dp, 5.dp),
                        onClick = {
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) -> {
                                    if (LocationManagerCompat.isLocationEnabled(locationManager!!)) {
                                        locationManager.requestLocationUpdates(
                                            LocationManager.NETWORK_PROVIDER,
                                            0L,
                                            0f,
                                            locationListener
                                        )

                                        locationManager.requestLocationUpdates(
                                            LocationManager.GPS_PROVIDER,
                                            0L,
                                            0f,
                                            locationListener
                                        )
                                    } else {
                                        isGPSNotOpen.value = true
                                    }
                                }

                                else -> {
                                    locationPermissionLauncher.launch(locationPermissions)
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            modifier = Modifier.size(55.dp),
                            painter = painterResource(R.drawable.location),
                            contentDescription = "get Location"
                        )
                    }

                    if (isGPSNotOpen.value) {
                        AlertDialog(
                            onDismissRequest = {
                                isGPSNotOpen.value = false
                            },
                            title = { Text("") },
                            text = { Text("請打開GPS以取得確切位置") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                        isGPSNotOpen.value = false
                                    }
                                ) {
                                    Text("打開設置")
                                }
                            }
                        )
                    }
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 0.dp, 0.dp, 10.dp),
                onClick = {
                    try {
                        CoroutineScope(Dispatchers.IO).launch {
                            Log.d("6", "簽到啟動")

                            isSignin.value = true

                            val doc = Jsoup.connect("https://irs.zuvio.com.tw/irs/submitLogin")
                                .data("email", account.value)
                                .data("password", password.value)
                                .data("current_language", "zh-TW")
                                .userAgent("Mozilla")
                                .post()

                            val scripts = doc.getElementsByTag("script")
                            val pattern = Pattern.compile("var accessToken = \"(.*?)\";")
                            var userId = ""
                            var accessToken = ""

                            scripts.forEach { element ->
                                val script = element.data()
                                if (script.contains("var accessToken")) {
                                    val matcher = pattern.matcher(script)
                                    if (matcher.find()) {
                                        accessToken = matcher.group(1) as String
                                    }
                                }
                                if (script.contains("var user_id")) {
                                    userId = script.split("var user_id = ")[1].split(";")[0]
                                }
                            }

                            try {
                                results.value = courseUtils.getCourseList(
                                    userId,
                                    accessToken,
                                    currentLocation.value
                                )
                            } catch (e: Exception) {
                                results.value = listOf()
                                CoroutineScope(Dispatchers.IO).launch {
                                    snackbarHostState.showSnackbar(message = "無法登入，請檢查帳號密碼或網路")
                                }
                            } finally {
                                if (isRememberPassword.value) {
                                    context.dataStore.edit {
                                        it[preferenceAccount] = account.value
                                        it[preferencePassword] = password.value
                                        it[preferenceIsRemember] = isRememberPassword.value
                                    }
                                }
                            }

                            isSignin.value = false
                        }
                    } catch (e: Exception) {
                        Log.e("6", e.toString())
                        results.value = listOf()
                        CoroutineScope(Dispatchers.IO).launch {
                            snackbarHostState.showSnackbar(message = "無法登入，請檢查帳號密碼或網路")
                        }
                    }
                }
            ) {
                Text(text = "簽到，啟動！")
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(0.dp, 0.dp, 0.dp, 10.dp)
            ) {
                if (isSignin.value) {
                    TwoArcLoading(
                        modifier = Modifier
                            .width(200.dp)
                            .height(200.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn {
                        items(results.value) {
                            Card(
                                modifier = Modifier.padding(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                ),
                            ) {
                                Text(
                                    text = "課程ID: ${it.courseID}",
                                    modifier = Modifier.padding(5.dp)
                                )
                                Text(
                                    text = "課程名稱: ${it.courseName}",
                                    modifier = Modifier.padding(5.dp)
                                )
                                Text(
                                    text = "講師: ${it.teacherName}",
                                    modifier = Modifier.padding(5.dp)
                                )
                                Text(
                                    text = "簽到狀態: ${it.checkStatus}",
                                    modifier = Modifier.padding(5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    ZuvioBoosterTheme {
        App(null)
    }
}