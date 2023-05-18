package com.example.cslabposco

import java.util.*

internal class MovingAverage constructor(period : Int){
    private var window : Queue<Double> = LinkedList()
    private val period : Int = if (period > 0) period else 0
    private var sum : Double = 0.0

    fun newData(data : Double) {
        sum += data
        window.add(data)
        if (window.size > period)
            sum -= window.poll()
    }

    fun getAvg() : Double {
        return if (window.isEmpty()) 0.0 else (sum / window.size)
    }
}