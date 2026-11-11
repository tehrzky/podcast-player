package com.example.podcast.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object PodcastMotion {
    fun <T> springSpec(reduceMotion: Boolean): AnimationSpec<T> {
        return if (reduceMotion) {
            tween(durationMillis = 150, easing = FastOutSlowInEasing)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        }
    }

    fun <T> morphSpec(reduceMotion: Boolean): AnimationSpec<T> {
        return if (reduceMotion) {
            tween(durationMillis = 100)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }
    }
}
