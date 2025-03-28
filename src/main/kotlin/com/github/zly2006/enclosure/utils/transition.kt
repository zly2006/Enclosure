package com.github.zly2006.enclosure.utils

import org.jetbrains.annotations.Range
import kotlin.math.cos

sealed class TransitionValue (
    private val transitionTime: Int
){
    private var lastSetTarget: Long = 0
    private var fromValue: Float = 0F
    private var toValue: Float = 0F

    protected abstract fun calc(progress: @Range(from = 0, to = 1) Float): Float

    fun calcCurrent(): Float {
        val elapsedTime: Long = System.currentTimeMillis() - lastSetTarget
        if (elapsedTime >= transitionTime.toDouble()) return toValue

        val progress = elapsedTime / transitionTime.toFloat()
        return fromValue + (toValue - fromValue) * calc(progress)
    }

    fun setToValue(toValue: Float) {
        this.fromValue = calcCurrent()
        this.lastSetTarget = System.currentTimeMillis()
        this.toValue = toValue
    }
}

class CosTransitionValue(transitionTime: Int): TransitionValue(transitionTime) {
    override fun calc(progress: Float) = (1 - cos(progress * Math.PI)).toFloat() / 2f
}