package com.example.lteandgps

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Vibrator
import android.telephony.CellInfo
import android.telephony.CellInfoWcdma
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.telephony.gsm.GsmCellLocation
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*
import kotlin.math.round
import kotlin.reflect.typeOf


class MainActivity : AppCompatActivity() {
    private val vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    var longitude: Double = 0.0 // 경도
    var latitude: Double = 0.0 // 위도

    var cellID: Int = 0
    var lac: Int = 0

    var cnt = 0

    var cellIDlist = ArrayList<Int>()

    var mccmnc: Int = 0
    var isStartRecording = false
    var check = false

    var wifidata = ""
    var gpsdata = ""
    var ltedata = ""
    var ltehistdata = ""

    lateinit var toast : Toast
    private var positionX : Double = 0.0
    private var positionY : Double = 0.0

    var wifipermitted = false
    var scanstarted = false

    var firstscan = false

    lateinit var wifiManager: WifiManager

    val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            var success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            Log.d("extra wifi check", success.toString())
            wifipermitted = success
        }
    }
    //////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val btnSaveData = findViewById<Button>(R.id.btnSaveData)
        val fileName = findViewById<EditText>(R.id.fileName)
        val btnupdategps = findViewById<Button>(R.id.btnupdategps)


        checkFunction()
        init()
        threadStart()
        btnSaveData.setOnClickListener {
            isStartRecording = !isStartRecording
            Log.d("isstartrecording", isStartRecording.toString())
            when (isStartRecording) {
                true -> {
                    wifidata = ""
                    var positionXView = findViewById<EditText>(R.id.positionXView)
                    var positionYView = findViewById<EditText>(R.id.positionYView)

                    btnSaveData.text = "Recording"
                    positionX = positionXView.text.toString().toDouble()
                    positionY = positionYView.text.toString().toDouble()
                    toast = Toast.makeText(this, "cell 변화 감지", Toast.LENGTH_SHORT)

                    wifipermitted = false
                    scanstarted = true
                    firstscan = true
                    wifiManager.startScan()
                    getWifiInfo()
//                    getgpsinfo()
//                    getcellinfo()
//                    getcellinfo2()
                }
                else -> {
                    btnSaveData.text = "Save"
                    writewifiFile(if (fileName.text.toString().isEmpty()) "default.txt" else fileName.text.toString() + ".txt", wifidata)

                    // lte, gps 데이터 안 씀
                    // 2023 01 10 최민혁 명동 신세계
//                    writelteFile(if (fileName.text.toString().isEmpty()) "default.txt" else fileName.text.toString() + ".txt", ltedata)
//                    writegpsFile(if (fileName.text.toString().isEmpty()) "default.txt" else fileName.text.toString() + ".txt", gpsdata)
//                    writeltehistFile(if (fileName.text.toString().isEmpty()) "defaultcellhist.txt" else fileName.text.toString() + "cellhist.txt", ltehistdata)
                    longitude = 0.0
                    latitude = 0.0
                    cellID = 0
                    lac = 0
                    check = false
                    wifidata = ""
                    gpsdata = ""
                    ltedata = ""
                    ltehistdata = ""
                    cellIDlist = ArrayList<Int>()
                    cnt = 0
                    positionX = 0.0
                    positionY = 0.0
                }
            }
        }
        btnupdategps.setOnClickListener {
//            getgpsinfo()
//            getcellinfo()
//            getcellinfo2()
        }
    }

    private fun checkFunction() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                && ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                && ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 101
                )
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 101
                )
            }
        } else {
        }
    }


    fun onChangePositionClicked(v: View) {
        var positionXView = findViewById<EditText>(R.id.positionXView)
        var positionYView = findViewById<EditText>(R.id.positionYView)

        positionX = positionXView.text.toString().toDouble()
        positionY = positionYView.text.toString().toDouble()
        Log.d("buttontest", (v.id == R.id.plusX).toString())
        when (v.id) {
            R.id.plusX -> positionX += 6.0
            R.id.minusX -> positionX -= 6.0
            R.id.plusY -> positionY += 6.0
            R.id.minusY -> positionY -= 6.0
        }
        Log.d("buttontest", (positionX).toString())

        scanstarted = true
        wifiManager.startScan()
        vibrator.vibrate(80)

//        getgpsinfo()
//        getcellinfo2()
//        getcellinfo()

        positionXView.setText(positionX.toString())
        positionYView.setText(positionY.toString())

        cnt = 0

//        wifiManager.startScan()
//        getWifiInfo()
//        getgpsinfo()

    }




    private fun init() {
        var positionXView = findViewById<EditText>(R.id.positionXView)
        var positionYView = findViewById<EditText>(R.id.positionYView)
        positionXView.setText("0")
        positionYView.setText("0")
        var path = getwifiExternalPath()
        var file = File(path)
        if (!file.exists()) file.mkdir()

        path = getgpsExternalPath()
        file = File(path)
        if (!file.exists()) file.mkdir()

        path = getlteExternalPath()
        file = File(path)
        if (!file.exists()) file.mkdir()
    }



    fun getgpsinfo() {
        var locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if(locationManager != null) {
            var providers = locationManager.allProviders
            val isGPSEnabled: Boolean =
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled: Boolean =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                checkFunction()
            } else {
                when {
                    isNetworkEnabled -> {
                        val location =
                            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        longitude = location?.longitude!!
                        latitude = location?.latitude!!
//                    Toast.makeText(this, "네트워크 기반 현재 위치", Toast.LENGTH_SHORT).show()
                    }
//                isGPSEnabled -> {
//                    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
//                    longitude = location?.longitude!!
//                    latitude = location?.latitude!!
//                    Toast.makeText(this, "gps 기반 현재 위치", Toast.LENGTH_SHORT).show()
//                }
                    else -> {
                        Toast.makeText(this, "현재 위치 확인 불가", Toast.LENGTH_SHORT).show()
                    }
                }
                Log.d("providercheck", longitude.toString() + "\t" + latitude.toString())
//                locationManager.requestLocationUpdates(
//                    LocationManager.GPS_PROVIDER,
//                    1,
//                    0F,
//                    gpsLocationListener
//                )
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1,
                    1F,
                    gpsLocationListener
                )
                locationManager.removeUpdates(gpsLocationListener)
            }
            locationManager.removeUpdates(gpsLocationListener)
        }
    }

    fun getcellinfo(): Boolean {
        var returnbool = false
        var tmpcellID = cellID

        var tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkFunction()
        } else {
            var location = tm.cellLocation as GsmCellLocation
            cellID = location.cid
//            if ((cellID != tmpcellID) && (!cellIDlist.contains(cellID))) {
//                cellIDlist.add(cellID)
//                returnbool = true
//            }
            lac = location.lac
            mccmnc = tm.networkOperator.toInt()
        }
        return returnbool
    }
    fun getcellinfo2(): String {
        var returnstr = ""
        var tmpcellID = cellID

        var tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkFunction()
        } else {
            var celllist = tm.allCellInfo
            for (n : CellInfo in celllist){
                returnstr += n.toString() + "\n\r"
                Log.d("celllist", n.toString())
            }

        }
        return returnstr
    }

    val gpsLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val provider: String = location.provider
            val longitude: Double = location.longitude
            val latitude: Double = location.latitude
            val altitude: Double = location.altitude
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }


    private fun getwifiExternalPath(): String {
        var sdPath = ""
        val ext = Environment.getExternalStorageState()
        sdPath = if (ext == Environment.MEDIA_MOUNTED) {
            val dir = File(Environment.getExternalStorageDirectory(), "wifidata")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir.absolutePath + "/"
        } else {
            val dir = File(filesDir, "wifidata")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir.absolutePath + "/"
        }
        return sdPath
    }


    private fun getgpsExternalPath(): String {
        var sdPath = ""
        val ext = Environment.getExternalStorageState()
        sdPath = if (ext == Environment.MEDIA_MOUNTED) {
            Environment.getExternalStorageDirectory()
                .absolutePath + "/gpsdata/"
        } else {
            "$filesDir/gpsdata/"
        }
        return sdPath
    }

    private fun getlteExternalPath(): String {
        var sdPath = ""
        val ext = Environment.getExternalStorageState()
        sdPath = if (ext == Environment.MEDIA_MOUNTED) {
            Environment.getExternalStorageDirectory()
                .absolutePath + "/ltedata/"
        } else {
            "$filesDir/ltedata/"
        }
        return sdPath
    }

    private fun writewifiFile(title: String, body: String) {
        try {
            val path = getwifiExternalPath()
            val bw = BufferedWriter(FileWriter(path + title, false))
            bw.write(body)
            bw.close()
            Toast.makeText(this, path + "에 저장완료", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun writegpsFile(title: String, body: String) {
        try {
            val path = getgpsExternalPath()
            val bw = BufferedWriter(FileWriter(path + title, false))
            bw.write(body)
            bw.close()
            Toast.makeText(this, path + "에 저장완료", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun writelteFile(title: String, body: String) {
        try {
            val path = getlteExternalPath()
            val bw = BufferedWriter(FileWriter(path + title, false))
            bw.write(body)
            bw.close()
            Toast.makeText(this, path + "에 저장완료", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeltehistFile(title: String, body: String) {
        try {
            val path = getlteExternalPath()
            val bw = BufferedWriter(FileWriter(path + title, false))
            bw.write(body)
            bw.close()
            Toast.makeText(this, path + "에 저장완료", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWifiInfo(){
        //if (wifipermitted) { // wifiScan을 한 경우에만 getScanResult를 사용하도록 flag 변수 구현
        if (firstscan) {
            if (wifipermitted) {
                var scanResultList = wifiManager.scanResults
                var updatedata = ""
                for (i in 1 until scanResultList!!.size) {
                    updatedata += cnt.toString() + "\t" + positionX.toString() + "\t" + positionY.toString() + "\t" +
                            scanResultList[i].SSID.toString() + "\t" + scanResultList[i].BSSID.toString() +
                            "\t" + scanResultList[i].level.toString() + "\r\n"
                }
                wifidata += updatedata
                Log.d("wifilist", updatedata)
                cnt += 1

                Log.d("getwifiinfocheck", wifipermitted.toString())
                wifipermitted = false
                scanstarted = false
                firstscan = false
                vibrator.vibrate(160)
                //}
            }
        } else{
            var scanResultList = wifiManager.scanResults
            var updatedata = ""
            for (i in 1 until scanResultList!!.size) {
                updatedata += cnt.toString() + "\t" + positionX.toString() + "\t" + positionY.toString() + "\t" +
                        scanResultList[i].SSID.toString() + "\t" + scanResultList[i].BSSID.toString() +
                        "\t" + scanResultList[i].level.toString() + "\r\n"
            }
            wifidata += updatedata
            Log.d("wifilist", updatedata)
            cnt += 1

            Log.d("getwifiinfocheck", wifipermitted.toString())
            wifipermitted = false
            scanstarted = false
        }
    }

    inner class dataThread : Thread() {
        override fun run() {
            while (true) {
                try {
                    sleep(500)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if(isStartRecording){
                    if (firstscan){
                        if(wifipermitted){
                            getWifiInfo()
//                            getgpsinfo()
//                            getcellinfo()
//                            getcellinfo2()
                        }
                    }else{

                        getWifiInfo()
//                        getgpsinfo()
//                        getcellinfo()
//                        getcellinfo2()
                    }
////                    if(!scanstarted) {
////                        wifiManager.startScan()
////                        scanstarted = true
//////                        check = getWifiInfo()
////                    }else{
//                    if(wifipermitted && scanstarted) {
//                        getWifiInfo()
//                    }
//                    if(!scanstarted) {
//                        wifiManager.startScan()
//                        scanstarted = true
//                        getWifiInfo()
//                    }
                    if (!firstscan) {
                        gpsdata += positionX.toString() + "\t" + positionY.toString() + "\t" + latitude.toString() + "\t" + longitude.toString() + "\r\n"
                        ltedata += positionX.toString() + "\t" + positionY.toString() + "\t" + mccmnc.toString() + "\t" +
                        cellID.toString() + "\t" + lac.toString() + "\r\n"
                        ltehistdata += positionX.toString() + "\t" + positionY.toString() + "\r\n" + getcellinfo2() + "\r\n" +"\r\n"
                        check = false
                    }
                }
            }
        }
    }

    fun threadStart() {
        val thread1 = dataThread()
        thread1.isDaemon = true
        thread1.start()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiScanReceiver)
    }
}

