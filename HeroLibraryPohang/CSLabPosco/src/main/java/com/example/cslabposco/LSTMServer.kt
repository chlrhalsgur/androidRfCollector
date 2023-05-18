package com.example.cslabposco

import android.os.Handler
import android.os.Looper
import java.util.*

private const val WEB_ADDRESS = "http://rnn.korea.ac.kr:3896/CWIT"        // 위치 시각화를 위한 웹서버
private const val SERVER_URL = "http://rnn.korea.ac.kr:3896/ips"          // 딥러닝 서버
private const val SERVER_RESET_URL = "http://rnn.korea.ac.kr:3896/reset"  // 딥러닝 서버 초기화
private const val FLOOR_CHECK_URL = "http://rnn.korea.ac.kr:3896/floor"   // 기압 기반 층 판단 서버

internal class LSTMServer /*constructor(id : String)*/ {
    //private val myUniqueID : String = id
    private val mHandler : Handler = Handler(Looper.getMainLooper())
    private val x : Queue<Double> = LinkedList()
    private val y : Queue<Double> = LinkedList()
    private var nowFloor : String = ""

    fun play() {
        //Log.d("id::::", myUniqueID)
    }

    fun dataSend(x : Double, y : Double) {
//        thread {
//            var retrofit = Retrofit.Builder()
//                .baseUrl("http://rnn.korea.ac.kr:3896")
//                .addConverterFactory(GsonConverterFactory.create()) .build()
//            var positionSending : PositionSend = retrofit.create(PositionSend::class.java)
//
//            positionSending.registerPosition(PositionData(myUniqueID, x, y, nowFloor.toInt())).enqueue(object : Callback<ResponseData> {
//                override fun onFailure(call: Call<ResponseData>, t: Throwable) {
//
//                }
//                override fun onResponse(call: Call<ResponseData>, response: Response<ResponseData>) {
////                    var msg = response.body()?.responseMsg.toString()
////                    Log.d("serverResponse:::", msg)
//                }
//            })
//        }
    }

//    fun floorCheck(data : Double) : String {
//        thread {
//            var retrofit = Retrofit.Builder()
//                .baseUrl("http://rnn.korea.ac.kr:3896")
//                .addConverterFactory(GsonConverterFactory.create()) .build()
//            var floorchecking : FloorCheck = retrofit.create(FloorCheck::class.java)
//
//            floorchecking.requestFloor(PressureData(data.toString())).enqueue(object : Callback<FloorData> {
//                override fun onFailure(call: Call<FloorData>, t: Throwable) {
//
//                }
//                override fun onResponse(call: Call<FloorData>, response: Response<FloorData>) {
//                    nowFloor = response.body()?.floor.toString()
//                }
//            })
//        }
//        return ouputFloor();
//    }

//    fun getServerResetUrl() : String {
//        return SERVER_RESET_URL
//    }

    fun ouputFloor() : String{
        return nowFloor
    }

}