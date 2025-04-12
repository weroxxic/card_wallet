package com.example.myapplication

import SpaceItemDecoration
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var database: CardDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var cardAdapter: CardAdapter
    private lateinit var cardDao: CardDao
    private lateinit var doneButton: Button
    private lateinit var backgroundImageView: ImageView

    private var isPinVerified = false
    private var exitRequestedOnce = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences and check if PIN is verified
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        isPinVerified = sharedPreferences.getBoolean("PIN_VERIFIED", false)

        if (!isPinVerified) {
            launchPinActivity()
            return  // Exit early to prevent the rest of onCreate from executing
        }

        setupUI()
    }

    override fun onResume() {
        super.onResume()

        // Check again if the PIN is verified when the activity resumes
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        isPinVerified = sharedPreferences.getBoolean("PIN_VERIFIED", false)

        if (isPinVerified) {
            loadCards()
        } else {
            launchPinActivity()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed();
        if (exitRequestedOnce) {
            finishAffinity() // Close all activities and exit the app
        } else {
            this.exitRequestedOnce = true
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

            handler.postDelayed({ exitRequestedOnce = false }, 2000)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        finishAffinity() // Exit the app if it is minimized
    }

    private fun setupUI() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        doneButton = findViewById(R.id.doneButton)
        doneButton.visibility = View.GONE

        doneButton.setOnClickListener {
            val selectedCards = cardAdapter.getSelectedItems()
            if (selectedCards.isNotEmpty()) {
                deleteSelectedCards(selectedCards)
            }
            cardAdapter.toggleSelectionMode()
            doneButton.visibility = View.GONE
        }

        backgroundImageView = findViewById(R.id.backgroundImageView)

        database = CardDatabase.getDatabase(this)
        cardDao = database.cardDao()

        cardAdapter = CardAdapter(emptyList(), this)
        recyclerView.adapter = cardAdapter

        // Add space between items
        val spaceHeight = resources.getDimensionPixelSize(R.dimen.recycler_item_spacing)
        recyclerView.addItemDecoration(SpaceItemDecoration(spaceHeight))

        // Load cards after setting up the UI
        loadCards()
    }

    private fun loadCards() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cardEntities = cardDao.getAllCards()
            if (cardEntities.isNullOrEmpty()) {
                Log.e("MainActivity", "No cards found in the database.")
            } else {
                val cardItems = cardEntities.map { cardEntity ->
                    CardItem(
                        title = cardEntity.cardName,
                        frontImagePath = cardEntity.frontImagePath,
                        backImagePath = cardEntity.backImagePath
                    )
                }
                Log.d("MainActivity", "Loaded ${cardItems.size} cards.")

                withContext(Dispatchers.Main) {
                    cardAdapter.updateCardList(cardItems)
                    recyclerView.visibility = View.VISIBLE  // Make sure RecyclerView is visible
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_card -> {
                val intent = Intent(this, AddCardActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_delete_card -> {
                checkAndToggleSelectionMode()
                true
            }
            R.id.settings -> {
                val intent = Intent(this, ResetPINActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.exit -> {
                finishAffinity() // Close all activities and exit the app
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkAndToggleSelectionMode() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cardCount = cardDao.getAllCards().size
            withContext(Dispatchers.Main) {
                if (cardCount > 0) {
                    cardAdapter.toggleSelectionMode()
                    doneButton.visibility = if (doneButton.visibility == View.GONE) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(this@MainActivity, "No Cards Available to show. Please Add Card", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchPinActivity() {
        val intent = Intent(this, PinActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Ensure that MainActivity is finished so that it won't stack up.
    }

    private fun deleteSelectedCards(selectedCards: List<CardItem>) {
        lifecycleScope.launch(Dispatchers.IO) {
            selectedCards.forEach { cardItem ->
                val cardEntity = cardDao.getCardByName(cardItem.title)
                cardEntity?.let { cardDao.deleteCard(it) }
            }

            val updatedCardEntities = cardDao.getAllCards()
            val updatedCardItems = updatedCardEntities.map { cardEntity ->
                CardItem(
                    title = cardEntity.cardName,
                    frontImagePath = cardEntity.frontImagePath,
                    backImagePath = cardEntity.backImagePath
                )
            }

            withContext(Dispatchers.Main) {
                cardAdapter.clearSelection()
                cardAdapter.updateCardList(updatedCardItems)
            }
        }
    }

    private fun displayBlurredBackground() {
        val blurredBitmap = decodeSampledBitmapFromResource(resources, R.drawable.blurred, 1024, 1024)
        backgroundImageView.setImageBitmap(blurredBitmap)
    }

    private fun decodeSampledBitmapFromResource(
        res: Resources,
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(res, resId, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeResource(res, resId, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    companion object {
        const val REQUEST_CODE_PIN = 1001
    }
}
