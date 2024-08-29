package com.werebug.anmapwrapper.parser

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.werebug.anmapwrapper.MainActivity
import com.werebug.anmapwrapper.R
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class ParserActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var hostAdapter: HostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_parser)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.host_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val file = File(filesDir, "scan_output.xml")
        val inputStream: InputStream = FileInputStream(file)
        val parser = XMLOutputParser()
        val hosts = parser.parse(inputStream)
        Log.d(MainActivity.LOG_TAG, hosts.toString())
        hostAdapter = HostAdapter(hosts)
        recyclerView.adapter = hostAdapter
    }
}