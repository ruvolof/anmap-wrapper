package com.werebug.anmapwrapper.parser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.werebug.anmapwrapper.MainActivity
import com.werebug.anmapwrapper.databinding.ActivityParserBinding
import java.io.File
import java.io.FileInputStream

class ParserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParserBinding
    private lateinit var hostAdapter: HostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.hostListRecyclerView.layoutManager = LinearLayoutManager(this)
        val hosts = FileInputStream(File(filesDir, MainActivity.XML_OUTPUT_FILE)).use {
            XMLOutputParser().parse(it)
        }
        hostAdapter = HostAdapter(hosts)
        binding.hostListRecyclerView.adapter = hostAdapter
    }
}