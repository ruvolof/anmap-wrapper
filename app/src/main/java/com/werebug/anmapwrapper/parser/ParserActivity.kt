package com.werebug.anmapwrapper.parser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.werebug.anmapwrapper.databinding.ActivityParserBinding
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class ParserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParserBinding
    private lateinit var hostAdapter: HostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.hostListRecyclerView.layoutManager = LinearLayoutManager(this)
        val file = File(filesDir, "scan_output.xml")
        val inputStream: InputStream = FileInputStream(file)
        val parser = XMLOutputParser()
        val hosts = parser.parse(inputStream)
        hostAdapter = HostAdapter(hosts)
        binding.hostListRecyclerView.adapter = hostAdapter
    }
}