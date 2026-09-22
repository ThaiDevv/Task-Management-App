package com.team.taskmanagementapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.team.taskmanagementapp.databinding.ActivityMainBinding
import com.team.taskmanagementapp.ui.AiAssistantBottomSheet
import com.team.taskmanagementapp.ui.activity.AddEditTaskActivity
import com.team.taskmanagementapp.ui.base.BaseActivity
import com.team.taskmanagementapp.ui.pomodoro.PomodoroFragment
import com.team.taskmanagementapp.util.Constants

/**
 * Main Activity serving as the primary entry point and container for the app's navigation tabs.
 * Uses Navigation Component for tab-based navigation with a custom bottom navigation bar.
 * Automatically preserves fragment state when switching tabs.
 * Inherits PIN lock auto-lock functionality from BaseActivity.
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var aiGreetingDismissed = false
    private val aiGreetingHandler = Handler(Looper.getMainLooper())
    private var aiGreetingCycle: Runnable? = null

    private val tabIds = intArrayOf(
        R.id.taskListFragment,
        R.id.calendarFragment,
        R.id.statsFragment,
        R.id.streakFragment,
        R.id.rewardsFragment,
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

    private val importActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Import successful - navigate back to task list and refresh
            navController.navigate(R.id.taskListFragment)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        aiGreetingDismissed = getPreferences(MODE_PRIVATE)
            .getBoolean(PREF_AI_GREETING_DISMISSED, false)

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
        binding.fabAiAssistant.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(AiAssistantBottomSheet.TAG) == null) {
                AiAssistantBottomSheet().show(supportFragmentManager, AiAssistantBottomSheet.TAG)
            }
        }
        binding.aiGreetingBubble.setOnClickListener {
            binding.fabAiAssistant.performClick()
        }
        binding.aiGreetingClose.setOnClickListener {
            aiGreetingDismissed = true
            stopAiGreetingCycle()
            getPreferences(MODE_PRIVATE).edit()
                .putBoolean(PREF_AI_GREETING_DISMISSED, true)
                .apply()
            binding.aiGreetingBubble.isVisible = false
        }

        openPomodoroScreenIfRequested(intent)
    }

    /**
     * Mở Pomodoro Timer Screen khi:
     * - người dùng chạm notification đang chạy (`EXTRA_OPEN_POMODORO_TIMER` do `PomodoroService` gắn), hoặc
     * - người dùng bấm "Bắt đầu Pomodoro" ở Task Detail (Task 13, kèm `EXTRA_POMODORO_TASK_ID`).
     *
     * TaskId được truyền vào destination qua nav argument `PomodoroFragment.ARG_TASK_ID`;
     * ViewModel của màn hình Pomodoro sẽ quyết định có áp dụng hay không.
     */
    private fun openPomodoroScreenIfRequested(intent: Intent?) {
        if (intent?.getBooleanExtra(Constants.EXTRA_OPEN_POMODORO_TIMER, false) != true) return

        // Tránh đẩy trùng destination khi người dùng chạm notification lúc màn hình đang mở.
        if (navController.currentDestination?.id == R.id.pomodoroFragment) return

        val taskId = intent.getLongExtra(Constants.EXTRA_POMODORO_TASK_ID, Constants.NO_TASK_ID)
        navController.navigate(
            R.id.pomodoroFragment,
            bundleOf(PomodoroFragment.ARG_TASK_ID to taskId)
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Notification dùng FLAG_ACTIVITY_SINGLE_TOP: khi MainActivity đã mở sẵn thì phải
        // xử lý intent mới ở đây, nếu không chạm notification sẽ không mở được màn hình.
        setIntent(intent)
        openPomodoroScreenIfRequested(intent)
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
        // Register tab views (order matches layout: Home, Calendar, Stats, Streak, Rewards, Settings)
        tabContainers.add(binding.tabHome)
        tabContainers.add(binding.tabCalendar)
        tabContainers.add(binding.tabStats)
        tabContainers.add(binding.tabStreak)
        tabContainers.add(binding.tabRewards)
        tabContainers.add(binding.tabSettings)

        tabIcons.add(binding.iconHome)
        tabIcons.add(binding.iconCalendar)
        tabIcons.add(binding.iconStats)
        tabIcons.add(binding.iconStreak)
        tabIcons.add(binding.iconRewards)
        tabIcons.add(binding.iconSettings)

        tabLabels.add(binding.labelHome)
        tabLabels.add(binding.labelCalendar)
        tabLabels.add(binding.labelStats)
        tabLabels.add(binding.labelStreak)
        tabLabels.add(binding.labelRewards)
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
            updateNavigationChrome(index >= 0)
            if (index >= 0) {
                updateTabSelection(index)
            }
        }

        // Initialize with home tab selected
        updateTabSelection(0)
    }

    private fun updateNavigationChrome(isPrimaryDestination: Boolean) {
        binding.bottomBarContainer.isVisible = isPrimaryDestination
        binding.fabAiAssistant.isVisible = isPrimaryDestination
        if (isPrimaryDestination && !aiGreetingDismissed) {
            startAiGreetingCycle()
        } else {
            stopAiGreetingCycle()
            binding.aiGreetingBubble.isVisible = false
        }
        val layoutParams = binding.navHostFragment.layoutParams as FrameLayout.LayoutParams
        layoutParams.bottomMargin = if (isPrimaryDestination) {
            resources.getDimensionPixelSize(R.dimen.bottom_navigation_height)
        } else {
            0
        }
        binding.navHostFragment.layoutParams = layoutParams
    }

    private fun updateTabSelection(selectedIndex: Int) {
        tabContainers.forEachIndexed { index, view ->
            val isSelected = index == selectedIndex
            tabIcons[index].isSelected = isSelected
            tabLabels[index].isSelected = isSelected
            view.isSelected = isSelected
        }
    }

    private fun startAiGreetingCycle() {
        if (aiGreetingCycle != null) return
        binding.aiGreetingBubble.isVisible = true
        val cycle = object : Runnable {
            private var isVisiblePhase = true

            override fun run() {
                if (aiGreetingDismissed || isFinishing || isDestroyed) return
                isVisiblePhase = !isVisiblePhase
                binding.aiGreetingBubble.isVisible = isVisiblePhase
                aiGreetingHandler.postDelayed(this, if (isVisiblePhase) 5_000L else 15_000L)
            }
        }
        aiGreetingCycle = cycle
        aiGreetingHandler.postDelayed(cycle, 5_000L)
    }

    private fun stopAiGreetingCycle() {
        aiGreetingCycle?.let(aiGreetingHandler::removeCallbacks)
        aiGreetingCycle = null
    }

    override fun onDestroy() {
        stopAiGreetingCycle()
        super.onDestroy()
    }

    private companion object {
        const val PREF_AI_GREETING_DISMISSED = "ai_greeting_dismissed"
    }
}
