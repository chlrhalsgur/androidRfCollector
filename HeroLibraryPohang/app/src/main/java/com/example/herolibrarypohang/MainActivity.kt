package com.example.herolibrarypohang

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.cslabposco.Elevator
import com.example.cslabposco.Escalator
import com.example.cslabposco.IndoorLocalization
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import kotlin.concurrent.thread
/**0909**/
private val WEB_ADDRESS =  "file:///android_asset/sciencelib5ffinal.html"//"file:///android_asset/posco_pohang2.html"//"file:///android_asset/SeoulStation.html"
private val WEB_ADDRESS2 = "file:///android_asset/posco_pohang2.html"



class MainActivity : AppCompatActivity(), SensorEventListener {


    private val mSensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private lateinit var indoorLocalization : IndoorLocalization
    private var myUUID : String = ""
    private val mHandler : Handler = Handler(Looper.myLooper()!!)
    private var countI = 100
    private lateinit var wvLayout0401v3 : WebView

    private var lastStep = 0
    var x = 2622f - 2478f
    var y = 930f - 672f

    private val XRANGE = 200
    val DATA_RANGE = 180
    val AXIS_VALUE = 3F
    var zVal = ArrayList<Entry>()
    var setZcomp = LineDataSet(zVal, "acc Z")
    var zVals = ArrayList<String>()
    var zDataSets = ArrayList<ILineDataSet>()

    private var is_popup_on : Boolean = false
    private var isFirstInit : Boolean = true
    private lateinit var alertDialog : AlertDialog

    //////수민 데이터 저장하기


    private lateinit var elevator: Elevator /**수민 210814**/
    private lateinit var escalator: Escalator /**수민 210814**/
    var elevatorInitEnd : Boolean = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkFunction()


        Glide.with(this).load(R.raw.whiteloading).into(loadingView)

        myUUID = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID).toString()
        wvLayout0401v3 = webView
        wvLayout0401v3.setInitialScale(190)
        wvLayout0401v3.scrollTo(1900, 110)
        indoorLocalization = IndoorLocalization(resources.openRawResource(R.raw.sciencelibrary_f5), resources.openRawResource(R.raw.sciencelibrary_f5_for_instant_3),resources.openRawResource(R.raw.build_map))

        val webSettings = wvLayout0401v3.settings
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = true
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = false
        webSettings.setSupportMultipleWindows(false)
        webSettings.setSupportZoom(true)

        wvLayout0401v3.goBack()
        wvLayout0401v3.loadUrl(WEB_ADDRESS)
        wvLayout0401v3.goBack()
        wvLayout0401v3.loadUrl(WEB_ADDRESS)

        initChart()
        threadStart()

        btnReset.setOnClickListener {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        coverSwitch.setOnClickListener {
            coverLayout.visibility = if (coverSwitch.isChecked) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun checkFunction() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), 101)
            }
        }
    }

    private fun initChart() {
        chartZ.axisLeft.setAxisMaxValue(AXIS_VALUE)
        chartZ.axisLeft.setAxisMinValue(-AXIS_VALUE)
        chartZ.axisRight.setAxisMaxValue(AXIS_VALUE)
        chartZ.axisRight.setAxisMinValue(-AXIS_VALUE)
        setZcomp.color = Color.BLUE
        setZcomp.lineWidth = 2F
        setZcomp.setDrawValues(false)
        setZcomp.setDrawCircles(false)
        setZcomp.setDrawCubic(true)
        setZcomp.axisDependency = YAxis.AxisDependency.RIGHT
        zDataSets.add(setZcomp)

        for (i in 0 until XRANGE) {
            zVals.add("")
        }
        chartZ.data = LineData(zVals, zDataSets)
        chartZ.invalidate()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {

            var result = indoorLocalization.sensorChanged(event)

            when(result[1]) {
                "The sensors is not ready yet" -> return
                "완전 수렴" -> {
                    if (lastStep != result[2].toInt()) {
                        if(result[5] == "true") {
                            wvLayout0401v3.goBack()
                            wvLayout0401v3.loadUrl(WEB_ADDRESS2)
                            wvLayout0401v3.goBack()
                            wvLayout0401v3.loadUrl(WEB_ADDRESS2)
                            indoorLocalization.floorchange = "false"
                        }
                        ResetAllPoint()
                        SyncPositionSend(result[3].toDouble(), result[4].toDouble())
                        lastStep = result[2].toInt()
                    }
                }
                else -> {
                    if (isFirstInit) {
    //                    Glide.with(this).pauseAllRequests()
                        mFrameLayout.removeView(loadingLayout)
                        Toast.makeText(this, "Let's go~", Toast.LENGTH_SHORT).show()
                        isFirstInit = false
                        is_popup_on=true
                        showSettingPopup(8)
                    }
                }
            }

            gyroView.text = result[0]
            insView.text = result[1]
            scView.text = result[2]
            xView.text = result[3]
            yView.text = result[4]

//            if (result[1] == "완전 수렴" /*result[1].substring(0 until 5) == "Given"*/) {
//                if (lastStep != result[2].toInt()) {
//                    SyncPositionSend(result[3].toDouble(), result[4].toDouble())
//                    lastStep = result[2].toInt()
//                }
//            } else if (result[1] != "The sensors is not ready yet") {
//                if (isFirstInit) {
////                    Glide.with(this).pauseAllRequests()
//                    mFrameLayout.removeView(loadingLayout)
//                    Toast.makeText(this, "Let's go~", Toast.LENGTH_SHORT).show()
//                    isFirstInit = false
//                    is_popup_on=true
//                    showSettingPopup(8)
//                }
//            }

            when (event.sensor.type) {
                Sensor.TYPE_MAGNETIC_FIELD -> {
//                    accuracyView.text = "magnetic sensor accuracy : ${event.accuracy}"  // 자기장 센서 accuracy
                    if (event.accuracy != 3) {   // 자기장 센서 accuracy 가 3이 아닐 땐,
//                        notiView.text = "I Need Calibration!"
                        if (is_popup_on==false) {  // popup 창이 여러개 뜨는 것을 방지
                            is_popup_on=true
                            showSettingPopup(event.accuracy)   // popup 창을 띄움
                        }
                    }
                }
            }

        }
        ////////////////////////////////test 용///////////////////////////////////////////
//        dataPosition.add(indoorLocalization.nowPosition[0].toString() + "\t" + indoorLocalization.nowPosition[1].toString())
//        dataMagx.add(indoorLocalization.caliX.toString())
//        dataMagy.add(indoorLocalization.caliY.toString())
//        dataMagz.add(indoorLocalization.caliZ.toString())
//        datalinearaccx.add(indoorLocalization.linearAccX.toString())
//        datalinearaccy.add(indoorLocalization.linearAccY.toString())
//        datalinearaccz.add(indoorLocalization.transformedAccZ.toString())
//        datagyro.add(indoorLocalization.dir.toString())
//        teststepnum.add(indoorLocalization.totalStepCount.toString())
//
//        extractSaveData()

//        if (elevator.writeStart) {
//            elevatorInitEnd = true
//        }
//        Log.d("elevatorInitEnd",elevatorInitEnd.toString())
//
//        /**210814 수민**/
//        //저장하고자 하는 데이터는 꼭 public 처리 할것.
//        if(elevatorInitEnd) {
//            writeFileForSumin() // 수민 데이터 파일로 내려받기
//            recordData[0].add(elevator.MoveDirection.toDouble())
//            recordData[1].add(elevator.finalResult.toDouble())
//            recordData[2].add(indoorLocalization.startfloor.toDouble())
//            ////////////////////////////////test 용///////////////////////////////////////////
//        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor != null) {
            if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                if (is_popup_on == true)
                    showSettingPopup(accuracy)   // sensor calibration 동작으로 sensor accuracy 가 변했다면, popup을 새로 띄움.
            }
        }
    }

    private fun showSettingPopup(accuracy: Int) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.alert_popup, null)
        val textView: TextView = view.findViewById(R.id.textView)
        val imageView: ImageView = view.findViewById(R.id.gif_image)
        Glide.with(this).load(R.drawable.compass).into(imageView);
        var accuracyLevel : String = ""
        var txtColor : Int = Color.RED
        accuracyLevel = when(accuracy) {
            0 -> "Sensor Accuracy : Very LOW"
            1 -> "Sensor Accuracy : LOW"
            2 -> "Sensor Accuracy : MEDIUM"
            3 -> "Sensor Accuracy : HIGH"
            else ->  "기기를 8자로 돌려주세요"
        }
        txtColor = when(accuracy) {
            0 -> Color.RED
            1 -> Color.RED
            2 -> Color.GREEN
            3 -> Color.BLUE
            else -> Color.BLACK
        }

        textView.text = accuracyLevel   // popup의 text에 현재 accuracy level을 띄움.
        textView.setTextColor(txtColor)

        try {
            alertDialog.dismiss()
        }
        catch (e: java.lang.Exception){
        }
        alertDialog = AlertDialog.Builder(this)
//            .setTitle("기기를 8자로 돌려주세요\n")
            .setPositiveButton("완료", {dialog, which ->  is_popup_on=false})
            .create()

        alertDialog.setView(view)
        alertDialog.show()

    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_GAME)
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    fun SyncPositionSend(x : Double, y : Double/*, dir:Int, color: Int*/) {
        mHandler.postDelayed(Runnable {
            //wvLayout0401v3.loadUrl("javascript:androidBridge1($x, $y)")   ////// 200328_원준_수정
            wvLayout0401v3.loadUrl("javascript:androidBridge($x, $y, 90, 'red')")   ////// 200328_원준_수정
        }, 100)
    }

    fun ResetAllPoint() {
        mHandler.postDelayed(Runnable {
            //wvLayout0401v3.loadUrl("javascript:androidBridge1($x, $y)")   ////// 200328_원준_수정
            wvLayout0401v3.loadUrl("javascript:removeAllPoints()")   ////// 200328_원준_수정
        }, 100)
    }

    fun showColorList(index_num : Int) {
        wvLayout0401v3.loadUrl("javascript:showColorList($index_num)")   ////// 200328_원준_수정
    }

    private fun chartZUpdate(x : String) {
        if (zVal.size > DATA_RANGE) {
            zVal.removeAt(0)
            for (i in 0 until DATA_RANGE) {
                zVal[i].xIndex = i
            }
        }
        zVal.add(Entry(x.toFloat(), zVal.size))
        setZcomp.notifyDataSetChanged()
        chartZ.notifyDataSetChanged()
        chartZ.invalidate()
    }

    var handlerZ: Handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            val now_acc = indoorLocalization.transformedAccZ
            if (msg.what == 0) {
                chartZUpdate(now_acc.toString())
            }
        }
    }


/////////////////////////////////test용////////////////////////////////////////////

//    private var autoFileNum : Int = 0
//    private var nowSampling : Boolean = false
//    private var saveData : String = ""
//    private var dataPosition : Queue<String> = LinkedList()
//    private var dataMagx : Queue<String> = LinkedList()
//    private var dataMagy : Queue<String> = LinkedList()
//    private var dataMagz : Queue<String> = LinkedList()
//    private var datalinearaccx : Queue<String> = LinkedList()
//    private var datalinearaccy : Queue<String> = LinkedList()
//    private var datalinearaccz : Queue<String> = LinkedList()
//    private var datagyro : Queue<String> = LinkedList()
//    private var teststepnum : Queue<String> = LinkedList()

//    private fun extractSaveData() {
//
//        var posi = dataPosition.poll().split("\t")
//
//        saveData += indoorLocalization.totalStepCount.toString() + "\t" +posi[0] + "\t" + posi[1] + "\t" + datalinearaccx.poll() + "\t" +datalinearaccy.poll() + "\t" + datalinearaccz.poll() + "\t" +  dataMagx.poll() + "\t" + dataMagy.poll() + "\t" + dataMagz.poll() + "\r\n"
//
//        datalinearaccx.clear()
//        datalinearaccy.clear()
//        datalinearaccz.clear()
//        dataPosition.clear()
//        dataMagx.clear()
//        dataMagy.clear()
//        dataMagz.clear()
//    }

//    private fun getExternalPath() : String {
//        var sdPath = ""
//        val ext = Environment.getExternalStorageState()
//        sdPath = if (ext == Environment.MEDIA_MOUNTED) { //외부저장소 사용 가능한지 확인하기. MEDIA_MOUNTED : 파일 읽고 쓰기 모두 가능
//            Environment.getExternalStorageDirectory()
//                .absolutePath + "/HeroLibraryPohang/"
//        } else {
//            "$filesDir/HeroLibraryPohang/"
//        }
//        return sdPath
//    }
//
//    private fun writeFile(title: String, body: String) {
//        try {
//            val path = getExternalPath()
//            val bw = BufferedWriter(FileWriter(path + title, false))
//            bw.write(body)
//            bw.close()
//            Toast.makeText(this, path + "에 저장완료", Toast.LENGTH_SHORT).show()
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
//        }
////        dataPosition.clear()
////        dataMagx.clear()
////        dataMagy.clear()
////        dataMagz.clear()
//    }

//    private fun writeFileForSumin() {
//        when(elevator.runElevator) {
//            true -> {
//                recordData = arrayListOf<ArrayList<Double>>()
//                for (i in 0 until NUMOFDATA) {
//                    recordData.add(arrayListOf(0.0))
//                }
//            }
//            else -> {
//                //getExternalFilesDir()을 사용하면 /Android/data/패키지명/files 폴더가 자동으로 생성되며
//                //READ_EXTERNAL_STORAGE 퍼미션을 별도로 획득하지 않아도 파일의 접근이 가능하다.
//                thread {
//                    myExternalFile = File(getExternalFilesDir(null), fileName)
//                    fileContent = dataForSave(recordData)
//                    try {
//                        val fileOutPutStream = FileOutputStream(myExternalFile)
////                        Log.d("fileTest", myExternalFile.path)
//                        fileOutPutStream.write(fileContent.toByteArray())
//                        fileOutPutStream.close()
//
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                }
//            }
//        }
//    }
//    private fun dataForSave(recorded : ArrayList<ArrayList<Double>>) : String {
//        var result = ""
//        var n = 0
//
//        for (line in recorded) {
//            result += dataType[n] + "\t"
//            for (data in line) {
//                result += data.toString() + "\t"
//            }
//            result += "\n"
//            n++
//        }
//
//        return result
//    }



//        fun onBtnSamplingStartClicked(v: View?) {
//
//        if (!nowSampling) {
//            btnRecord.text = "STOP RECORDING"
//            nowSampling = true
//            saveData = "${0}\t${0}\t${0}\t${0}\t${0}${0}\t${0}\t${0}\t${0}\r\n"
////                backUpPosition[0] = positionX
////                backUpPosition[1] = positionY
////                backUpSaveData = saveData
//            indoorLocalization.totalStepCount = 0
//            Toast.makeText(this, "지금부터 데이터를 기록합니다.", Toast.LENGTH_SHORT).show()
//        } else {
//            btnRecord.text = "START RECORDING"
//            nowSampling = false
//            writeFile("default${autoFileNum++}.txt" , saveData)
//        }
//
//    }
/////////////////////////////////test용////////////////////////////////////////////

    inner class MyThread3 : Thread() {
        override fun run() {
            while (true) {
                handlerZ.sendEmptyMessage(0)
                try {
                    sleep(50)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun threadStart() {
        val thread3 = MyThread3()
        thread3.isDaemon = true
        thread3.start()
    }
    /////////////////////////////////test용////////////////////////////////////////////

    /////////////////////////////////test용////////////////////////////////////////////
}