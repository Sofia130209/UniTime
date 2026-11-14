package com.example.unitime

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unitime.R
import com.example.unitime.db.events.Event
import com.example.unitime.db.events.EventsDbProvider
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.unitime.db.events.EventRepository

class AddEventActivity : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var dateInput: EditText
    private lateinit var timeInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var maxParticipantsInput: EditText
    private lateinit var createButton: Button

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var isSaving = false

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_event_activity)

        titleInput = findViewById(R.id.eventTitleInput)
        descriptionInput = findViewById(R.id.eventDescriptionInput)
        categorySpinner = findViewById(R.id.eventCategorySpinner)
        dateInput = findViewById(R.id.eventDateInput)
        timeInput = findViewById(R.id.eventTimeInput)
        locationInput = findViewById(R.id.eventLocationInput)
        maxParticipantsInput = findViewById(R.id.eventMaxParticipantsInput)
        createButton = findViewById(R.id.createEventButton)

        setupSpinner()
        setupDatePicker()
        setupTimePicker()

        createButton.setOnClickListener { onCreateClicked() }
    }

    private fun setupSpinner() {
        val categories = listOf(
            "Спорт", "Музыка", "Учёба", "Еда",
            "Игры", "Искусство", "Волонтёрство", "Другое"
        )
        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun setupDatePicker() {
        dateInput.setOnClickListener {
            val initial = selectedDate ?: LocalDate.now()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day)
                    dateInput.setText(selectedDate!!.format(dateFormatter))
                    dateInput.error = null
                },
                initial.year, initial.monthValue - 1, initial.dayOfMonth
            ).apply {
                datePicker.minDate = System.currentTimeMillis() - 60_000
            }.show()
        }
    }

    private fun setupTimePicker() {
        timeInput.setOnClickListener {
            val initial = selectedTime ?: LocalTime.now()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedTime = LocalTime.of(hour, minute)
                    timeInput.setText(selectedTime!!.format(timeFormatter))
                    timeInput.error = null
                },
                initial.hour, initial.minute, true
            ).show()
        }
    }

    private fun onCreateClicked() {
        if (isSaving) return

        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val category = categorySpinner.selectedItem?.toString().orEmpty()
        val location = locationInput.text.toString().trim()
        val participantsRaw = maxParticipantsInput.text.toString().trim()

        if (!validate(title, location, participantsRaw)) return

        val maxParticipants = participantsRaw.toInt()
        val dateTime = LocalDateTime.of(selectedDate!!, selectedTime!!)
        val millis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        isSaving = true
        createButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val db = EventsDbProvider.get(applicationContext)

                val repository = EventRepository(
                    db.getEventsDao(),
                    db.getTagDao()
                )

                val event = Event(
                    activityTitle = title,
                    activityDescription = description,
                    activityCategory = category,
                    activityDate = millis,
                    activityLocation = location,
                    activityParticipants = maxParticipants
                )

                // Создаём событие вместе с его тегом
                repository.createEvent(
                    event = event,
                    tagNames = listOf(category)
                )

                Toast.makeText(
                    this@AddEventActivity,
                    "Активность создана",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(
                    this@AddEventActivity,
                    MainActivity::class.java
                )
                startActivity(intent)
            } catch (e: Exception) {
                isSaving = false
                createButton.isEnabled = true
                Toast.makeText(
                    this@AddEventActivity,
                    "Не удалось создать: ${e.message ?: "ошибка"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun validate(title: String, location: String, participantsRaw: String): Boolean {
        if (title.isBlank()) { titleInput.error = "Введите название"; return false }
        if (location.isBlank()) { locationInput.error = "Введите место"; return false }
        if (selectedDate == null) { dateInput.error = "Выберите дату"; return false }
        if (selectedTime == null) { timeInput.error = "Выберите время"; return false }

        val participants = participantsRaw.toIntOrNull()
        if (participants == null || participants <= 0) {
            maxParticipantsInput.error = "Введите число больше 0"
            return false
        }

        val dt = LocalDateTime.of(selectedDate, selectedTime)
        if (dt.isBefore(LocalDateTime.now())) {
            dateInput.error = "Дата должна быть в будущем"
            return false
        }
        return true
    }
}