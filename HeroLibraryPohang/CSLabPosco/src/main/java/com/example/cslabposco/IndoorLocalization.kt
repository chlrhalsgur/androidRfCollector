package com.example.cslabposco

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.kircherelectronics.fsensor.filter.gyroscope.OrientationGyroscope
import org.apache.commons.math3.complex.Quaternion
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.math.*

/**
 * 자기장 기반 실내측위 결과를 얻기 위한 클래스
 *
 * SensorEventListener 를 implementation 한 후, override 된 onSensorChanged 메소드 내부에서 본 클래스의 sensorChanged 메소드를 지속적으로 호출
 *
 * Required Permissions -> WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, INTERNET, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION,
 *
 * Required Sensors -> Sensor.TYPE_ACCELEROMETER, TYPE_MAGNETIC_FIELD, TYPE_GYROSCOPE, TYPE_LINEAR_ACCELERATION, TYPE_ROTATION_VECTOR
 *
 *
 */
//class AppforContext2: Application() {
//    init{
//        instance = this
//    }
//
//    companion object {
//        private var instance:AppforContext2? = null
//        fun myappContext2() : Context {
//            return instance!!.applicationContext
//        }
//    }
//}

class IndoorLocalization {

    /**
     * 사용자의 식별정보 및 맵 데이터 확보
     *
     * @param inputStreamOfMap  자기장 맵 파일의 inputStream     ex) raw 폴더 운용시  resources.openRawResource(R.raw.ktx2f)
     */
    constructor(inputStreamOfMap : InputStream, inputStreamOfInstant : InputStream , buildingInfoFileStream : InputStream) {
        //myUUID = uuid
        map = Map(inputStreamOfMap)
        instantLocalization = InstantLocalization(map, inputStreamOfInstant)
        magneticQueue = arrayListOf()   // ###
        instantResult = arrayOf("", "", "", "") // ###
        sendSensorData = arrayListOf() //sumin
        val thread = InstantLocalizationThread() // ###
        thread.start()


        buildingInfo = BuildingInfo(buildingInfoFileStream)
    }

    /**
     * 사용자의 식별정보, 맵 데이터 및 진입 좌표 확보
     *
     * @param inputStreamOfMap  자기장 맵 파일의 inputStream     ex) raw 폴더 운용시  resources.openRawResource(R.raw.intersect)
     * @param x        비콘 위치에 기반하여 전달받은 x 좌표
     * @param y        비콘 위치에 기반하여 전달받은 y 좌표
     * @param heading  사용자가 이동하던 방향
     */
    constructor(inputStreamOfMap : InputStream, x : Float, y : Float, heading : Double) {
        //myUUID = uuid
        map = Map(inputStreamOfMap)
        orientationGyroscope.reset()        ////// 200328_원준_수정
        gyroCaliValue = Math.toRadians(heading).toFloat()
        particleOn = true
        particleFilter = ParticleFilter(map, 100, round(x).toInt(), round(y).toInt(), 20)
        runningInsLoca = false
        isFirst = false
        returnIns = "Given position (${round(x).toInt()}, ${round(y).toInt()})"
        runningElevatorThread = false /**0819**/
        runningEscalatorThread = false /**0819**/
    }

    private val lstmServer by lazy {
        LSTMServer()
    }

    private var map : Map //= Map(inputStreamTotal)   ////// 200330_원준_수정
    private lateinit var particleFilter : ParticleFilter    ////// 200328_원준_수정
    private var currentFloor : String = ""  ////// 200330_원준_수정
    //private var myUUID : String //= uuid

    private val accXMovingAverage : MovingAverage = MovingAverage(10)
    private val accYMovingAverage : MovingAverage = MovingAverage(10)
    private val accZMovingAverage : MovingAverage = MovingAverage(10)
    private val stepLengthMovingAverage : MovingAverage = MovingAverage(10)
    private val pressureMovingAverage : MovingAverage = MovingAverage(10)

    private var quaternion = FloatArray(4)
    private var magMatrix = FloatArray(3)
    private var accMatrix = FloatArray(3)
    private var roVecMatrix = FloatArray(5)
    private var pressure = FloatArray(1)

    private var linearAccX : Float = 0f
    private var linearAccY : Float = 0f
    private var linearAccZ : Float = 0f
    private var transformedAccX : Float = 0f
    private var transformedAccY : Float = 0f
    /*private*/ var transformedAccZ : Float = 0f
    private var quaRoll : Float = 0f
    private var quaPitch : Float = 0f
    private var quaYaw : Float = 0f
    private var timeStamp : Double = 0.0
    private var upPeakTime : Double = 0.0
    private var downPeakTime : Double = 0.0
    private var magnitudeOfMagnetic : Double = 0.0

    private val oneStepPressure : Queue<Double> = LinkedList()
    private var totalStepCount : Int = 0
    private var oneStepSum : Double = 0.0
    private var lastStepLength : Double = 0.0
    private var maxAccZ : Double = 0.0
    private var minAccZ : Double = 0.0
    private var k : Double = 0.445
    private var isMapRotationMode : Boolean = false
    private var isUpPeak : Boolean = false
    private var isDownPeak : Boolean = false
    private var isStepFinished : Boolean = false
    private var particleOn : Boolean = false
    private var isSetFloor : Boolean = true

    private var isSensorStabled : Boolean = false
    private var magStableCount : Int = 100
    private var accStableCount : Int = 50

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 원준 변수 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    private val orientationGyroscope by lazy {
        OrientationGyroscope()
    }
    private lateinit var instantLocalization : InstantLocalization
    private var rotation = FloatArray(3)
    private var fusedOrientation = FloatArray(3)
    private var mRotationMatrix = FloatArray(16)
    private var mRot = FloatArray(3)
    private var mAzimuth : Float = 0f
    private var angleA : Double = 0.0
    private var caliX : Double= 0.0
    private var caliY : Double= 0.0
    private var caliZ : Double= 0.0
    private var nowPosition = arrayOf(0.0, 0.0)
    private var gyroCaliValue : Float = 0f
    private var dir = 0.0

    private var isFailInstantLocalization : Boolean = false
    private var runningInsLoca : Boolean = true
    private var isFirst : Boolean = true
    private lateinit var magneticQueue : ArrayList<ArrayList<Double>> //= arrayListOf()   // ###
    private lateinit var instantResult : Array<String> //= arrayOf("", "", "", "") // ###
    private var stepCount : Int = 0

    private var returnGyro : String = ""
    private var returnIns : String = ""
    private var returnX : String = "unknown"
    private var returnY : String = "unknown"

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ 수민 변수 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    private lateinit var buildingInfo : BuildingInfo /**0819**/
    var startfloor : Int = 5
    var testuserX : Double = 0.0
    var testuserY : Double = 0.0
    lateinit var sendSensorData : ArrayList<Double>

    var floorchange : String = "false"


    var buildingInfoResult : BuilInfo? = null
    private var getInZone : Boolean = false
    private var acontext:Context = AppforContext.myappContext()

    //@@@에스컬레이터@@
    private var runningEscalatorThread : Boolean = false /**0819**/
    private lateinit var escalatorModule : Escalator
    private lateinit var whereUserAfterEscalator : Array<Double>
    var makeElevatorRun : Boolean = false
    var makeEscalatorRun : Boolean = false

    //@@@엘리베이터@@
    private var runningElevatorThread : Boolean = false /**0819**/
    private lateinit var elevatorModule : Elevator
    lateinit var whereUserAfterElevator : Array<Double>
    private var elvToastThread:Boolean = false


    ////데이터 저장하기
    private val NUMOFDATA = 3
    private val dataType = arrayOf( "elvMoveDirection","pressureCnt", "elvPressureGap"/*,"pressure", "Gradient", "Gradientaverage10","Gradientaverage20", "mag_X4", "mag_Y4", "mag_Z4", "mag_X20", "mag_Y20", "mag_Z20", "mag_X4", "mag_Y4", "mag_Z4"*/)
    var isStartRecording : Boolean = false
    var fileName = "DTW.txt"//"odr_test.txt"
    var fileContent = ""
    var recordData = arrayListOf<ArrayList<Double>>()
    private lateinit var myExternalFile: File
    var sendelevatorFile : Boolean = false
    var filewriteDown : Boolean = false





    init {
        //lstmServer.play()
        orientationGyroscope.reset()
//        instantLocalization = InstantLocalization(map)
//        val thread = InstantLocalizationThread() // ###
//        thread.start()

    }

    private fun axisTransform(axis : Char, rawDataX : Float, rawDataY : Float, rawDataZ : Float) : Double {
        return when(axis) {
            'x' -> { (cos(-quaYaw) * cos(-quaRoll) * rawDataX + (cos(-quaYaw) * sin(-quaRoll) * sin(quaPitch) - sin(-quaYaw) * cos(quaPitch)) * rawDataY + (cos(-quaYaw) * sin(-quaRoll) * cos(quaPitch) + sin(-quaYaw) * sin(quaPitch)) * rawDataZ).toDouble() }
            'y' -> { (sin(-quaYaw) * cos(-quaRoll) * rawDataX + (sin(-quaYaw) * sin(-quaRoll) * sin(quaPitch) + cos(-quaYaw) * cos(quaPitch)) * rawDataY + (sin(-quaYaw) * sin(-quaRoll) * cos(quaPitch) - cos(-quaYaw) * sin(quaPitch)) * rawDataZ).toDouble() }
            'z' -> { (-sin(quaRoll) * rawDataX + (cos(quaRoll) * sin(-quaPitch)) * rawDataY + (cos(quaRoll) * cos(-quaPitch)) * rawDataZ).toDouble() }
            else -> -966.966966
        }
    }


    private fun sensorReady(event: SensorEvent) : Boolean {
        if (isSensorStabled)
            return true

        when(event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                accXMovingAverage.newData(event.values[0].toDouble())
                accYMovingAverage.newData(event.values[1].toDouble())
                accZMovingAverage.newData(event.values[2].toDouble())

                accStableCount += if (accXMovingAverage.getAvg() in -0.3..0.3 && accYMovingAverage.getAvg() in -0.3..0.3 && accZMovingAverage.getAvg() in -0.3..0.3) -1 else 1

                accStableCount = if (accStableCount > 50) 50 else accStableCount
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magStableCount--
            }
        }

        isSensorStabled = accStableCount<0 && magStableCount<=0
//        if (isSensorStabled) {
//            Glide.with(this).pauseAllRequests()
//            mFrameLayout.removeView(loadingLayout)
//            Toast.makeText(this, "Let's go~", Toast.LENGTH_SHORT).show()
//        }
        return isSensorStabled
    }

    private fun resetPosition() : String {
        // 수정 필요
//        instantLocalization = InstantLocalization(map, inputStreamOfInstant)
//        Log.d("mVector3", "resetPosition Call!!")
        instantResult = arrayOf("", "", "", "")
        returnIns = ""
        runningInsLoca = true
        isFailInstantLocalization = false
        magneticQueue.clear()
        magneticQueue.add(arrayListOf(caliX, caliY, caliZ, 0.0, dir))
        totalStepCount = 0
        val thread = InstantLocalizationThread() // ###

//        val thread2 = ElevatorThread() /**0819**/
//        val thread3 = EscalatorThread() /**0819**/

        thread.start()
//        thread2.start() /**0819**/
//        thread3.start() /**0819**/

        /////////////////////////////////////////////////////////
        return "reset....."
    }

    /**
     * 측위 결과를 얻기 위한 메소드(실행 초반에는 센서가 안정화 될 때 까지 "The sensors is not ready yet" 리턴)
     *
     * ex)
     *
     * override fun onSensorChanged(event: SensorEvent?) {
     *
     * var result = indoorLocalization.sensorChanged(event)...
     *
     *
     * @param event SensorEvent   ex) override fun onSensorChanged(event: SensorEvent?) {
     * @return String array, size 5  -> {Gyro, 초기 위치 인식 상태, count, X측위좌표, Y측위좌표}
     */
    fun sensorChanged(event: SensorEvent?) : Array<String> {

        if ((event?:false)?.let {sensorReady(event!!)}) {
            when(event!!.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> accMatrix = event.values.clone()
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magMatrix = event.values.clone()
                    magnitudeOfMagnetic = sqrt(magMatrix[0].pow(2) + magMatrix[1].pow(2) + magMatrix[2].pow(2) ).toDouble()
                    if ( accMatrix.isNotEmpty() && magMatrix.isNotEmpty() ) {
                        var I = FloatArray(9)
                        var success = SensorManager.getRotationMatrix(mRotationMatrix, I, accMatrix, magMatrix)
                        mRot[0] = mRotationMatrix[0] * magMatrix[0] + mRotationMatrix[1] * magMatrix[1] + mRotationMatrix[2] * magMatrix[2]
                        mRot[1] = mRotationMatrix[4] * magMatrix[0] + mRotationMatrix[5] * magMatrix[1] + mRotationMatrix[6] * magMatrix[2]
                        mRot[2] = mRotationMatrix[8] * magMatrix[0] + mRotationMatrix[9] * magMatrix[1] + mRotationMatrix[10] * magMatrix[2]
                        if (success) {
                            var orientation = FloatArray(3)
                            SensorManager.getOrientation(mRotationMatrix, orientation)
                            mAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        }
                        // Instant Localization 끝나기 전까지는 자기장 x, y가 글로벌 좌표계에서의 nonCali 값으로 들어가야됨.    ////// 200319_원준_수정
                        // 글로벌 좌표계에서의 nonCali를 위해 그냥 angleA 만 바꿔주면 됨.
                        angleA = if (particleOn) {
                            (mAzimuth - Math.toDegrees(fusedOrientation[0].toDouble()) + 360) % 360    // Instant Localization 에서 받아온 초기 방향 값을 사용 ////// 200319_원준_수정
                        } else {
                            (mAzimuth.toDouble() + 360) % 360
                        }
                        ////////////////////
                        caliX = -1 * sqrt(magnitudeOfMagnetic.pow(2) - mRot[2].pow(2)) * sin(angleA * PI / 180)
                        caliY = sqrt(magnitudeOfMagnetic.pow(2) - mRot[2].pow(2)) * cos(angleA * PI / 180)
                        caliZ = mRot[2].toDouble()
                        /////////////////////////////////////  ////// 200319_원준_수정
                        if (isFirst) {
                            if (mRot[2] != 0f) {
                                isFirst = false
//                                Log.d("mVector", "${caliX}, ${caliY}, ${caliZ}")
                                magneticQueue.add(arrayListOf(caliX, caliY, caliZ, 0.0, dir))
                            }
                        }


                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    System.arraycopy(event.values, 0, rotation, 0, event.values.size)
                    if (!orientationGyroscope.isBaseOrientationSet)
                        orientationGyroscope.setBaseOrientation(Quaternion.IDENTITY)
                    else
                        fusedOrientation = orientationGyroscope.calculateOrientation(rotation, event.timestamp)

                    fusedOrientation[0] += gyroCaliValue ////// 200328_원준_수정
                    dir = (Math.toDegrees(fusedOrientation[0].toDouble()) + 360) % 360
                    //dir = round(dir/10) * 10   // 각도 smoothing
                }
                Sensor.TYPE_ROTATION_VECTOR -> {
                    roVecMatrix = event.values.clone()
                    if (roVecMatrix.isNotEmpty()) {
                        quaternion[0]= roVecMatrix[3]
                        quaternion[1]= roVecMatrix[0]
                        quaternion[2]= roVecMatrix[1]
                        quaternion[3]= roVecMatrix[2]
                    }
                }
                Sensor.TYPE_PRESSURE -> { /**수민 0806 **/
                    pressure = event.values.clone()

                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    timeStamp = System.currentTimeMillis().toString().substring(6).toDouble()

                    quaYaw = atan2(2.0 * (quaternion[3] * quaternion[0] + quaternion[1] * quaternion[2]),
                        1 - 2.0 * (quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1])).toFloat()
                    quaPitch = (-atan2(2 * (quaternion[0] * quaternion[1] + quaternion[3] * quaternion[2]).toDouble(),
                        quaternion[3] * quaternion[3] + quaternion[0] * quaternion[0] - quaternion[1] * quaternion[1] - (quaternion[2] * quaternion[2]).toDouble())).toFloat()
                    quaRoll = asin(2 * (quaternion[0] * quaternion[2] - quaternion[3] * quaternion[1]).toDouble()).toFloat()
                    linearAccX = event.values[0]
                    linearAccY = event.values[1]
                    linearAccZ = event.values[2] /**0813 수민**/
                    accXMovingAverage.newData(event.values[0].toDouble())
                    accYMovingAverage.newData(event.values[1].toDouble())
                    accZMovingAverage.newData(event.values[2].toDouble())


                    var accDataForStepDetection = axisTransform('z', accXMovingAverage.getAvg().toFloat(), accYMovingAverage.getAvg().toFloat(), accZMovingAverage.getAvg().toFloat())
                    transformedAccZ = accDataForStepDetection.toFloat()
                    if ( !isUpPeak && !isDownPeak && !isStepFinished ) { //가속도의 up peak점 검출 c=1, maxiaz에는 up peak의 가속도값이 저장
                        if (accDataForStepDetection > 0.5) {
                            if (accDataForStepDetection < maxAccZ) {
                                isUpPeak = true
                                upPeakTime = timeStamp
                            } else
                                maxAccZ = accDataForStepDetection
                        }
                    }
                    if ( isUpPeak && !isDownPeak && !isStepFinished ) {
                        if (accDataForStepDetection > maxAccZ) {
                            maxAccZ = accDataForStepDetection
                            upPeakTime = timeStamp
                        } else if (accDataForStepDetection < -0.3) {
                            if (accDataForStepDetection > minAccZ) {
                                isDownPeak = true
                                downPeakTime = timeStamp
                            } else
                                minAccZ = accDataForStepDetection
                        }
                    }
                    if ( isUpPeak && isDownPeak && !isStepFinished ) {
                        if (accDataForStepDetection < minAccZ) {
                            minAccZ = accDataForStepDetection
                            downPeakTime = timeStamp
                        } else if (accDataForStepDetection > 0.2)
                            isStepFinished = true
                    }
                    if ( isUpPeak && isDownPeak && isStepFinished ) {
                        var timePeak2Peak = downPeakTime - upPeakTime

                        if ( timePeak2Peak > 150 && timePeak2Peak < 500 /*&& maxAccZ < 5 && minAccZ > -4*/) {
//                            vibrator.vibrate(80)
                            totalStepCount++
//                            oneStepSum = 0.0
//                            oneStepPressure.clear()

                            // EX weinberg approach
                            lastStepLength = k * sqrt(sqrt(maxAccZ - minAccZ))
                            stepLengthMovingAverage.newData(lastStepLength)
                            var stepLength = stepLengthMovingAverage.getAvg()
                            //lstmServer.dataSaveAndSend(cali_x, cali_y, cali_z)
//                            elevatorModule.startElevator(pressure[0].toDouble(),afterFilterMagX,afterFilterMagY,afterFilterMagZ,AccZAfterlpf)
//                            resultOfElevator = elevatorModule.getElvatorResult()


                            if (particleOn) {
                                Log.d("particleOn4",particleOn.toString())
                                nowPosition = particleFilter.step(arrayOf(caliX, caliY, caliZ), dir, stepLength) //현재 좌표를 알려줌
                                buildingInfoResult = buildingInfo?.search(nowPosition[0], nowPosition[1])
                                Log.d("nowPosition[0]",nowPosition[0].toString())
                                Log.d("nowPosition[1]",nowPosition[1].toString())
                                if(buildingInfoResult != null && !getInZone) {
                                    //엘리베이터
                                    if (buildingInfoResult?.type == "EL1") {
                                        val thread2 = ElevatorThread() /**0906**/
                                        thread2.isDaemon = true
                                        thread2.start() // thread1 실행시킨다

                                        floorchange = "true"

                                        runningElevatorThread = true
                                        getInZone = true /** 210903**/
                                    }
                                    //에스컬레이터
                                    if (buildingInfoResult?.type == "Es1") {
                                        val thread3 = EscalatorThread()
                                        thread3.isDaemon = true
                                        thread3.start()

                                        runningEscalatorThread = true
                                        getInZone = true /** 210905**/
                                    }
                                }
                                /***********************************************************************************/
                                when(isFailInstantLocalization) {
                                    true -> resetPosition()
                                    else -> {
                                        if (runningInsLoca)
                                            magneticQueue.add(arrayListOf(caliX, caliY, caliZ, stepLength, dir))  // ###
                                    }
                                }



                                returnX = nowPosition[0].toString()
                                returnY = nowPosition[1].toString()
                            } else {
                                if (isFailInstantLocalization) {
                                    resetPosition()
                                } else {
//                                    Log.d("mVector", "${caliX}, ${caliY}, ${caliZ}")
                                    magneticQueue.add(arrayListOf(caliX, caliY, caliZ, stepLength, dir))  // ###
                                }
                            }
                        }


                        isUpPeak = false
                        isDownPeak = false
                        isStepFinished = false
                        maxAccZ = 0.0
                        minAccZ = 0.0
                    }

//                    elevatorModule.sendSensor(pressure[0].toDouble(), caliX, caliY, caliZ, linearAccZ.toDouble())

                }
            }
            //                threadStartForSuminModule() 이렇게 한꺼번에 부르니까 바로 밑의 runningElevatorThread는 true가 되는데 쓰레드 안에 꺼는 false가 된다.
            /****/


            if(makeElevatorRun) { /**0829**/
                elevatorModule.startElevator(pressure[0].toDouble(), caliX, caliY, caliZ, linearAccZ.toDouble()) // 엘베모듈 작동시작
                writeFileForSumin() //파일에 기록하기 위함

                if(elevatorModule.runElevator) { //파일에 기록하기 위함
                    recordData[0].add(elevatorModule.moveDirection.toDouble())
                    recordData[1].add(elevatorModule.pressureCnt.toDouble())
                    recordData[2].add(elevatorModule.elvPressureGap)

                }
            }

            if(makeEscalatorRun) { /**0829**/
                escalatorModule.startEscalator(pressure[0].toDouble(),caliX,caliY,caliZ) // 엘모듈 작동시작

            }
        } else {
            return arrayOf("The sensors is not ready yet", "The sensors is not ready yet", "The sensors is not ready yet", "The sensors is not ready yet", "The sensors is not ready yet")
        }
        returnGyro = round(dir).toString()
        return arrayOf(returnGyro, returnIns, totalStepCount.toString(), returnX, returnY, floorchange)
    }


    private inner class InstantLocalizationThread: Thread() {
        override fun run() {
            while(runningInsLoca) {
                if (magneticQueue.size > 0) {
                    val inputData = magneticQueue.removeAt(0)
//                    Log.d("mVector2", "${inputData[0].toFloat()}, ${inputData[1].toFloat()}, ${inputData[2].toFloat()}, ${inputData[3].toFloat()}, ${inputData[4].toFloat()}")
                    instantResult = instantLocalization.getLocation(inputData[0].toFloat(), inputData[1].toFloat(), inputData[2].toFloat(), inputData[3].toFloat(), inputData[4].toFloat(), early_stop = true /*isfirst = first_instant_localization*/)
                    returnIns = instantResult[0]
                    // 어플 켜지자마자 바로 instant localization 첫 연산 시작
                    if (instantResult[0] == "완전 수렴") {
                        returnX = instantResult[2]
                        returnY = instantResult[3]
                        //UnityPlayer.UnitySendMessage("Canvas", "changeScene", "완전 수렴")
                        orientationGyroscope.reset()        ////// 200328_원준_수정
                        gyroCaliValue = Math.toRadians(instantResult[1].toDouble()).toFloat()     ////// 200319_원준_수정
                        particleFilter = ParticleFilter(map, 100, instantResult[2].toFloat().toInt(), instantResult[3].toFloat().toInt(), 10) // 원래 maxR 10이였음 particleCount 100
                        //lstmServer.SyncPositionSend(instant_result[2].toDouble(), instant_result[3].toDouble())
                        particleOn = true
                        runningInsLoca = false

                        break
                    } else if (instantResult[0] == "에러! 일치되는 좌표가 없음") {
                        isFailInstantLocalization = true
                        runningInsLoca = false
                        break
                    }
                }
            }
        }
    }

    private inner class ElevatorThread : Thread() {
        override fun run() {
            while(runningElevatorThread) { /**0906**/
                if(!makeElevatorRun) {
                    elevatorModule = Elevator(1, startfloor)
                    makeElevatorRun = true /**0903 센서들 보내기**/
                }

//                if(buildingInfoResult?.type == "EL1") {
//                    elevatorModule = Elevator(1,startfloor)
//                }
//
//                if(buildingInfoResult?.type == "EL2") {
//                    elevatorModule = Elevator(2,startfloor)
//                }
//
//                if(buildingInfoResult?.type == "EL3") {
//                    elevatorModule = Elevator(3,startfloor)
//                }
                    //////////////////////////////////////////////////////////

                if (elevatorModule.resultReady) { // 모듈이 작동하고 다 끝나고 나서 결과를 넣으려고 /**0826 수민**/
                    startfloor = elevatorModule.getElvatorResult() // 사용자 위치 층 갱신 시키기
                    whereUserAfterElevator = buildingInfoResult?.arrival!! //도착 좌표
                    val thread4 = ElvToastThread() /**0906 toast메세지를 위한 쓰레드 **/
                    thread4.isDaemon = true
                    thread4.start()
                    floorchange = "true"

                    if(elevatorModule.writeDown) { /**0905 한번만 들어오게 하려고, 여러번 들어와서 file도 여러번 만들어지는 것 같아서**/
                        runningElevatorThread = false
                        getInZone = false
                        elevatorModule.runElevator = false //파일에 기록하기 끝냄
                        sendelevatorFile = true //파일 저장시키기
                        elevatorModule.writeDown = false
                        elevatorModule.resultReady = false // 결과 내면 빠져나오도록함
                        break /**0906**/
                    }
                }
            }
        }
    }

    private inner class EscalatorThread : Thread() {
        override fun run() {
            while (runningEscalatorThread) {
                if (!makeEscalatorRun) {
                    escalatorModule = Escalator(startfloor) // 몇층인지에 따라 에스컬레이터 reference 다르게 참고 하게 하기
                    makeEscalatorRun = true //센서들 보내기
                }

                if (escalatorModule.resultReady) {
                    startfloor = escalatorModule.getFloorResultEscalator() // 사용자 위치 층 갱신 시키기
                    whereUserAfterEscalator = buildingInfoResult?.arrival!! // 도착좌표
                    val thread5 = EscalToastThread() /**0906 toast메세지를 위한 쓰레드 **/
                    thread5.isDaemon = true
                    thread5.start()
//                    // 도착지점이 정해져 있는 경우
//                    if (buildingInfo.search() == "Es1") {
//                        whereUserAfterEscalator = buildingInfo.getArrival() // 도착 좌표
//                    }
//                    // 도착지점이 두가지인 경우
//                    if( buildingInfo.search() == "Es2") {
//                        buildingInfo.getArrivalForEscalatorVersion2(escalatorModule.getDirection()) // 상승인지 하강인지에 따라 도착좌표 다르게 함.
//                    }
                        if (escalatorModule.writeDown) { /**0905 한번만 들어오게 하려고, 여러번 들어와서 file도 여러번 만들어지는 것 같아서**/
                            runningEscalatorThread = false
                            getInZone = false
                            escalatorModule.runEscalator = false /**0905**/ //파일에 기록하기 끝냄
                            escalatorModule.writeDown = false /**0906**/
                            escalatorModule.resultReady = false  // 결과 내면 빠져나오도록함.
                            break /**0906**/
                        }

                    }

            }
        }
    }


    private var handler: Handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            if (msg.what == 1) {
                stepCount = 0
                resetPosition()
            }
        }
    }

    private var elvToasthandler: Handler = object : Handler(Looper.myLooper()!!) { /**0905 toast메세지를 위한 쓰레드 **/
        override fun handleMessage(msg: Message) {
            while(elevatorModule.resultReady) { /**0906**/
                Toast.makeText(acontext, " 엘베를 타고 ${startfloor}층으로 ${elevatorModule.resultMoveDirection}했습니다 : 도착좌표(${whereUserAfterElevator.joinToString()})", Toast.LENGTH_SHORT).show();
                testuserX = 3.0
                testuserY = 3.0
            }

        }
    }


    private var escalToasthandler: Handler = object : Handler(Looper.myLooper()!!) { /**0905 toast메세지를 위한 쓰레드 **/
        override fun handleMessage(msg: Message) {
            while(escalatorModule.resultReady) { /**0906**/
                Toast.makeText(acontext, "에스컬레이터를 타고 ${startfloor}층으로 ${escalatorModule.resultMoveDirection}했습니다 : 도착좌표 (${whereUserAfterEscalator.joinToString()}) ", Toast.LENGTH_SHORT).show();
                testuserX = 1.0
                testuserY = 1.0
            }
        }
    }

    private inner class AutoInsLoca: Thread() {
        override fun run() {
            while (true) {
                var w = when(stepCount) {
                    in 0..30 -> 0
                    else -> 1
                }
                handler.sendEmptyMessage(w)
                try {
                    sleep(1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private inner class ElvToastThread : Thread() { /**0905 toast메세지를 위한 쓰레드 **/
        override fun run() {
            while (elevatorModule.resultReady) {
                elvToasthandler.sendEmptyMessage(0)
                try {
                    sleep(50)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if(!runningElevatorThread) { /**0906**/
                    elvToasthandler.removeMessages(0)
                    break
                }
            }
        }
    }

    private inner class EscalToastThread : Thread() { /**0905 toast메세지를 위한 쓰레드 **/
    override fun run() {
        while (escalatorModule.resultReady) {
            escalToasthandler.sendEmptyMessage(0)
            try {
                sleep(50)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if(!runningEscalatorThread) { /**0906**/
                escalToasthandler.removeMessages(0)
                break
            }
        }
    }
    }


    private fun writeFileForSumin() { /**0903**/
        var bcontext:Context = AppforContext.myappContext()

        when(elevatorModule.runElevator) {
            true -> {
                recordData = arrayListOf<ArrayList<Double>>()
                for (i in 0 until NUMOFDATA) {
                    recordData.add(arrayListOf(0.0))
                }
            }
        }
        when(sendelevatorFile) {
            true -> {
                //getExternalFilesDir()을 사용하면 /Android/data/패키지명/files 폴더가 자동으로 생성되며
                //READ_EXTERNAL_STORAGE 퍼미션을 별도로 획득하지 않아도 파일의 접근이 가능하다.
                thread {
                        myExternalFile = File(bcontext.getExternalFilesDir(null), fileName)
                        fileContent = dataForSave(recordData)
                        try {
                            val fileOutPutStream = FileOutputStream(myExternalFile)
                            fileOutPutStream.write(fileContent.toByteArray())
                            fileOutPutStream.close()
                            Log.d("fileTest", myExternalFile.path)
                            sendelevatorFile = false

                        } catch (e: IOException) {
                        e.printStackTrace()
                        }

                }
            }
        }

    }
    private fun dataForSave(recorded : ArrayList<ArrayList<Double>>) : String { /**0903**/
        var result = ""
        var n = 0

        for (line in recorded) {
            result += dataType[n] + "\t"
            for (data in line) {
                result += data.toString() + "\t"
            }
            result += "\n"
            n++
        }

        return result
    }

//    private fun threadStartForSuminModule() {
//        val thread2 = ElevatorThread() /**0819**/
//        thread2.isDaemon = true
//        thread2.start() // thread1 실행시킨다
//
//        val thread3 = EscalatorThread() /**0819**/
//        thread3.isDaemon = true
//        thread3.start()
//
//        val thread4 = ElvToastThread() /**0905 toast메세지를 위한 쓰레드 **/
//        thread4.isDaemon = true
//        thread4.start()
//
//
//    }


}


