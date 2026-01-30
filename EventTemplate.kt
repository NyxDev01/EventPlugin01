package com.eventplugin.data

data class EventTemplate(
    val name: String,
    val eventMode: String,
    val minPlayers: Int,
    val gracePeriod: Int,
    val borderPhases: MutableMap<Int, Pair<Int, Int>>,
    val kitSettings: MutableMap<String, Any>,
    val surgeSettings: MutableMap<String, Any>,
    val partySettings: MutableMap<String, Any>
)