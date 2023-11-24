package com.example.lteandgps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.btnRst
import kotlinx.android.synthetic.main.activity_main.btnSaveData
import kotlinx.android.synthetic.main.activity_main.fileName
import kotlinx.android.synthetic.main.activity_main.minusX
import kotlinx.android.synthetic.main.activity_main.minusY
import kotlinx.android.synthetic.main.activity_main.plusX
import kotlinx.android.synthetic.main.activity_main.plusY
import kotlinx.android.synthetic.main.activity_main.positionXView
import kotlinx.android.synthetic.main.activity_main.positionYView
import kotlinx.android.synthetic.main.activity_main.webView
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {
    private var dirInternal = ""
    private var wifiChanged = true
    private val HTML_FILE = "http://221.153.176.109/dot/"
    private var preScanResult: Any? = null
    private val vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    var isStartRecording = false

    var wifidata = ""
    private var origWifiData = ""

    lateinit var toast : Toast
    private var positionX : Double = 0.0
    private var positionY : Double = 0.0

    var wifipermitted = false

    private var wifiString = ""
    lateinit var wifiManager: WifiManager

    val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            var success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            Log.d("extra wifi check", success.toString())
            wifipermitted = success
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webViewSetting(HTML_FILE)

        wifiManager = this.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager



        checkFunction()
        init()
        threadStart()


        btnSaveData.setOnClickListener {
            isStartRecording = !isStartRecording
            when (isStartRecording) {
                true -> {
                    wifidata = ""

                    btnSaveData.text = "Recording"
                    positionX = positionXView.text.toString().toDouble()
                    positionY = positionYView.text.toString().toDouble()
                    positionX = (positionX/6).roundToInt().toDouble()*6
                    positionY = (positionY/6).roundToInt().toDouble()*6

                    positionXView.setText(positionX.toString())
                    positionYView.setText(positionY.toString())

                    toast = Toast.makeText(this, "cell 변화 감지", Toast.LENGTH_SHORT)

                    wifipermitted = false
                    wifiManager.startScan()
                    getWifiInfo()
                }
                else -> {
                    btnSaveData.text = "Save"
                    writewifiFile(if (fileName.text.toString().isEmpty()) "default.txt" else fileName.text.toString() + ".txt", wifidata)
                    wifidata = ""
                    positionX = 0.0
                    positionY = 0.0
                }
            }
        }
        btnRst.setOnClickListener{
            webView.loadUrl("javascript:make_matrix(0, 0, 2, 0, 0.2, 'blue')")
        }
        plusX.setOnClickListener {
            wifiManager.startScan()
            positionX = positionXView.text.toString().toDouble()
            positionY = positionYView.text.toString().toDouble()
            positionX += 6.0
            positionX
            posChanged()
            if (wifiChanged){
                webView.loadUrl("javascript:make_matrix($positionY, ${positionX}, 2, 1, 0.2, 'blue')")
            }
            webView.loadUrl("javascript:show_my_position($positionY, ${positionX}, 2, ${if(wifiChanged) 3 else 1})")
            writeTextFileInternal(if (fileName.text.toString().isEmpty()) "default.txt" else fileName.text.toString() + ".txt", wifiString)
            wifiString = ""
            vibrator.vibrate(80)

            positionXView.setText(positionX.toString())
            positionYView.setText(positionY.toString())
        }
        minusX.setOnClickListener {
            wifiManager.startScan()
            positionX = positionXView.text.toString().toDouble()
            positionY = positionYView.text.toString().toDouble()
            positionX -= 6.0
            posChanged()

            if (wifiChanged){
                webView.loadUrl("javascript:make_matrix($positionY, ${positionX}, 2, 1, 0.2, 'blue')")
            }
            webView.loadUrl("javascript:show_my_position($positionY, ${positionX}, 2, ${if(wifiChanged) 3 else 1})")
            writeTextFileInternal(if (fileName.text.toString().isEmpty()) "default.txt" else fileName.text.toString() + ".txt", wifiString)
            wifiString = ""
            vibrator.vibrate(80)

            positionXView.setText(positionX.toString())
            positionYView.setText(positionY.toString())
        }
        plusY.setOnClickListener{
            wifiManager.startScan()
            positionX = positionXView.text.toString().toDouble()
            positionY = positionYView.text.toString().toDouble()
            positionY += 6.0
            posChanged()

            if (wifiChanged){
                webView.loadUrl("javascript:make_matrix($positionY, ${positionX}, 2, 1, 0.2, 'blue')")
            }
            webView.loadUrl("javascript:show_my_position($positionY, ${positionX}, 2, ${if(wifiChanged) 3 else 1})")
            writeTextFileInternal(if (fileName.text.toString().isEmpty()) "default.txt" else fileName.text.toString() + ".txt", wifiString)
            wifiString = ""
            vibrator.vibrate(80)

            positionXView.setText(positionX.toString())
            positionYView.setText(positionY.toString())
        }
        minusY.setOnClickListener {
            wifiManager.startScan()
            positionX = positionXView.text.toString().toDouble()
            positionY = positionYView.text.toString().toDouble()
            positionY -= 6.0
            posChanged()

            if (wifiChanged){
                webView.loadUrl("javascript:make_matrix($positionY, ${positionX}, 2, 1, 0.2, 'blue')")
            }
            webView.loadUrl("javascript:show_my_position($positionY, ${positionX}, 2, ${if(wifiChanged) 3 else 1})")
            writeTextFileInternal(if (fileName.text.toString().isEmpty()) "default.txt" else fileName.text.toString() + ".txt", wifiString)
            wifiString = ""
            vibrator.vibrate(80)

            positionXView.setText(positionX.toString())
            positionYView.setText(positionY.toString())
        }
    }

    private fun webViewSetting(html_file_name: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        webView.addJavascriptInterface(WebviewInterface(this), "NaviEvent")

        webView.loadUrl(html_file_name)
        webView.scrollTo(1690, 480)
        webView.isScrollbarFadingEnabled = true
        webView.setInitialScale(160)

        val webSettings = webView.settings
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = true
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = false
        webSettings.setSupportMultipleWindows(false)
        webSettings.setSupportZoom(true)
        webSettings.domStorageEnabled = true
    }
    class WebviewInterface(private val mContext: Context) {
        @JavascriptInterface
        fun showAndroidToast(toast: String) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
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


    private fun init() {
        positionXView.setText("0")
        positionYView.setText("0")
        dirInternal = filesDir.absolutePath //내부저장소 절대 경로

        var path = getwifiExternalPath(this)
        var file = File(path)
        if (!file.exists()) file.mkdir()

        file = File(path)
        if (!file.exists()) file.mkdir()

        file = File(path)
        if (!file.exists()) file.mkdir()
    }
    private fun getwifiExternalPath(context: Context): String {
        val directory = File(context.getExternalFilesDir(null), "wifidata")
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                // 디렉토리 생성 성공
                Toast.makeText(context, "디렉토리 생성 성공", Toast.LENGTH_SHORT).show()
            } else {
                // 디렉토리 생성 실패
                Toast.makeText(context, "디렉토리 생성 실패", Toast.LENGTH_SHORT).show()
            }
        }
        return directory.absolutePath
    }



    // 파일 쓰기
    fun writeTextFileInternal(filename:String, content:String){
        val directory = dirInternal
        val dir = File(directory)

        if(!dir.exists()){ //dir이 존재 하지 않을때
            dir.mkdirs() //mkdirs : 중간에 directory가 없어도 생성됨
        }

        val writer = FileWriter(directory + "/" + filename, true)
        //true는 파일을 이어쓰는 것을 의미

        //쓰기 속도 향상
        val buffer = BufferedWriter(writer)
        buffer.write(content)
        buffer.close()
    }

    //파일 읽기
    fun readTextFile(fullpath:String) : String{
        val file = File(fullpath)
        if(!file.exists()) return "" //파일이 존재하지 않을때

        val reader = FileReader(file)
        val buffer = BufferedReader(reader)

        var temp:String? = ""
        var result = StringBuffer()

        while(true){
            temp = buffer.readLine() // 줄단위로 read
            if(temp == null) break
            else result.append(temp).append("\n")
        }
        buffer.close()
        return result.toString()
    }




    private fun writewifiFile(title: String, body: String) {
        try {
            val path = getwifiExternalPath(this)
            Log.d("dsijdisjdisjdi", path)
            val bw = BufferedWriter(FileWriter(path + title, true)) // 11.10 누적 저장
            bw.write(body)
            bw.close()
            Toast.makeText(this, path + "에 저장완료", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }
    private fun getWifiInfo(){
        var scanResultList = wifiManager.scanResults
        var updatedata = ""
        for (i in 1 until scanResultList!!.size) {
            updatedata += positionX.toString() + "\t" + positionY.toString() + "\t" +
                    scanResultList[i].SSID.toString() + "\t" + scanResultList[i].BSSID.toString() +
                    "\t" + scanResultList[i].level.toString() + "\r\n"
        }
        if (updatedata != origWifiData) {
            wifidata += updatedata
        }
    }
    private fun posChanged(){
        var scanResultList = wifiManager.scanResults
        if (!isScanResultEqual(preScanResult as List<ScanResult>?, scanResultList)) {
            var updatedata = ""
            for (i in 1 until scanResultList!!.size) {
                updatedata += positionX.toString() + "\t" + positionY.toString() + "\t" +
                        scanResultList[i].SSID.toString() + "\t" + scanResultList[i].BSSID.toString() +
                        "\t" + scanResultList[i].level.toString() + "\r\n"
            }
            wifiString = updatedata
            vibrator.vibrate(160)
            preScanResult = scanResultList
            wifiChanged = true
        }
        else{
            wifiChanged = false
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
                    getWifiInfo()
                }
            }
        }
    }
    fun isScanResultEqual(previousResults: List<ScanResult>?, currentResults: List<ScanResult>): Boolean {
        if (previousResults == null) {
            return false
        }

        // BSSID를 기준으로 스캔 결과 비교
        val previousBSSIDs = previousResults.map { it.BSSID }
        val currentBSSIDs = currentResults.map { it.BSSID }

        return previousBSSIDs == currentBSSIDs
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

