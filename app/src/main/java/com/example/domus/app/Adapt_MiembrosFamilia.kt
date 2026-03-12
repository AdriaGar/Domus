package com.example.domus.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.domus.R
import com.example.domus.data.repository.MemberInfo
import com.example.domus.databinding.ItemFamilyMemberBinding

class Adapt_MiembrosFamilia(
    private var members: List<MemberInfo>,
    private val adminId: String,
    private val creatorId: String,
    private val isCurrentUserAdmin: Boolean = false,
    private val currentUserId: String = "",
    private val onAcceptClick: ((String) -> Unit)? = null,
    private val onTransferAdminClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<Adapt_MiembrosFamilia.ViewHolder>() {

    class ViewHolder(val binding: ItemFamilyMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFamilyMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = members[position]
        holder.binding.apply {
            tvMemberName.text = member.nombre
            tvMemberEmail.text = member.email
            
            // Forzamos el refresco de la foto usando lastUpdated como firma
            Glide.with(ivMemberAvatar.context)
                .load(member.photoUrl)
                .signature(ObjectKey(member.lastUpdated))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_user_default)
                .into(ivMemberAvatar)

            tvRoleBadge.isVisible = false
            tvRoleBadge.setOnClickListener(null)

            when {
                member.id == creatorId -> {
                    tvRoleBadge.isVisible = true
                    tvRoleBadge.text = "Fundador"
                }
                member.id == adminId -> {
                    tvRoleBadge.isVisible = true
                    tvRoleBadge.text = "Dueño"
                }
                onAcceptClick != null -> {
                    tvRoleBadge.isVisible = true
                    tvRoleBadge.text = "Aceptar"
                    tvRoleBadge.setOnClickListener { onAcceptClick.invoke(member.id) }
                }
                isCurrentUserAdmin && member.id != currentUserId -> {
                    tvRoleBadge.isVisible = true
                    tvRoleBadge.text = "Gestionar"
                    tvRoleBadge.setOnClickListener { view ->
                        val popup = PopupMenu(view.context, view)
                        popup.menu.add("Hacer administrador")
                        popup.setOnMenuItemClickListener {
                            onTransferAdminClick?.invoke(member.id)
                            true
                        }
                        popup.show()
                    }
                }
            }
        }
    }

    override fun getItemCount() = members.size

    fun updateData(newMembers: List<MemberInfo>) {
        members = newMembers
        notifyDataSetChanged()
    }
}
