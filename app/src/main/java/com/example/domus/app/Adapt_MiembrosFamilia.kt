package com.example.domus.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.domus.R
import com.example.domus.data.repository.MemberInfo
import com.example.domus.databinding.ItemFamilyMemberBinding

class Adapt_MiembrosFamilia(
    private var members: List<MemberInfo>,
    private val adminId: String,
    private val onAcceptClick: ((String) -> Unit)? = null
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
            
            Glide.with(ivMemberAvatar.context)
                .load(member.photoUrl)
                .placeholder(R.drawable.ic_user_default)
                .into(ivMemberAvatar)

            // Mostrar el badge si es el administrador (Fundador)
            if (member.id == adminId) {
                tvRoleBadge.isVisible = true
                tvRoleBadge.text = "Fundador"
            } else {
                tvRoleBadge.isVisible = onAcceptClick != null // Si hay callback de aceptar, es la lista de pendientes
                if (onAcceptClick != null) {
                    tvRoleBadge.text = "Aceptar"
                    tvRoleBadge.setOnClickListener { onAcceptClick.invoke(member.id) }
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