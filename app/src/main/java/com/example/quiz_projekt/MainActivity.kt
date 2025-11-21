package com.example.quiz_projekt

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.example.quiz_projekt.api.RetrofitClient
import com.example.quiz_projekt.api.TriviaQuestion
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvQuestion: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvScore: TextView
    private lateinit var btnAnswer1: Button
    private lateinit var btnAnswer2: Button
    private lateinit var btnAnswer3: Button
    private lateinit var btnAnswer4: Button
    private lateinit var animationView: LottieAnimationView

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector

    private var questions = listOf<TriviaQuestion>()
    private var currentQuestionIndex = 0
    private var score = 0
    private var answeredQuestions = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        tvQuestion = findViewById(R.id.tvQuestion)
        tvCategory = findViewById(R.id.tvCategory)
        tvScore = findViewById(R.id.tvScore)
        btnAnswer1 = findViewById(R.id.btnAnswer1)
        btnAnswer2 = findViewById(R.id.btnAnswer2)
        btnAnswer3 = findViewById(R.id.btnAnswer3)
        btnAnswer4 = findViewById(R.id.btnAnswer4)
        animationView = findViewById(R.id.animationView)

        // Setup shake detector
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        shakeDetector = ShakeDetector {
            onShakeDetected()
        }

        // Setup answer buttons
        setupAnswerButtons()

        // Load questions
        loadQuestions()
    }

    private fun setupAnswerButtons() {
        val buttons = listOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)
        buttons.forEach { button ->
            button.setOnClickListener {
                checkAnswer(button.text.toString())
            }
        }
    }

    private fun loadQuestions() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getQuestions(amount = 10)
                if (response.response_code == 0) {
                    questions = response.results
                    displayQuestion()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to load questions",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun displayQuestion() {
        if (currentQuestionIndex >= questions.size) {
            showFinalScore()
            return
        }

        val question = questions[currentQuestionIndex]

        // Display category and question (decode HTML entities)
        tvCategory.text = Html.fromHtml(question.category, Html.FROM_HTML_MODE_LEGACY)
        tvQuestion.text = Html.fromHtml(question.question, Html.FROM_HTML_MODE_LEGACY)

        // Prepare answers and shuffle
        val answers = (question.incorrect_answers + question.correct_answer).shuffled()

        // Display answers on buttons
        btnAnswer1.text = Html.fromHtml(answers.getOrNull(0) ?: "", Html.FROM_HTML_MODE_LEGACY)
        btnAnswer2.text = Html.fromHtml(answers.getOrNull(1) ?: "", Html.FROM_HTML_MODE_LEGACY)
        btnAnswer3.text = Html.fromHtml(answers.getOrNull(2) ?: "", Html.FROM_HTML_MODE_LEGACY)
        btnAnswer4.text = Html.fromHtml(answers.getOrNull(3) ?: "", Html.FROM_HTML_MODE_LEGACY)

        // Enable all buttons
        enableButtons(true)

        // Update score
        tvScore.text = "Score: $score/$answeredQuestions"
    }

    private fun checkAnswer(selectedAnswer: String) {
        val question = questions[currentQuestionIndex]
        val correctAnswer = Html.fromHtml(question.correct_answer, Html.FROM_HTML_MODE_LEGACY).toString()

        answeredQuestions++

        if (selectedAnswer == correctAnswer) {
            score++
            Toast.makeText(this, "✓ Correct!", Toast.LENGTH_SHORT).show()
            animationView.playAnimation()
        } else {
            Toast.makeText(this, "✗ Wrong! Correct: $correctAnswer", Toast.LENGTH_LONG).show()
        }

        // Disable buttons temporarily
        enableButtons(false)

        // Move to next question after delay
        tvQuestion.postDelayed({
            currentQuestionIndex++
            displayQuestion()
        }, 2000)
    }

    private fun enableButtons(enabled: Boolean) {
        btnAnswer1.isEnabled = enabled
        btnAnswer2.isEnabled = enabled
        btnAnswer3.isEnabled = enabled
        btnAnswer4.isEnabled = enabled
    }

    private fun showFinalScore() {
        tvQuestion.text = "Quiz Completed!"
        tvCategory.text = "Final Score"
        tvScore.text = "$score out of $answeredQuestions"

        btnAnswer1.text = "Restart Quiz"
        btnAnswer1.isEnabled = true
        btnAnswer1.setOnClickListener {
            resetQuiz()
        }

        btnAnswer2.isEnabled = false
        btnAnswer3.isEnabled = false
        btnAnswer4.isEnabled = false
    }

    private fun resetQuiz() {
        score = 0
        answeredQuestions = 0
        currentQuestionIndex = 0
        setupAnswerButtons()
        loadQuestions()
    }

    private fun onShakeDetected() {
        // When phone is shaken, load new question
        runOnUiThread {
            if (questions.isNotEmpty() && currentQuestionIndex < questions.size) {
                Toast.makeText(this, "📱 Loading new question...", Toast.LENGTH_SHORT).show()
                currentQuestionIndex = (currentQuestionIndex + 1) % questions.size
                displayQuestion()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { acc ->
            sensorManager.registerListener(shakeDetector, acc, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }
}