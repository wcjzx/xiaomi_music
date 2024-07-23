package com.example.music_yaoyuhang
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.music_yaoyuhang.Dao.LyricLine

class LyricsAdapter(
    private var lyrics: List<LyricLine>,
    private val itemClickListener: () -> Unit
) : RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    private var currentLyricIndex: Int = -1

    class LyricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lyricTextView: TextView = itemView.findViewById(R.id.lyricTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ly_text_recyleview, parent, false)
        return LyricViewHolder(view)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        val lyricLine = lyrics[position]
        holder.lyricTextView.text = lyricLine.text
        holder.itemView.setOnClickListener {
            itemClickListener()
        }
        if (position == currentLyricIndex) {
            holder.lyricTextView.setTextColor(holder.itemView.context.getColor(android.R.color.white))
        } else {
            holder.lyricTextView.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        }
    }

    override fun getItemCount(): Int = lyrics.size

    fun updateCurrentLyricIndex(index: Int) {
        val previousIndex = currentLyricIndex
        currentLyricIndex = index
        notifyItemChanged(previousIndex)
        notifyItemChanged(currentLyricIndex)
    }

    fun updateLyrics(newLyrics: List<LyricLine>) {
        lyrics = newLyrics
        notifyDataSetChanged()
    }
}
