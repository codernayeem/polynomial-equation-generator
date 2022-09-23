package com.nw.polyeqgen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class DataAdapter (val data: MutableList<DataItem>) : RecyclerView.Adapter<DataAdapter.DataViewHolder>(){

    private lateinit var root: MainActivity
    class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        root = parent.context as MainActivity

        return DataViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.tv_x).text = data[position].x.toString()
        holder.itemView.findViewById<TextView>(R.id.tv_y).text = data[position].y.toString()

        holder.itemView.findViewById<ImageButton>(R.id.bt_modify).setOnClickListener{
            root.inputDialog(position)
        }

        holder.itemView.findViewById<ImageButton>(R.id.bt_delete).setOnClickListener{
            deleteData(position)
        }
    }

    override fun getItemCount(): Int = data.size

    fun addData(d: DataItem){
        data.add(d)
        notifyItemInserted(data.size-1)
    }

    fun modifyData(d: DataItem, pos: Int){
        data[pos].x = d.x
        data[pos].y = d.y
        notifyItemChanged(pos)
    }

    private fun deleteData(pos: Int){
        data.removeAt(pos)
        notifyDataSetChanged()
    }

    fun clearAllData(){
        data.clear()
        notifyDataSetChanged()
    }

}