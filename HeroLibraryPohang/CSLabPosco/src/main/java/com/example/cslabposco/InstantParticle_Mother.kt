package com.example.cslabposco

internal class InstantParticle_Mother constructor(angle : Int) {
    var particle_children_list = listOf<InstantParticle>()
    var my_angle : Int = 0
    var win_num : Int = 0
    init{
        my_angle = angle
    }
    fun appendChildren(position: ArrayList<Float>, map_value: Array<Double>){
        particle_children_list = particle_children_list.plus(InstantParticle(position, map_value))
    }
    fun removeChildren(idx: Int){
        particle_children_list = particle_children_list.minus(particle_children_list[idx])
    }

}