package com.team.taskmanagementapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.databinding.FragmentCalendarBinding
import java.util.Calendar

/**
 * CalendarFragment displays a monthly calendar view with task indicators
 * and a schedule list for the selected date.
 */
class CalendarFragment : Fragment() {

    private lateinit var binding: FragmentCalendarBinding
    private lateinit var taskAdapter: TaskAdapter
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        // Setup RecyclerView
        taskAdapter = TaskAdapter()
        binding.scheduleRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }

        // Setup Calendar Grid
        buildCalendarGrid()
        updateMonthYear()
    }

    private fun updateMonthYear() {
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault())
        val year = calendar.get(Calendar.YEAR)
        binding.monthYearText.text = "$monthName $year"
    }

    private fun buildCalendarGrid() {
        val gridLayout = binding.calendarGrid
        gridLayout.removeAllViews()

        // Days of week header
        val daysOfWeek = arrayOf("S", "M", "T", "W", "T", "F", "S")
        for (day in daysOfWeek) {
            val dayLabel = TextView(requireContext()).apply {
                text = day
                textSize = 12f
                setTextColor(resources.getColor(R.color.outline, requireContext().theme))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            gridLayout.addView(dayLabel)
        }

        // Fill calendar days
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1

        // Empty cells before month starts
        repeat(firstDayOfWeek) {
            gridLayout.addView(TextView(requireContext()).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 50
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            })
        }

        // Days of month
        for (day in 1..maxDays) {
            val dayLabel = TextView(requireContext()).apply {
                text = day.toString()
                textSize = 14f
                setTextColor(resources.getColor(R.color.on_surface, requireContext().theme))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 50
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            gridLayout.addView(dayLabel)
        }
    }
}
