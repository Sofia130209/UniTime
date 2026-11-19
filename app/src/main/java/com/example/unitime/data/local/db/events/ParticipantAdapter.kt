package com.example.unitime.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.unitime.R
import com.example.unitime.data.local.model.User
import java.io.File

class ParticipantAdapter(
    private val participants: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<ParticipantAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivParticipantAvatar)
        val tvName: TextView = view.findViewById(R.id.tvParticipantName)
        val tvLogin: TextView = view.findViewById(R.id.tvParticipantLogin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_participant, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = participants[position]
        holder.tvName.text = "${user.name} ${user.surname}".trim()
        holder.tvLogin.text = "@${user.login}"

        val avatarFile = user.avatarPath?.let(::File)?.takeIf { it.exists() }
        if (avatarFile != null) {
            holder.ivAvatar.load(avatarFile) {
                crossfade(true)
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder)
                transformations(CircleCropTransformation())
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
        }

        holder.itemView.setOnClickListener {
            onItemClick(user)
        }
    }

    override fun getItemCount() = participants.size
}