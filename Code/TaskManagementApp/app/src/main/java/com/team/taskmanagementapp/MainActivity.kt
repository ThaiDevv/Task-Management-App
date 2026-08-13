package com.team.taskmanagementapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.team.taskmanagementapp.databinding.ActivityMainBinding
import com.team.taskmanagementapp.ui.activity.AddEditTaskActivity

/**
 * Main Activity serving as the primary entry point and container for the app's navigation tabs.
 * Uses Navigation Component for tab-based navigation with a custom bottom navigation bar.
 * Automatically preserves fragment state when switching tabs.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val tabIds = intArrayOf(
        R.id.taskListFragment,
        R.id.calendarFragment,
        R.id.statsFragment,
        R.id.settingsFragment
    )

    private val tabContainers = mutableListOf<View>()
    private val tabIcons = mutableListOf<ImageView>()
    private val tabLabels = mutableListOf<TextView>()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // NotificationHelper already checks the permission before posting a notification.
        // If denied, the user can enable it later from the Android app settings.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermissionIfNeeded()

        // Setup Navigation Component
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupCustomBottomNav()

        // FAB to Create Task
        binding.fabCreateTask.setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupCustomBottomNav() {
        // Register tab views
        tabContainers.add(binding.tabHome)
        tabContainers.add(binding.tabCalendar)
        tabContainers.add(binding.tabStats)
        tabContainers.add(binding.tabSettings)

        tabIcons.add(binding.iconHome)
        tabIcons.add(binding.iconCalendar)
        tabIcons.add(binding.iconStats)
        tabIcons.add(binding.iconSettings)

        tabLabels.add(binding.labelHome)
        tabLabels.add(binding.labelCalendar)
        tabLabels.add(binding.labelStats)
        tabLabels.add(binding.labelSettings)

        // Set click listeners
        tabContainers.forEachIndexed { index, view ->
            view.setOnClickListener {
                navController.navigate(tabIds[index])
            }
        }

        // Observe destination changes to update active tab
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val index = tabIds.indexOf(destination.id)
            if (index >= 0) {
                updateTabSelection(index)
            }
        }

        // Initialize with home tab selected
        updateTabSelection(0)
    }

    private fun updateTabSelection(selectedIndex: Int) {
        tabContainers.forEachIndexed { index, view ->
            val isSelected = index == selectedIndex
            tabIcons[index].isSelected = isSelected
            tabLabels[index].isSelected = isSelected
            view.isSelected = isSelected
        }
    }
}
