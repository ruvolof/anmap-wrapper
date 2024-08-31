package com.werebug.anmapwrapper.parser

data class Service(
    val port: Int,
    val state: String,
    val name: String,
    val product: String,
    val version: String
)