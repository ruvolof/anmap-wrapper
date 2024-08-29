package com.werebug.anmapwrapper.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class XMLOutputParser {

    fun parse(inputStream: InputStream): List<Host> {
        val hosts = mutableListOf<Host>()
        var hostnames = mutableSetOf<String>()
        var ipAddress: String? = null
        var services = mutableListOf<Service>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "host") {
                    eventType = parser.next()
                    while (eventType != XmlPullParser.END_TAG || parser.name != "host") {
                        if (eventType == XmlPullParser.START_TAG) {
                            when (parser.name) {
                                "hostname" -> {
                                    hostnames.add(parser.getAttributeValue(null, "name"))
                                }

                                "address" -> {
                                    ipAddress = parser.getAttributeValue(null, "addr")
                                }

                                "port" -> {
                                    val portId = parser.getAttributeValue(null, "portid")?.toInt()
                                    var serviceName = ""

                                    // Loop to get the details inside the port tag
                                    while (parser.next() != XmlPullParser.END_TAG || parser.name != "port") {
                                        if (parser.eventType == XmlPullParser.START_TAG) {
                                            when (parser.name) {
                                                "service" -> {
                                                    serviceName =
                                                        parser.getAttributeValue(null, "name") ?: ""
                                                }
                                            }
                                        }
                                    }
                                    portId?.let {
                                        services.add(
                                            Service(
                                                port = it,
                                                name = serviceName
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    ipAddress?.let { ip ->
                        if (hostnames.isEmpty()) {
                            hostnames.add("N/A")
                        }
                        hosts.add(
                            Host(
                                hostnames = hostnames,
                                ipAddress = ip,
                                services = services
                            )
                        )
                    }
                    services = mutableListOf()
                    hostnames = mutableSetOf()
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return hosts
    }
}