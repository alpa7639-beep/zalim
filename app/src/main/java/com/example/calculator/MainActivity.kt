package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.EmptyStackException
import java.util.Stack

class MainActivity : AppCompatActivity() {

    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private var expression: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)

        val numberIds = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9", R.id.btnDot to "."
        )
        numberIds.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener { append(value) }
        }

        val operatorIds = mapOf(
            R.id.btnPlus to "+", R.id.btnMinus to "−",
            R.id.btnMultiply to "×", R.id.btnDivide to "÷",
            R.id.btnParenOpen to "(", R.id.btnParenClose to ")"
        )
        operatorIds.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener { append(value) }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            expression = ""
            updateDisplay()
        }

        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (expression.isNotEmpty()) {
                expression = expression.substring(0, expression.length - 1)
                updateDisplay()
            }
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            calculateResult()
        }

        updateDisplay()
    }

    private fun append(value: String) {
        expression += value
        updateDisplay()
    }

    private fun updateDisplay() {
        tvExpression.text = expression
        if (expression.isEmpty()) {
            tvResult.text = "0"
            return
        }
        val value = evaluateSafely(expression)
        tvResult.text = value ?: ""
    }

    private fun calculateResult() {
        val value = evaluateSafely(expression)
        if (value != null) {
            expression = formatNumber(value)
            tvExpression.text = ""
            tvResult.text = expression
        } else {
            tvResult.text = "Hata"
        }
    }

    private fun evaluateSafely(expr: String): String? {
        return try {
            val sanitized = expr.replace("×", "*").replace("÷", "/").replace("−", "-")
            val result = ExpressionEvaluator.evaluate(sanitized)
            if (result.isNaN() || result.isInfinite()) null else formatNumber(result)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            val rounded = Math.round(value * 1e10) / 1e10
            rounded.toString()
        }
    }
}

/**
 * Basit bir matematiksel ifade değerlendiricisi.
 * Toplama, çıkarma, çarpma, bölme ve parantezleri destekler.
 */
object ExpressionEvaluator {

    fun evaluate(expression: String): Double {
        val tokens = tokenize(expression)
        val rpn = toRPN(tokens)
        return evalRPN(rpn)
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens.add(expr.substring(start, i))
                }
                c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/", "(")) -> {
                    val start = i
                    i++
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens.add(expr.substring(start, i))
                }
                else -> {
                    tokens.add(c.toString())
                    i++
                }
            }
        }
        return tokens
    }

    private fun precedence(op: String): Int = when (op) {
        "+", "-" -> 1
        "*", "/" -> 2
        else -> 0
    }

    private fun toRPN(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val ops = Stack<String>()

        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> output.add(token)
                token == "(" -> ops.push(token)
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.peek() != "(") {
                        output.add(ops.pop())
                    }
                    if (ops.isNotEmpty()) ops.pop()
                }
                else -> {
                    while (ops.isNotEmpty() && ops.peek() != "(" &&
                        precedence(ops.peek()) >= precedence(token)
                    ) {
                        output.add(ops.pop())
                    }
                    ops.push(token)
                }
            }
        }
        while (ops.isNotEmpty()) {
            output.add(ops.pop())
        }
        return output
    }

    private fun evalRPN(tokens: List<String>): Double {
        val stack = Stack<Double>()
        for (token in tokens) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                stack.push(num)
            } else {
                val b = stack.pop()
                val a = try {
                    stack.pop()
                } catch (e: EmptyStackException) {
                    throw ArithmeticException("Geçersiz ifade")
                }
                val result = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> a / b
                    else -> throw ArithmeticException("Bilinmeyen işlem")
                }
                stack.push(result)
            }
        }
        return stack.pop()
    }
}
