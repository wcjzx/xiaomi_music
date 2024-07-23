package com.example.music_yaoyuhang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.music_yaoyuhang.Dao.MusicInfo
import com.google.gson.Gson
import android.content.SharedPreferences
import android.content.Context

class PlaylistAdapter(private val context: Context, private var playlist: MutableList<MusicInfo>) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {
    private val gson = Gson()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val musicInfo = playlist[position]
        holder.musicNameTextView.text = musicInfo.musicName
        holder.authorTextView.text = musicInfo.author
        holder.removeButton.setOnClickListener {
            removeMusicAt(position)
        }
    }

    override fun getItemCount(): Int {
        return playlist.size
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val musicNameTextView: TextView = itemView.findViewById(R.id.musicNameTextView)
        val authorTextView: TextView = itemView.findViewById(R.id.authorTextView)
        val removeButton: ImageButton = itemView.findViewById(R.id.removeButton)
    }
    private fun removeMusicAt(position: Int) {
        playlist.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, playlist.size)
        saveMusicList()
    }

    private fun saveMusicList() {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(playlist)
        editor.putString("music_list", json)
        editor.apply()
    }
}