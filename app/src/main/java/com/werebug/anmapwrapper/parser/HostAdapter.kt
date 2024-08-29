package com.werebug.anmapwrapper.parser

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.werebug.anmapwrapper.R

class HostAdapter(private val hostList: List<Host>) :
    RecyclerView.Adapter<HostAdapter.HostViewHolder>() {

    class HostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val containerConstraintLayout: ConstraintLayout =
            view.findViewById(R.id.host_adapter_layout_container)
        val hostnamesRecyclerView: RecyclerView = view.findViewById(R.id.hostname_values_list_view)
        val ipAddressTextView: TextView = view.findViewById(R.id.address_value_text_view)
        val servicesRecyclerView: RecyclerView = view.findViewById(R.id.open_services_list_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.parsed_host_view, parent, false)
        return HostViewHolder(view)
    }

    override fun onBindViewHolder(holder: HostViewHolder, position: Int) {
        val host = hostList[position]
        holder.ipAddressTextView.text = host.ipAddress
        holder.hostnamesRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.hostnamesRecyclerView.adapter = HostnameAdapter(host.hostnames.toList())
        holder.servicesRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.servicesRecyclerView.adapter = ServiceAdapter(host.services)
        if (position % 2 == 1) {
            holder.containerConstraintLayout.setBackgroundColor(Color.LTGRAY)
        }
    }

    override fun getItemCount(): Int = hostList.size
}