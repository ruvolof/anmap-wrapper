package com.werebug.anmapwrapper.parser

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
        val ipAddressTextView: TextView = view.findViewById(R.id.ip_address_value_text_view)
        val macAddressLabelTextView: TextView = view.findViewById(R.id.mac_address_label_text_view)
        val macAddressTextView: TextView = view.findViewById(R.id.mac_address_value_text_view)
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
        if (host.macAddress != null) {
            holder.macAddressLabelTextView.visibility = View.VISIBLE
            holder.macAddressTextView.visibility = View.VISIBLE
            holder.macAddressTextView.text = host.macAddress
        } else {
            holder.macAddressLabelTextView.visibility = View.GONE
            holder.macAddressTextView.visibility = View.GONE
        }
        holder.hostnamesRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.hostnamesRecyclerView.adapter = HostnameAdapter(host.hostnames.toList())
        holder.servicesRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.servicesRecyclerView.adapter = ServiceAdapter(host.services)
        val backgroundColorRes = if (position % 2 == 0) {
            R.color.row_background_even
        } else {
            R.color.row_background_odd
        }
        holder.containerConstraintLayout.setBackgroundResource(backgroundColorRes)
    }

    override fun getItemCount(): Int = hostList.size
}