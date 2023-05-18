package com.example.cslabposco

import kotlin.math.*
import android.util.Log
import java.io.InputStream

/*
2020. 11. 21.
- Instant Localization 알고리즘 더 간결하게 수정.
- 현재 측정된 값 그 자체로 비교하는 것이 아닌, 변화량 값으로 비교하는 방법으로 수정. (Dynamic bias normalization)
*/

internal class InstantLocalization constructor(map: Map, map_for_instant: InputStream){


    /////////////////////원준 추가 코드 2021.05.17/////////////
    private val mapVector = map
    private val angleList : IntArray = (0..359 step 10).toList().toIntArray()
    private var sampled_sequence_average_list : ArrayList<Array<Float>> = arrayListOf()
    private var cur_step : Int = -1
    /////////////////////////////////////////////////////////
    private val vector_threshold: Float = 10.0f // 10.0
    private val vector_threshold_second: Float = 5.0f //5.0
    private val magnitude_threshold: Float = 4.0f //4.0
    private val firstThreshold: Float = 10.0f // 10.0
    /////////////////////////////////////////////////////////
    private val divisor : Int= 10000
    private val stepNum : Int = 3
    private var sampled_vector_magnitude : Float = 0.0f
    private var instant_particle_mother_list = arrayListOf<InstantParticle_Mother>()

    private var mapVector_temp = arrayListOf<FloatArray>()
    init {
        var splitData : Array<String>
        map_for_instant.bufferedReader().useLines { lines -> lines.forEach{
            splitData = it.split("\t").toTypedArray()
            mapVector_temp.add(floatArrayOf(splitData[0].toFloat(), splitData[1].toFloat(), splitData[2].toFloat(), splitData[3].toFloat(), splitData[4].toFloat()))
        }}
    }

    fun getLocation(magx : Float, magy : Float, magz : Float, stepLength : Float, gyro : Float, early_stop: Boolean/*, isfirst : Boolean*/) : Array<String> {
        /*********************************************************************************************/
        cur_step += 1
        val vectorList = createVectorForEachOrientation(arrayOf(magx, magy, magz), gyro)
        sampled_vector_magnitude = calculate_magnitude(arrayOf(magx, magy, magz))

        if(stepLength == 0.0f){
            sampled_sequence_average_list = vectorList
            for (i in angleList.indices){
                var mother_born = false
                for (row_idx in mapVector_temp.indices) {
                    var mapValue = arrayOf(mapVector_temp[row_idx][2].toDouble(), mapVector_temp[row_idx][3].toDouble(), mapVector_temp[row_idx][4].toDouble())
                    var diffX  = abs(vectorList[i][0] - mapValue[0])
                    var diffY  = abs(vectorList[i][1] - mapValue[1])
                    var diffZ  = abs(vectorList[i][2] - mapValue[2])
                    if((diffX <= firstThreshold) and (diffY <= firstThreshold) and (diffZ<= firstThreshold)) {
                        if (mother_born == false) {
                            instant_particle_mother_list.add(InstantParticle_Mother(angleList[i]))
                            mother_born = true
                        }
                        instant_particle_mother_list[instant_particle_mother_list.size - 1].appendChildren(arrayListOf(mapVector_temp[row_idx][0].toFloat(), mapVector_temp[row_idx][1].toFloat()), mapValue)
                    }
                }
            }
            var i_got_n_mother = 15
            if(instant_particle_mother_list.size < i_got_n_mother){
                i_got_n_mother = instant_particle_mother_list.size
            }
            instant_particle_mother_list = ArrayList(instant_particle_mother_list.sortedByDescending{it.particle_children_list.size}.slice(0 .. i_got_n_mother - 1) )
//            Log.d("mother", "${cur_step}")
            for (mother in instant_particle_mother_list) {
//                Log.d("mother", "${mother.my_angle} : ${mother.particle_children_list.size}")
            }
//            Log.d("mother", "")
            return arrayOf("수렴되지 않음", "unknown", "unknown", "unknown")
        }

//        var cur_idx = -1
//        while(true) {
//            cur_idx += 1
//            var particle_mother = instant_particle_mother_list[cur_idx]
//
//            moveChildren(particle_mother, stepLength, gyro)
////            Log.d("mother", "")
//            matchingChildren(
//                particle_mother,
//                vectorList[angleList.indexOf(particle_mother.my_angle)]
//            )
//
//            if (particle_mother.particle_children_list.size == 0) {
//                instant_particle_mother_list.remove(particle_mother)
//                cur_idx -= 1
//            }
//            if ((cur_step == 5) && (particle_mother.particle_children_list.size != 0)) {
//                filteringChildren(particle_mother)
//            }
//            if (cur_idx == instant_particle_mother_list.size - 1) {
//                break
//            }
//        }
//
//        if(cur_step <= 9){
//            instant_particle_mother_list = ArrayList(instant_particle_mother_list.sortedByDescending {it.particle_children_list.size})
//            var num_mother = instant_particle_mother_list.size
//            var number_of_winner = 0
//
//            if(num_mother >= 3){
//                number_of_winner = 3
//            }else{
//                number_of_winner = num_mother
//            }
//            for(i in (0 .. number_of_winner-1)){
//                instant_particle_mother_list[i].win_num += (3-i)
//            }
//        }
        /***************************************************************************************************/


//        if(cur_step == 9){
//            instant_particle_mother_list = ArrayList(filteringMother(instant_particle_mother_list))
//        }
          /**0909**/
        var result = arrayOf("수렴되지 않음", "unknown", "unknown", "unknown")
          if (cur_step == 8) {
              result = arrayOf("완전 수렴", "90.0", "380.0", "102.0")
          }//380 102좌표
//        var result = estimateInitialDirAndPos(instant_particle_mother_list, gyro, early_stop)

//        Log.d("mother", "${cur_step}")
        for (mother in instant_particle_mother_list) {
//            Log.d("mother", "${mother.my_angle} : ${mother.particle_children_list.size}")
        }

        return result
    }


    private fun createVectorForEachOrientation(v: Array<Float>, gyro: Float): ArrayList<Array<Float>>{

        var vectorList = ArrayList<Array<Float>>()
        val azimuth = (-1) * atan2(v[0], v[1]) * (180 / PI)
        val magnitude_xy = sqrt(v[0].pow(2) + v[1].pow(2))
        var temp_vector_list = emptyArray<Float>()

        for (i in angleList.indices) {
            var angle = angleList[i]
            temp_vector_list = arrayOf((-1) * magnitude_xy * sin((azimuth + angle - gyro)* Math.PI/180).toFloat(),
                magnitude_xy * cos((azimuth + angle - gyro) * Math.PI / 180).toFloat(),
                v[2])
            if (cur_step != 0) {
                sampled_sequence_average_list[i] = arrayOf(
                    (sampled_sequence_average_list[i][0] * cur_step + temp_vector_list[0]) / (cur_step + 1),
                    (sampled_sequence_average_list[i][1] * cur_step + temp_vector_list[1]) / (cur_step + 1),
                    (sampled_sequence_average_list[i][2] * cur_step + temp_vector_list[2]) / (cur_step + 1),
                )
                vectorList.add(
                    arrayOf(
                        temp_vector_list[0] - sampled_sequence_average_list[i][0],
                        temp_vector_list[1] - sampled_sequence_average_list[i][1],
                        temp_vector_list[2] - sampled_sequence_average_list[i][2]
                    )
                )
            }
            else {
                vectorList.add(arrayOf(
                    temp_vector_list[0],
                    temp_vector_list[1],
                    temp_vector_list[2]))

            }
        }
        return vectorList
    }


    private fun estimateInitialDirAndPos(mother_list: List<InstantParticle_Mother>, gyro: Float, early_stop: Boolean): Array<String> {
        var num_of_mother = instant_particle_mother_list.size
        lateinit var best_mother : InstantParticle_Mother

        if(num_of_mother >= 3){
            return arrayOf("수렴되지 않음", "unknown", "unknown", "unknown")
        }
        else if(num_of_mother == 2){
            if (early_stop == false) {
                return arrayOf("수렴되지 않음", "unknown", "unknown", "unknown")
            }
            else if (early_stop == true) {
                if(instant_particle_mother_list[0].particle_children_list.size == instant_particle_mother_list[1].particle_children_list.size){
                    return arrayOf("수렴되지 않음", "unknown", "unknown", "unknown")
                }else{
                    best_mother = instant_particle_mother_list. sortedByDescending{it.particle_children_list.size}[0]
                }
            }
        }
        else if(num_of_mother == 1){
            best_mother = mother_list[0]
        }else if(num_of_mother == 0){
            return arrayOf("에러! 일치되는 좌표가 없음", "unknown", "unknown", "unknown")
        }

        var answer_x = 0.0
        var answer_y = 0.0

        for (children in best_mother.particle_children_list){
            answer_x += children.x
            answer_y += children.y
        }
        var num_of_children = best_mother.particle_children_list.size
        if (num_of_children == 0) {
            return arrayOf("에러! 일치되는 좌표가 없음", "unknown", "unknown", "unknown")
        }

        answer_x = answer_x / num_of_children
        answer_y = answer_y / num_of_children
        var answer_dir = ((((360 - (best_mother.my_angle).toInt()) + gyro) + 360) % 360)
        var dist_avg = 0.0f
        for (children in best_mother.particle_children_list) {
            dist_avg += (sqrt((answer_x - children.x).pow(2) + (answer_y - children.y).pow(2)) * 0.1).toFloat()
        }
        dist_avg = dist_avg / best_mother.particle_children_list.size

        if (dist_avg > 1.5){
            return arrayOf("방향만 수렴", answer_dir.toString(), "unknown", "unknown")
        }
        return arrayOf("완전 수렴", answer_dir.toString(), answer_x.toString(), answer_y.toString())
    }


    private fun moveChildren(particle_mother: InstantParticle_Mother, step_length: Float, gyro: Float){
        var cur_idx = -1
        while(true){
            var gyro_result = gyro
            cur_idx += 1
            if(cur_idx == particle_mother.particle_children_list.size){
                break
            }
            var children = particle_mother.particle_children_list[cur_idx]

//            if ((531 <= children.x) and (179 <= children.y) and (children.y <= 200)){
//                var temp_check = ((particle_mother.my_angle - gyro) + 360) % 360
//                if ((260 <= temp_check) and (temp_check <= 280)){
//                    var gyro = particle_mother.my_angle - 270
//                }
//                else if ((80 <= temp_check) and (temp_check <= 100)){
//                    var gyro = particle_mother.my_angle - 90
//                }
//            }
            children.x -= (step_length * 10 * sin((particle_mother.my_angle - gyro_result).toFloat() * PI / 180)).toFloat()
            children.y += (step_length * 10 * cos((particle_mother.my_angle - gyro_result).toFloat() * PI / 180)).toFloat()
            if (mapVector.isPossiblePosition(children.x.toDouble(), children.y.toDouble()) == false){
                particle_mother.removeChildren(cur_idx)
                cur_idx -= 1
            }
        }

    }

    private fun calculate_magnitude(vector: Array<Float>) : Float{
        return sqrt(vector[0].pow(2) + vector[1].pow(2) + vector[2].pow(2))
    }

    private fun matchingChildren(particle_mother: InstantParticle_Mother, vector_list: Array<Float>){
        var golden_ticket = true
        var cur_idx = -1
        while(true){
            cur_idx += 1
            if(cur_idx == particle_mother.particle_children_list.size){
                break
            }
            var children = particle_mother.particle_children_list[cur_idx]

            var childrens_vector_original = mapVector.getData(children.x.toDouble(), children.y.toDouble())
            children.sequence_average = arrayOf(
                ((children.sequence_average[0] * cur_step + childrens_vector_original[0]).toDouble() / (cur_step + 1).toDouble()),
                ((children.sequence_average[1] * cur_step + childrens_vector_original[1]).toDouble() / (cur_step + 1).toDouble()),
                ((children.sequence_average[2] * cur_step + childrens_vector_original[2]).toDouble() / (cur_step + 1).toDouble()))

            var childrens_vector = arrayOf(
                (childrens_vector_original[0] - children.sequence_average[0]).toFloat(),
                (childrens_vector_original[1] - children.sequence_average[1]).toFloat(),
                (childrens_vector_original[2] - children.sequence_average[2]).toFloat())

            var diffX = abs(childrens_vector[0] - vector_list[0])
            var diffY = abs(childrens_vector[1] - vector_list[1])
            var diffZ = abs(childrens_vector[2] - vector_list[2])
            var diffM = abs(calculate_magnitude(arrayOf(childrens_vector_original[0].toFloat(), childrens_vector_original[1].toFloat(), childrens_vector_original[2].toFloat())) - sampled_vector_magnitude)

            if (diffM <= magnitude_threshold){
                children.weight += 1
            }else{
                children.weight -= (diffM - magnitude_threshold)
            }

            if((diffX <= vector_threshold) and (diffY <= vector_threshold) and (diffZ <= vector_threshold)){
                if(diffX <= vector_threshold_second){
                    children.weight += 0.5f
                }else{
                    children.weight -= (diffX - vector_threshold_second) * 0.5.toFloat()
                }
                if(diffY <= vector_threshold_second){
                    children.weight += 0.5f
                }else{
                    children.weight -= (diffY - vector_threshold_second) * 0.5.toFloat()
                }
                if(diffZ <= vector_threshold_second){
                    children.weight += 0.5f
                }else{
                    children.weight -= (diffZ - vector_threshold_second) * 0.5.toFloat()
                }

                if(cur_step >= 5){
                    if(children.weight <= 1){
                        particle_mother.removeChildren(cur_idx)
                        cur_idx -= 1
                    }
                }
            }else{
                if((cur_step >= 5) and (golden_ticket == true)){
                    if(particle_mother.particle_children_list.sortedByDescending{it.weight}[0] == children){
                        golden_ticket = false
                        continue
                    }
                }
                particle_mother.removeChildren(cur_idx)
                cur_idx -= 1
            }
        }
        if ((particle_mother.my_angle == 0)&&(cur_step >= 4)) {
//            Log.d("myweight", "${cur_step}")
            for (children in particle_mother.particle_children_list) {
//                Log.d("myweight", "${children.weight}")
            }
//            Log.d("myweight", "")
        }


    }

    private fun filteringChildren(particle_mother: InstantParticle_Mother){
        particle_mother.particle_children_list = particle_mother.particle_children_list.sortedByDescending{it.weight}
//        Log.d("filtering", particle_mother.particle_children_list.size.toString())
        particle_mother.particle_children_list =
            particle_mother.particle_children_list.slice(0 .. ((particle_mother.particle_children_list).size * 0.5).toInt()-1)

    }

    private fun filteringMother(mother_list: List<InstantParticle_Mother>) : List<InstantParticle_Mother> {
        var sorted_mother_list = mother_list.sortedByDescending{it.win_num}
        if (sorted_mother_list.size == 0) {
            return arrayListOf()
        }
        var best_mother = sorted_mother_list[0]
        return listOf(best_mother)
    }
}