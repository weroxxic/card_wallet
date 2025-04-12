package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter class for managing card items in a RecyclerView
class CardAdapter(
    private var cardItemList: List<CardItem>,
    private val context: Context
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    // Set to keep track of selected items for multi-select mode
    private val selectedItems = mutableSetOf<CardItem>()

    // Boolean to track whether we are in selection mode (for multi-select)
    private var isSelectionMode = false

    // Called when RecyclerView needs a new ViewHolder to represent an item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item, parent, false)
        return CardViewHolder(view)
    }

    // Called by RecyclerView to display data at the specified position
    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = cardItemList[position]
        holder.bind(item, selectedItems.contains(item), isSelectionMode)
    }

    // Returns the total number of items in the data set held by the adapter
    override fun getItemCount(): Int {
        return cardItemList.size
    }

    // Returns the list of selected items when in selection mode
    fun getSelectedItems(): List<CardItem> {
        return selectedItems.toList()
    }

    // Clears the current selection
    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged() // Notify the adapter that data has changed
    }

    // Toggles selection mode on or off
    fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        notifyDataSetChanged() // Refresh the view to show/hide checkboxes
    }

    // Inner class to represent a single item view in the RecyclerView
    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.title) // Card title
        private val image: ImageView = itemView.findViewById(R.id.image) // Card image
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox) // Selection checkbox
        private val menuButton: ImageView = itemView.findViewById(R.id.menuButton) // Three-dot menu button

        private var isFrontVisible = true // Boolean to track whether the front image is visible

        // Binds the data from a CardItem object to the views in the ViewHolder
        fun bind(cardItem: CardItem, isSelected: Boolean, isSelectionMode: Boolean) {
            // Set the title and the image based on the card item data
            title.text = cardItem.title
            image.setImageBitmap(loadBitmapFromFile(cardItem.frontImagePath))
            checkBox.isChecked = isSelected

            // Show or hide the checkbox based on selection mode
            checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE

            // Set up the click listener for the image view
            image.setOnClickListener {
                if (!isSelectionMode) {
                    // Animate the image with a fade-in effect
                    val fadeIn = android.view.animation.AlphaAnimation(0f, 1f).apply {
                        duration = 300 // Duration of the fade-in effect
                        fillAfter = true // Keep the final state after the animation
                    }

                    // Toggle between front and back image of the card
                    isFrontVisible = !isFrontVisible
                    image.setImageBitmap(
                        if (isFrontVisible) loadBitmapFromFile(cardItem.frontImagePath)
                        else loadBitmapFromFile(cardItem.backImagePath)
                    )

                    // Start the fade-in animation on the image
                    image.startAnimation(fadeIn)
                }
            }

            // Handle checkbox state changes for selection
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(cardItem) // Add item to selected list
                } else {
                    selectedItems.remove(cardItem) // Remove item from selected list
                }
            }

            // Set up the click listener for the menu button
            menuButton.setOnClickListener {
                showPopupMenu(it, cardItem)
            }
        }

        // Helper function to show a popup menu
        private fun showPopupMenu(view: View, cardItem: CardItem) {
            val popup = PopupMenu(context, view)
            val inflater: MenuInflater = popup.menuInflater
            inflater.inflate(R.menu.card_options_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_view -> {
                        // Handle the "View" action
                        val intent = Intent(context, CardDetailsActivity::class.java)
                        intent.putExtra("CARD_NAME", cardItem.title)
                        intent.putExtra("FRONT_IMAGE_PATH", cardItem.frontImagePath)
                        intent.putExtra("BACK_IMAGE_PATH", cardItem.backImagePath)
                        context.startActivity(intent)
                        true
                    }
                    R.id.action_modify -> {
                        // Handle the "Modify" action
                        // Implement your modification logic here
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Helper function to load a Bitmap image from a file path
        private fun loadBitmapFromFile(filePath: String): Bitmap? {
            return BitmapFactory.decodeFile(filePath)
        }
    }

    // Updates the list of cards and notifies the adapter to refresh the RecyclerView
    fun updateCardList(newCardItemList: List<CardItem>) {
        this.cardItemList = newCardItemList
        notifyDataSetChanged() // Notify the adapter that data has changed
    }
}
