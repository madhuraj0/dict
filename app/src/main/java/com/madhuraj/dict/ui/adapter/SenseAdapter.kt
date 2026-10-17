package com.madhuraj.dict.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.madhuraj.dict.data.model.WordSense
import com.madhuraj.dict.databinding.ItemSenseBinding

class SenseAdapter(
    private val onSynonymClick: (String) -> Unit
) : ListAdapter<WordSense, SenseAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(private val binding: ItemSenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sense: WordSense, index: Int) {
            binding.textIndex.text = "$index."

            if (!sense.pos.isNullOrBlank()) {
                binding.badgePos.visibility = View.VISIBLE
                binding.badgePos.text = sense.pos
            } else {
                binding.badgePos.visibility = View.GONE
            }

            binding.textDefinition.text = sense.definition

            if (sense.examples.isNotEmpty()) {
                binding.textExamples.visibility = View.VISIBLE
                val formattedExamples = sense.examples.joinToString("\n\n") { "“$it”" }
                binding.textExamples.text = formattedExamples
            } else {
                binding.textExamples.visibility = View.GONE
            }

            binding.chipGroupSynonyms.removeAllViews()
            if (sense.synonyms.isNotEmpty()) {
                binding.layoutThesaurus.visibility = View.VISIBLE
                val context = binding.root.context
                for (syn in sense.synonyms) {
                    val chip = Chip(context).apply {
                        text = syn
                        isClickable = true
                        isCheckable = false
                        setOnClickListener { onSynonymClick(syn) }
                    }
                    binding.chipGroupSynonyms.addView(chip)
                }
            } else {
                binding.layoutThesaurus.visibility = View.GONE
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<WordSense>() {
        override fun areItemsTheSame(oldItem: WordSense, newItem: WordSense): Boolean {
            return oldItem.definition == newItem.definition && oldItem.pos == newItem.pos
        }

        override fun areContentsTheSame(oldItem: WordSense, newItem: WordSense): Boolean {
            return oldItem == newItem
        }
    }
}
