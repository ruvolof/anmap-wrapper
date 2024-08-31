package com.werebug.anmapwrapper.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class XMLOutputParser {

    fun parse(inputStream: InputStream): List<Host> {
        val hosts = mutableListOf<Host>()
        var hostnames = mutableSetOf<String>()
        var ipAddress: String? = null
        var macAddress: String? = null
        var services = mutableListOf<Service>()

        fun parsePortTag(parser: XmlPullParser) {
            val portId = parser.getAttributeValue(null, "portid")?.toInt()
            var portState = ""
            var serviceName = ""
            var serviceProduct = ""
            var serviceVersion = ""
            while (parser.next() != XmlPullParser.END_TAG || parser.name != "port") {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "service" -> {
                            serviceName = parser.getAttributeValue(null, "name") ?: ""
                            serviceProduct = parser.getAttributeValue(null, "product") ?: ""
                            serviceVersion = parser.getAttributeValue(null, "version") ?: ""
                        }

                        "state" -> {
                            portState = parser.getAttributeValue(null, "state")
                        }
                    }
                }
            }
            portId?.let {
                services.add(
                    Service(
                        port = it,
                        state = portState,
                        name = serviceName,
                        product = serviceProduct,
                        version = serviceVersion
                    )
                )
            }
        }

        fun parseAddressTag(parser: XmlPullParser) {
            val type = parser.getAttributeValue(null, "addrtype")
            when (type) {
                "ipv4" -> {
                    ipAddress = parser.getAttributeValue(null, "addr")
                }

                "mac" -> {
                    macAddress = parser.getAttributeValue(null, "addr")
                }
            }
        }

        fun parseHostTag(parser: XmlPullParser) {
            while (parser.next() != XmlPullParser.END_TAG || parser.name != "host") {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "hostname" -> {
                            hostnames.add(parser.getAttributeValue(null, "name"))
                        }

                        "address" -> {
                            parseAddressTag(parser)
                        }

                        "port" -> {
                            parsePortTag(parser)
                        }
                    }
                }
            }
            ipAddress?.let { ip ->
                if (hostnames.isEmpty()) {
                    hostnames.add("N/A")
                }
                hosts.add(
                    Host(
                        hostnames = hostnames,
                        ipAddress = ip,
                        macAddress = macAddress,
                        services = services
                    )
                )
            }
            services = mutableListOf()
            hostnames = mutableSetOf()
            ipAddress = null
            macAddress = null
        }

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "host") {
                    parseHostTag(parser)
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return hosts
    }
}