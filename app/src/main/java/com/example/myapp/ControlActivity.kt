package com.example.myapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID

class ControlActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null

    private val PORT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        val btnConectar = findViewById<Button>(R.id.btnConectar)
        val btnAbrir = findViewById<Button>(R.id.btnAbrir)
        val btnCerrar = findViewById<Button>(R.id.btnCerrar)

        btnConectar.setOnClickListener {
            conectarAlModuloBluetooth()
        }

        btnAbrir.setOnClickListener {
            enviarComando("1")
        }

        btnCerrar.setOnClickListener {
            enviarComando("0")
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
                return
            }
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        if (pairedDevices.isNullOrEmpty()) {
            Toast.makeText(this, "No hay dispositivos vinculados. Empareja el HC-05 en los ajustes de tu celular.", Toast.LENGTH_LONG).show()
            return
        }

        var dispositivoHC05: BluetoothDevice? = null
        for (device in pairedDevices) {
            if (device.name.contains("HC-05") || device.name.contains("HC-06")) {
                dispositivoHC05 = device
                break
            }
        }

        if (dispositivoHC05 == null) {
            Toast.makeText(this, "No se encontró el módulo HC-05 entre los vinculados", Toast.LENGTH_LONG).show()
            return
        }

        Thread {
            try {
                bluetoothSocket = dispositivoHC05.createRfcommSocketToServiceRecord(PORT_UUID)
                bluetoothAdapter.cancelDiscovery()
                bluetoothSocket?.connect()

                runOnUiThread {
                    Toast.makeText(this, "¡Conectado exitosamente al HC-05!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Error de conexión: Asegúrate de que el Arduino esté encendido.", Toast.LENGTH_LONG).show()
                }
                try {
                    bluetoothSocket?.close()
                } catch (closeException: IOException) {

                }
            }
        }.start()
    }

    private fun enviarComando(comando: String) {
        if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
            try {
                bluetoothSocket?.outputStream?.write(comando.toByteArray())
                val mensaje = if (comando == "1") "Válvula Abierta" else "Válvula Cerrada"
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "Error al enviar el comando", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Primero debes conectar el módulo Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }
}