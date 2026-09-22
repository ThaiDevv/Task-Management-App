package com.team.taskmanagementapp.ui.pomodoro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.repository.PomodoroSettingsRepository
import com.team.taskmanagementapp.databinding.FragmentPomodoroSettingsBinding
import com.team.taskmanagementapp.pomodoro.PomodoroConfig
import kotlin.math.roundToInt

/**
 * Pomodoro Settings (Task 11).
 *
 * - Thời lượng chọn bằng `Slider` với `stepSize` khai báo trong XML, nên UI chỉ cho phép
 *   giá trị trong khoảng hợp lệ: Focus **15–60**, Nghỉ ngắn **3–10**, Nghỉ dài **10–30**.
 * - Auto-start dùng 2 `SwitchCompat`: auto-start nghỉ và auto-start tập trung.
 * - Lưu ngay khi người dùng thay đổi, xuống `SharedPreferences` qua
 *   [PomodoroSettingsRepository] (cùng cơ chế với `SettingsFragment` hiện có của app).
 * - Giá trị luôn được nạp lại từ nơi lưu trữ, không hard-code lại sau khi người dùng đã lưu.
 *
 * Màn hình này **không** chạy timer và không can thiệp vào `PomodoroService`; cấu hình mới
 * được engine áp dụng cho phiên **bắt đầu sau đó** (`PomodoroTimerEngine.updateConfig`).
 */
class PomodoroSettingsFragment : Fragment() {

    private var _binding: FragmentPomodoroSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var settingsRepository: PomodoroSettingsRepository

    /** Chặn ghi lại prefs khi đang đẩy giá trị vào UI (setValue có thể phát listener). */
    private var isBinding = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPomodoroSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsRepository = PomodoroSettingsRepository.from(requireContext())

        binding.backButton.setOnClickListener { findNavController().navigateUp() }

        setupListeners()
        bindStoredSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        // Lưu ngay khi người dùng thay đổi; SharedPreferences.apply() ghi bất đồng bộ nên
        // không chặn UI dù slider phát nhiều sự kiện khi kéo.
        binding.sliderFocus.addOnChangeListener { _, _, _ -> onSettingsChanged() }
        binding.sliderShortBreak.addOnChangeListener { _, _, _ -> onSettingsChanged() }
        binding.sliderLongBreak.addOnChangeListener { _, _, _ -> onSettingsChanged() }
        binding.swAutoStartBreaks.setOnCheckedChangeListener { _, _ -> onSettingsChanged() }
        binding.swAutoStartFocus.setOnCheckedChangeListener { _, _ -> onSettingsChanged() }
    }

    private fun onSettingsChanged() {
        renderValues()
        if (isBinding) return
        settingsRepository.save(readConfigFromUi())
    }

    /** Nạp cấu hình đã lưu — không dùng giá trị hard-code nào. */
    private fun bindStoredSettings() {
        val config = settingsRepository.load()

        isBinding = true
        binding.sliderFocus.value = snapToSliderGrid(binding.sliderFocus, config.focusMinutes)
        binding.sliderShortBreak.value =
            snapToSliderGrid(binding.sliderShortBreak, config.shortBreakMinutes)
        binding.sliderLongBreak.value =
            snapToSliderGrid(binding.sliderLongBreak, config.longBreakMinutes)
        binding.swAutoStartBreaks.isChecked = config.autoStartBreaks
        binding.swAutoStartFocus.isChecked = config.autoStartFocus
        isBinding = false

        renderValues()
    }

    private fun renderValues() {
        binding.tvFocusValue.text = getString(
            R.string.pomodoro_settings_minutes_format,
            binding.sliderFocus.value.roundToInt()
        )
        binding.tvShortBreakValue.text = getString(
            R.string.pomodoro_settings_minutes_format,
            binding.sliderShortBreak.value.roundToInt()
        )
        binding.tvLongBreakValue.text = getString(
            R.string.pomodoro_settings_minutes_format,
            binding.sliderLongBreak.value.roundToInt()
        )
    }

    private fun readConfigFromUi() = PomodoroConfig(
        focusMinutes = binding.sliderFocus.value.roundToInt(),
        shortBreakMinutes = binding.sliderShortBreak.value.roundToInt(),
        longBreakMinutes = binding.sliderLongBreak.value.roundToInt(),
        autoStartBreaks = binding.swAutoStartBreaks.isChecked,
        autoStartFocus = binding.swAutoStartFocus.isChecked
    )

    /**
     * Đưa giá trị đã lưu về đúng mốc của slider.
     *
     * `Slider` yêu cầu giá trị phải nằm trên lưới `valueFrom + n * stepSize`, nếu không sẽ
     * ném `IllegalStateException`. Dữ liệu cũ trong máy (ví dụ focus = 17 phút) không nằm
     * trên lưới bước 5, nên phải làm tròn về mốc gần nhất trước khi gán.
     */
    private fun snapToSliderGrid(slider: Slider, minutes: Int): Float {
        val from = slider.valueFrom
        val step = slider.stepSize
        val steps = ((minutes - from) / step).roundToInt()
        return (from + steps * step).coerceIn(from, slider.valueTo)
    }
}
