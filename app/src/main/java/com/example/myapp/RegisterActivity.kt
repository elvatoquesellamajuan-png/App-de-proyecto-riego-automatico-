package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        val etRegUsuario = findViewById<EditText>(R.id.etRegUsuario)
        val etRegPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnRegistrarUser = findViewById<Button>(R.id.btnRegistrarUser)
        val btnVolverLogin = findViewById<Button>(R.id.btnVolverLogin)

        btnRegistrarUser.setOnClickListener {
            val user = etRegUsuario.text.toString().trim()
            val pass = etRegPassword.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                val exito = dbHelper.insertarUsuario(user, pass)
                if (exito) {
                    Toast.makeText(this, "¡Usuario registrado con éxito!", Toast.LENGTH_SHORT).show()
                    finish() // Regresa al login
                } else {
                    Toast.makeText(this, "El usuario ya existe o hubo un error", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnVolverLogin.setOnClickListener {
            finish()
        }
    }
}