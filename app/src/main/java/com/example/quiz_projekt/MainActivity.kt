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
    private lateinit var resultAnimationView: LottieAnimationView

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

        tvQuestion = findViewById(R.id.tvQuestion)
        tvCategory = findViewById(R.id.tvCategory)
        tvScore = findViewById(R.id.tvScore)
        btnAnswer1 = findViewById(R.id.btnAnswer1)
        btnAnswer2 = findViewById(R.id.btnAnswer2)
        btnAnswer3 = findViewById(R.id.btnAnswer3)
        btnAnswer4 = findViewById(R.id.btnAnswer4)
        animationView = findViewById(R.id.animationView)
        resultAnimationView = findViewById(R.id.resultAnimationView)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        gestureDetector = GestureDetector(
            onShake = { onShakeDetected() },
            onTilt = { onTiltDetected() },
            onRotate = { onRotateDetected() }
        )

        setupAnswerButtons()

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
                    questions.addAll(response.results)
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

        if (currentQuestionIndex >= questions.size) {
            loadQuestions()
            return
        }

        if (currentQuestionIndex < 0) {
            currentQuestionIndex = 0
        }

        val question = questions[currentQuestionIndex]

        tvCategory.text = Html.fromHtml(question.category, Html.FROM_HTML_MODE_LEGACY)
        tvQuestion.text = Html.fromHtml(question.question, Html.FROM_HTML_MODE_LEGACY)

        val answers = (question.incorrect_answers + question.correct_answer).shuffled()

        btnAnswer1.text = Html.fromHtml(answers.getOrNull(0) ?: "", Html.FROM_HTML_MODE_LEGACY)
        btnAnswer2.text = Html.fromHtml(answers.getOrNull(1) ?: "", Html.FROM_HTML_MODE_LEGACY)
        btnAnswer3.text = Html.fromHtml(answers.getOrNull(2) ?: "", Html.FROM_HTML_MODE_LEGACY)
        btnAnswer4.text = Html.fromHtml(answers.getOrNull(3) ?: "", Html.FROM_HTML_MODE_LEGACY)

        enableButtons(true)

        tvScore.text = "Score: $score/$answeredQuestions"
    }

    private fun checkAnswer(selectedAnswer: String) {
        val question = questions[currentQuestionIndex]
        val correctAnswer = Html.fromHtml(question.correct_answer, Html.FROM_HTML_MODE_LEGACY).toString()

        answeredQuestions++

        if (selectedAnswer == correctAnswer) {
            score++
            Toast.makeText(this, "Correct", Toast.LENGTH_SHORT).show()
            animationView.playAnimation()

            showResultAnimation(true)
        } else {
            Toast.makeText(this, "Wrong, Correct: $correctAnswer", Toast.LENGTH_LONG).show()

            showResultAnimation(false)
        }

        enableButtons(false)

        tvQuestion.postDelayed({
            currentQuestionIndex++
            displayQuestion()
            resultAnimationView.visibility = android.view.View.GONE
        }, 2000)
    }

    private fun showResultAnimation(isCorrect: Boolean) {
        resultAnimationView.visibility = android.view.View.VISIBLE

        if (isCorrect) {
            resultAnimationView.setAnimation(R.raw.correct_animation)
        } else {
            resultAnimationView.setAnimation(R.raw.fail_animation)
        }

        resultAnimationView.playAnimation()
    }

    private fun enableButtons(enabled: Boolean) {
        btnAnswer1.isEnabled = enabled
        btnAnswer2.isEnabled = enabled
        btnAnswer3.isEnabled = enabled
        btnAnswer4.isEnabled = enabled
    }

    private fun onShakeDetected() {
        runOnUiThread {
            if (questions.isNotEmpty()) {
                currentQuestionIndex = (0 until questions.size).random()
                displayQuestion()
            }
        }
    }

    private fun onTiltDetected() {
        runOnUiThread {
            if (questions.isNotEmpty()) {
                currentQuestionIndex++
                displayQuestion()
            }
        }
    }

    private fun onRotateDetected() {
        runOnUiThread {
            if (questions.isNotEmpty()) {
                currentQuestionIndex++
                displayQuestion()
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