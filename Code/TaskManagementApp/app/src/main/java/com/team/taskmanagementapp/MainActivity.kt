package com.team.taskmanagementapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team.taskmanagementapp.databinding.ActivityMainBinding

/**
 * Main Activity serving as the primary entry point and container for the app's navigation tabs.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}