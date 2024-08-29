package com.werebug.anmapwrapper.parser

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.werebug.anmapwrapper.MainActivity
import com.werebug.anmapwrapper.R

class ServiceAdapter(private val serviceList: List<Service>) :
    RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val portTextView: TextView = view.findViewById(R.id.port_id_text_view)
        val serviceNameTextView: TextView = view.findViewById(R.id.service_name_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.parsed_service_view, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = serviceList[position]
        holder.portTextView.text = service.port.toString()
        holder.serviceNameTextView.text = service.name
    }

    override fun getItemCount(): Int = serviceList.size
}