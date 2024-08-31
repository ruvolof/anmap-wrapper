package com.werebug.anmapwrapper.parser

data class Host(
    val hostnames: Set<String>,
    val ipAddress: String,
    val macAddress: String?,
    val services: List<Service>
)