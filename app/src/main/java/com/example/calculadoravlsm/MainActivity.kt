package com.example.calculadoravlsm

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import java.net.InetAddress
import kotlin.math.ceil
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    private lateinit var ipInput: EditText
    private lateinit var maskInput: EditText
    private lateinit var subnetCountInput: EditText
    private lateinit var resultOutput: TextView
    private lateinit var calculateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ipInput = findViewById(R.id.ipInput)
        maskInput = findViewById(R.id.maskInput)
        subnetCountInput = findViewById(R.id.subnetCountInput)
        resultOutput = findViewById(R.id.resultOutput)
        calculateButton = findViewById(R.id.calculateButton)

        calculateButton.setOnClickListener {
            calcularSubredes()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun calcularSubredes() {
        val ipStr = ipInput.text.toString()
        val mask = maskInput.text.toString().toIntOrNull()
        val subnets = subnetCountInput.text.toString().toIntOrNull()

        if (mask == null || subnets == null || !ipValida(ipStr)) {
            resultOutput.setText("Por favor, ingresa datos válidos.")
            return
        }

        val bitsDisponibles = 32 - mask
        val maxSubredes = 1 shl bitsDisponibles

        if (subnets > maxSubredes) {
            resultOutput.setText("No hay suficientes bits para dividir en $subnets subredes desde /$mask.")
            return
        }

        val bitsNecesarios = ceil(log(subnets.toDouble(), 2.0)).toInt()
        val nuevaMascara = mask + bitsNecesarios
        val hostsPorSubred = (1 shl (32 - nuevaMascara)) - 2
        val baseIp = ipToInt(ipStr)

        val resultado = StringBuilder()
        for (i in 0 until subnets) {
            val subred = baseIp + (i * (1 shl (32 - nuevaMascara)))
            resultado.append("Subred ${i + 1}:\n")
            resultado.append("  Dirección: ${intToIp(subred)}\n")
            resultado.append("  Submáscara: /$nuevaMascara\n")
            resultado.append("  Hosts posibles: $hostsPorSubred\n\n")
        }

        resultOutput.setText(resultado.toString())
    }

    private fun ipValida(ip: String): Boolean {
        return try {
            InetAddress.getByName(ip)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun ipToInt(ip: String): Int {
        return ip.split(".").map { it.toInt() }
            .reduce { acc, byte -> (acc shl 8) or byte }
    }

    private fun intToIp(ip: Int): String {
        return listOf(
            (ip shr 24) and 0xFF,
            (ip shr 16) and 0xFF,
            (ip shr 8) and 0xFF,
            ip and 0xFF
        ).joinToString(".")
    }
}
