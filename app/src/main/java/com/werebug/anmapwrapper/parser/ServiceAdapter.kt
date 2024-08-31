package com.werebug.anmapwrapper.parser

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.werebug.anmapwrapper.R

class ServiceAdapter(private val serviceList: List<Service>) :
    RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val portIdTextView: TextView = view.findViewById(R.id.port_id_text_view)
        val serviceNameTextView: TextView = view.findViewById(R.id.service_name_text_view)
        val productVersionTextView: TextView =
            view.findViewById(R.id.service_product_version_text_view)
        val portStateTextView: TextView = view.findViewById(R.id.port_state_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.parsed_service_view, parent, false)
        return ServiceViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = serviceList[position]
        holder.portIdTextView.text = service.port.toString()
        holder.portStateTextView.text = service.state
        holder.serviceNameTextView.text = service.name
        holder.productVersionTextView.text = "${service.product} ${service.version}"
    }

    override fun getItemCount(): Int = serviceList.size
}