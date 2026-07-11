package com.maxkach.swipingcardssample.common

import com.maxkach.swipingcards.SwipeDirection

/** A swipe is a positive action (like/select/recruit) when it goes Right or Up. */
fun isPositiveSwipe(direction: SwipeDirection): Boolean =
    direction == SwipeDirection.Right || direction == SwipeDirection.Up
