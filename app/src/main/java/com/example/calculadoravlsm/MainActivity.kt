package com.example.calculadoravlsm

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.net.InetAddress
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var ipInput: EditText
    private lateinit var maskInput: EditText
    private lateinit var numSubnetsInput: EditText
    private lateinit var generateFieldsButton: Button
    private lateinit var calculateButton: Button
    private lateinit var resultOutput: TextView
    private lateinit var hostsContainer: LinearLayout

    private lateinit var ipInputLayout: LinearLayout
    private lateinit var maskInputLayout: LinearLayout
    private lateinit var numSubnetsInputLayout: LinearLayout
    private lateinit var resultsScroll: ScrollView

    private lateinit var backToStepOneButton: Button
    private lateinit var newCalculationButton: Button


    private lateinit var labelIp : TextView
    private lateinit var labelMask : TextView
    private lateinit var labelNumSubnets : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inputs
        ipInput = findViewById(R.id.ipInput)
        maskInput = findViewById(R.id.maskInput)
        numSubnetsInput = findViewById(R.id.numSubnetsInput)
        generateFieldsButton = findViewById(R.id.generateFieldsButton)
        calculateButton = findViewById(R.id.calculateButton)
        resultOutput = findViewById(R.id.resultOutput)
        hostsContainer = findViewById(R.id.hostsContainer)

        labelIp = findViewById(R.id.labelIp)
        labelMask = findViewById(R.id.labelMask)
        labelNumSubnets = findViewById(R.id.labelNumSubnets)

        // Layouts
        ipInputLayout = findViewById(R.id.ipInputLayout)
        maskInputLayout = findViewById(R.id.maskInputLayout)
        numSubnetsInputLayout = findViewById(R.id.numSubnetsInputLayout)
        resultsScroll = findViewById(R.id.resultsScroll)

        // Estado inicial
        hostsContainer.visibility = LinearLayout.GONE
        calculateButton.visibility = Button.GONE
        resultsScroll.visibility = ScrollView.GONE

        // Steps
        backToStepOneButton = findViewById(R.id.backToStepOneButton)
        newCalculationButton = findViewById(R.id.newCalculationButton)


        generateFieldsButton.setOnClickListener {
            generarCamposHosts()
        }

        calculateButton.setOnClickListener {
            calcularSubredes()
        }

        backToStepOneButton.setOnClickListener {
            volverPasoUno()
        }

        newCalculationButton.setOnClickListener {
            reiniciarTodo()
        }

    }

    private fun generarCamposHosts() {
        hostsContainer.removeAllViews()
        val cantidad = numSubnetsInput.text.toString().toIntOrNull()
        if (cantidad == null || cantidad <= 0) return

        for (i in 1..cantidad) {
            val label = TextView(this)
            label.text = "Hosts para Subred $i:"
            label.setPadding(0, 8, 0, 0)

            val input = EditText(this)
            input.hint = "Ej: 60"
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            input.id = 1000 + i

            hostsContainer.addView(label)
            hostsContainer.addView(input)
        }

        // PASO 2: Ocultar inputs iniciales y mostrar campos de host
        ipInputLayout.visibility = LinearLayout.GONE
        maskInputLayout.visibility = LinearLayout.GONE
        numSubnetsInputLayout.visibility = LinearLayout.GONE
        generateFieldsButton.visibility = Button.GONE

        hostsContainer.visibility = LinearLayout.VISIBLE
        calculateButton.visibility = Button.VISIBLE

        backToStepOneButton.visibility = Button.VISIBLE

        labelNumSubnets.visibility = TextView.GONE
        labelMask.visibility = TextView.GONE
        labelIp.visibility = TextView.GONE


    }

    private fun calcularSubredes() {
        val ipStr = ipInput.text.toString()
        val mask = maskInput.text.toString().toIntOrNull()
        val numSubnets = numSubnetsInput.text.toString().toIntOrNull()

        if (mask == null || numSubnets == null || !ipValida(ipStr)) {
            Toast.makeText(this, "Por favor, ingresa datos válidos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (mask < 8 || mask > 32) {
            Toast.makeText(this, "La máscara debe estar entre /8 y /32.", Toast.LENGTH_SHORT).show()

            return
        }

        val hostsList = mutableListOf<Pair<Int, Int>>()
        for (i in 1..numSubnets) {
            val field = hostsContainer.findViewById<EditText>(1000 + i)
            val hosts = field.text.toString().toIntOrNull()
            if (hosts == null || hosts <= 0) {
                Toast.makeText(this, "Por favor, ingresa un número válido de hosts para la subred $i.", Toast.LENGTH_SHORT).show()
                return
            }

            val bitsHost = ceil(log2(hosts + 2.0)).toInt()
            if (32 - bitsHost < mask) {
                Toast.makeText(this, "La máscara /$mask no es suficiente para $hosts hosts en la subred $i.", Toast.LENGTH_SHORT).show()
                return
            }

            hostsList.add(Pair(i, hosts))
        }

        val maxSubnets = 2.0.pow(32 - mask).toInt() - 2
        if (numSubnets > maxSubnets) {
            Toast.makeText(this, "La máscara /$mask solo permite un máximo de $maxSubnets subredes.", Toast.LENGTH_SHORT).show()

            return
        }

        if (hostsList.any { it.second <= 0 }) {
            Toast.makeText(this, "El número de hosts no puede ser negativo o cero.", Toast.LENGTH_SHORT).show()

            return
        }

        if (!ipValida(ipStr)) {
            Toast.makeText(this, "La IP ingresada no es válida.", Toast.LENGTH_SHORT).show()

            return
        }

        val ipParts = ipStr.split(".")
        if (ipParts.size != 4 || ipParts.any { it.toIntOrNull() !in 0..255 }) {
            Toast.makeText(this, "La IP ingresada no es válida.", Toast.LENGTH_SHORT).show()

            return
        }

        if (mask == 33) {
            Toast.makeText(this, "La máscara /33 no es válida.", Toast.LENGTH_SHORT).show()
            return
        }

        val sortedHostsList = hostsList.sortedByDescending { it.second }
        var currentIp = ipToInt(ipStr)
        val resultado = StringBuilder()

        for ((i, h) in sortedHostsList) {
            val bitsHost = ceil(log2(h + 2.0)).toInt()
            val nuevaMascara = 32 - bitsHost
            val hostsDisponibles = 2.0.pow(bitsHost).toInt() - 2

            val red = currentIp
            val broadcast = red + (1 shl bitsHost) - 1
            val primerHost = red + 1
            val ultimoHost = broadcast - 1

            resultado.append("Subred $i:\n")
            resultado.append("  Hosts requeridos: $h\n")
            resultado.append("  Hosts disponibles: $hostsDisponibles\n")
            resultado.append("  IP Red: ${intToIp(red)}\n")
            resultado.append("  Máscara: /$nuevaMascara\n")
            resultado.append("  Primer IP: ${intToIp(primerHost)}\n")
            resultado.append("  Última IP: ${intToIp(ultimoHost)}\n")
            resultado.append("  Broadcast: ${intToIp(broadcast)}\n\n")

            currentIp = broadcast + 1
        }

        hostsContainer.visibility = LinearLayout.GONE
        calculateButton.visibility = Button.GONE
        resultsScroll.visibility = ScrollView.VISIBLE
        newCalculationButton.visibility = Button.VISIBLE
        resultOutput.text = resultado.toString()
        backToStepOneButton.visibility = Button.GONE

        labelNumSubnets.visibility = TextView.GONE
        labelMask.visibility = TextView.GONE
        labelIp.visibility = TextView.GONE
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
        return ip.split(".")
            .map { it.toInt() }
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

    private fun volverPasoUno() {
        // Volver a mostrar inputs iniciales
        ipInputLayout.visibility = LinearLayout.VISIBLE
        maskInputLayout.visibility = LinearLayout.VISIBLE
        numSubnetsInputLayout.visibility = LinearLayout.VISIBLE
        generateFieldsButton.visibility = Button.VISIBLE

        // Ocultar campos de hosts
        hostsContainer.removeAllViews()
        hostsContainer.visibility = LinearLayout.GONE
        calculateButton.visibility = Button.GONE
        backToStepOneButton.visibility = Button.GONE
        newCalculationButton.visibility = Button.GONE

        labelNumSubnets.visibility = TextView.GONE
        labelMask.visibility = TextView.GONE
        labelIp.visibility = TextView.GONE

    }

    private fun reiniciarTodo() {
        // Limpiar todos los campos
        ipInput.text?.clear()
        maskInput.text?.clear()
        numSubnetsInput.text?.clear()
        resultOutput.text = ""
        hostsContainer.removeAllViews()

        // Mostrar solo el paso 1
        ipInputLayout.visibility = LinearLayout.VISIBLE
        maskInputLayout.visibility = LinearLayout.VISIBLE
        numSubnetsInputLayout.visibility = LinearLayout.VISIBLE
        generateFieldsButton.visibility = Button.VISIBLE

        labelNumSubnets.visibility = TextView.VISIBLE
        labelMask.visibility = TextView.VISIBLE
        labelIp.visibility = TextView.VISIBLE

        // Ocultar todo lo demas
        hostsContainer.visibility = LinearLayout.GONE
        calculateButton.visibility = Button.GONE
        resultsScroll.visibility = ScrollView.GONE
        newCalculationButton.visibility = Button.GONE
        backToStepOneButton.visibility = Button.GONE
    }

}
