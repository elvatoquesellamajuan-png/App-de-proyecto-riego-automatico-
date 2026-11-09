package com.example.myapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID

class ControlActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private val PORT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var comunicacionEnCurso = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        val btnConectar = findViewById<Button>(R.id.btnConectar)
        val btnAbrir = findViewById<Button>(R.id.btnAbrir)

        btnConectar.setOnClickListener {
            conectarAlModuloBluetooth()
        }

        btnAbrir.setOnClickListener {
            consultarEstado()
        }
    }

    private fun conectarAlModuloBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Por favor enciende el Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permisosFaltantes = mutableListOf<String>()
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permisosFaltantes.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permisosFaltantes.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (permisosFaltantes.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permisosFaltantes.toTypedArray(), 1)
                Toast.makeText(this, "Concede los permisos de Bluetooth y vuelve a intentar", Toast.LENGTH_LONG).show()
                return
            }
        }

        try {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            if (pairedDevices.isNullOrEmpty()) {
                Toast.makeText(this, "No hay dispositivos vinculados.", Toast.LENGTH_LONG).show()
                return
            }

            var dispositivoBluetooth: BluetoothDevice? = null
            for (device in pairedDevices) {
                val nombre = device.name ?: ""
                if (nombre.contains("HC-05", ignoreCase = true) || nombre.contains("HC-06", ignoreCase = true)) {
                    dispositivoBluetooth = device
                    break
                }
            }

            if (dispositivoBluetooth == null) {
                Toast.makeText(this, "No se encontró el HC-05 o HC-06 entre los vinculados", Toast.LENGTH_LONG).show()
                return
            }

            val dispositivoFinal = dispositivoBluetooth

            Thread {
                try {
                    bluetoothSocket = dispositivoFinal.createInsecureRfcommSocketToServiceRecord(PORT_UUID)
                    bluetoothAdapter.cancelDiscovery()
                    bluetoothSocket?.connect()

                    runOnUiThread {
                        Toast.makeText(this, "¡Conectado exitosamente al módulo!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this, "Error de conexión: Verifica que el Arduino esté encendido.", Toast.LENGTH_LONG).show()
                    }
                    try {
                        bluetoothSocket?.close()
                        bluetoothSocket = null
                    } catch (closeException: IOException) {
                    }
                }
            }.start()

        } catch (e: SecurityException) {
            Toast.makeText(this, "Faltan permisos de Bluetooth en el sistema", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun consultarEstado() {
        if (comunicacionEnCurso) return

        val socket = bluetoothSocket
        if (socket == null || !socket.isConnected) {
            Toast.makeText(this, "Primero debes conectar el módulo Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        comunicacionEnCurso = true
        Thread {
            try {
                socket.outputStream.write("estado\n".toByteArray(Charsets.UTF_8))
                val respuesta = leerLinea(socket)

                runOnUiThread {
                    when {
                        respuesta.contains("SECA", ignoreCase = true) -> {
                            preguntarSiRegar()
                        }
                        respuesta.contains("HUMEDA", ignoreCase = true) -> {
                            comunicacionEnCurso = false
                            Toast.makeText(this, respuesta, Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            comunicacionEnCurso = false
                            Toast.makeText(this, "Respuesta inesperada del Arduino: $respuesta", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    comunicacionEnCurso = false
                    Toast.makeText(this, "No se pudo consultar el estado del Arduino: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun preguntarSiRegar() {
        AlertDialog.Builder(this)
            .setTitle("Planta seca")
            .setMessage("La planta está seca. ¿Quieres iniciar el riego?")
            .setPositiveButton("Sí") { _, _ ->
                enviarComando("si", "Riego completado")
            }
            .setNegativeButton("No") { _, _ ->
                enviarComando("no", "Riego cancelado")
            }
            .setCancelable(false)
            .show()
    }

    private fun enviarComando(comando: String, mensajeExito: String) {
        val socket = bluetoothSocket
        if (socket == null || !socket.isConnected) {
            comunicacionEnCurso = false
            Toast.makeText(this, "Primero debes conectar el módulo Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                socket.outputStream.write("$comando\n".toByteArray(Charsets.UTF_8))
                val cantidadRespuestas = if (comando == "si") 2 else 1
                repeat(cantidadRespuestas) {
                    leerLinea(socket)
                }
                runOnUiThread {
                    comunicacionEnCurso = false
                    Toast.makeText(this, mensajeExito, Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    comunicacionEnCurso = false
                    Toast.makeText(this, "Error al enviar la respuesta al Arduino: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun leerLinea(socket: BluetoothSocket): String {
        val respuesta = StringBuilder()
        val input = socket.inputStream
        val limiteTiempo = SystemClock.elapsedRealtime() + 5000

        while (SystemClock.elapsedRealtime() < limiteTiempo) {
            while (input.available() > 0) {
                val byte = input.read()
                if (byte == -1) {
                    throw IOException("La conexión Bluetooth se cerró.")
                }
                if (byte == '\n'.code) {
                    return respuesta.toString().trim()
                }
                if (byte != '\r'.code) {
                    respuesta.append(byte.toChar())
                }
            }
            SystemClock.sleep(50)
        }

        throw IOException("El Arduino no respondió a tiempo.")
    }

    override fun onDestroy() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            // La actividad se cierra de todas formas.
        } finally {
            bluetoothSocket = null
        }
        super.onDestroy()
    }
}