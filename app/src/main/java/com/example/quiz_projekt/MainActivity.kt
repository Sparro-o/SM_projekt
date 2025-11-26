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
    private var rotationSensor: Sensor? = null
    private lateinit var gestureDetector: GestureDetector

    private var questions = mutableListOf<TriviaQuestion>()
    private var currentQuestionIndex = 0
    private var score = 0
    private var answeredQuestions = 0
    private var isLoadingQuestions = false

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

        // Setup gesture detector (shake + tilt + rotation)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        gestureDetector = GestureDetector(
            onShake = { onShakeDetected() },
            onTilt = { onTiltDetected() },
            onRotate = { onRotateDetected() }
        )

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
        if (isLoadingQuestions) return

        isLoadingQuestions = true
        tvQuestion.text = "Loading new questions..."

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getQuestions(amount = 10)
                if (response.response_code == 0) {
                    // Dodaj nowe pytania do listy
                    questions.addAll(response.results)
                    Toast.makeText(
                        this@MainActivity,
                        "✓ Loaded ${response.results.size} new questions",
                        Toast.LENGTH_SHORT
                    ).show()
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
            } finally {
                isLoadingQuestions = false
            }
        }
    }

    private fun displayQuestion() {
        if (questions.isEmpty()) {
            loadQuestions()
            return
        }

        // Jeśli doszliśmy do końca listy, pobierz nowe pytania
        if (currentQuestionIndex >= questions.size) {
            loadQuestions()
            return
        }

        // Zabezpieczenie przed ujemnymi indeksami
        if (currentQuestionIndex < 0) {
            currentQuestionIndex = 0
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

        // Update score and progress
        tvScore.text = "Score: $score/$answeredQuestions | Question: ${currentQuestionIndex + 1}/${questions.size}"
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
        tvQuestion.text = "Great job!"
        tvCategory.text = "You can continue or restart"
        tvScore.text = "Score: $score out of $answeredQuestions"

        btnAnswer1.text = "Continue Quiz"
        btnAnswer1.isEnabled = true
        btnAnswer1.setOnClickListener {
            setupAnswerButtons()
            displayQuestion() // Kontynuuj od aktualnego pytania
        }

        btnAnswer2.text = "Restart Quiz"
        btnAnswer2.isEnabled = true
        btnAnswer2.setOnClickListener {
            resetQuiz()
        }

        btnAnswer3.isEnabled = false
        btnAnswer4.isEnabled = false
    }

    private fun resetQuiz() {
        score = 0
        answeredQuestions = 0
        currentQuestionIndex = 0
        questions.clear() // Wyczyść starą listę pytań
        setupAnswerButtons()
        loadQuestions()
    }

    // GESTURE HANDLERS

    private fun onShakeDetected() {
        runOnUiThread {
            if (questions.isNotEmpty()) {
                Toast.makeText(this, "📱 Shake - Random question!", Toast.LENGTH_SHORT).show()
                currentQuestionIndex = (0 until questions.size).random()
                displayQuestion()
            }
        }
    }

    private fun onTiltDetected() {
        runOnUiThread {
            if (questions.isNotEmpty()) {
                Toast.makeText(this, "🔄 Tilt - Next question!", Toast.LENGTH_SHORT).show()
                currentQuestionIndex++
                displayQuestion() // Automatycznie załaduje nowe pytania gdy potrzeba
            }
        }
    }

    private fun onRotateDetected() {
        runOnUiThread {
            if (questions.isNotEmpty()) {
                Toast.makeText(this, "↻ Rotate - Next question!", Toast.LENGTH_SHORT).show()
                currentQuestionIndex++
                displayQuestion() // Automatycznie załaduje nowe pytania gdy potrzeba
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { acc ->
            sensorManager.registerListener(
                gestureDetector,
                acc,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        rotationSensor?.also { rot ->
            sensorManager.registerListener(
                gestureDetector,
                rot,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(gestureDetector)
    }
}