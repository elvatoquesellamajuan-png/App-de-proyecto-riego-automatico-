#include "Arduino_LED_Matrix.h"
ArduinoLEDMatrix matrix;

const int PIN_SENSOR = A0;
const int PIN_BOMBA  = 7;
const int RELE_ON  = LOW;    // la mayoria de modulos rele son activos en LOW
const int RELE_OFF = HIGH;   // si el tuyo es al reves, intercambia

const int UMBRAL_SECO = 800;                        // calibrar (ver abajo)
const unsigned long TIEMPO_RIEGO = 3000;            // 3 s de bomba
const unsigned long TIMEOUT_CONEXION = 180000UL;    // 3 minutos
const unsigned long ESPERA_ENTRE_RIEGOS = 1800000UL; // 30 min

String mensaje = "";
unsigned long ultimoContacto = 0;
unsigned long ultimoRiego = 0;
bool esperandoRespuesta = false;
int estadoMostrado = -1;   // 0 = humeda, 1 = seca

// Matriz 8 filas x 12 columnas
byte GOTA[8][12] = {
  {0,0,0,0,0,1,1,0,0,0,0,0},
  {0,0,0,0,0,1,1,0,0,0,0,0},
  {0,0,0,0,1,1,1,1,0,0,0,0},
  {0,0,0,1,1,1,1,1,1,0,0,0},
  {0,0,0,1,1,1,1,1,1,0,0,0},
  {0,0,0,1,1,1,1,1,1,0,0,0},
  {0,0,0,0,1,1,1,1,0,0,0,0},
  {0,0,0,0,0,1,1,0,0,0,0,0}
};
byte EQUIS[8][12] = {
  {0,0,1,0,0,0,0,0,0,1,0,0},
  {0,0,0,1,0,0,0,0,1,0,0,0},
  {0,0,0,0,1,0,0,1,0,0,0,0},
  {0,0,0,0,0,1,1,0,0,0,0,0},
  {0,0,0,0,0,1,1,0,0,0,0,0},
  {0,0,0,0,1,0,0,1,0,0,0,0},
  {0,0,0,1,0,0,0,0,1,0,0,0},
  {0,0,1,0,0,0,0,0,0,1,0,0}
};
byte REGANDO[8][12] = {
  {1,0,1,0,1,0,1,0,1,0,1,0},
  {0,1,0,1,0,1,0,1,0,1,0,1},
  {1,0,1,0,1,0,1,0,1,0,1,0},
  {0,1,0,1,0,1,0,1,0,1,0,1},
  {1,0,1,0,1,0,1,0,1,0,1,0},
  {0,1,0,1,0,1,0,1,0,1,0,1},
  {1,0,1,0,1,0,1,0,1,0,1,0},
  {0,1,0,1,0,1,0,1,0,1,0,1}
};

void setup() {
  digitalWrite(PIN_BOMBA, RELE_OFF);   // apagado antes de configurar
  pinMode(PIN_BOMBA, OUTPUT);
  Serial.begin(9600);
  Serial1.begin(9600);
  matrix.begin();
  ultimoContacto = millis();
}

void loop() {
  leerBluetooth();
  actualizarMatriz();

  if (!estaConectado()) {
    esperandoRespuesta = false;
    riegoAutomatico();
  }
}

bool estaConectado() {
  return millis() - ultimoContacto < TIMEOUT_CONEXION;
}

bool estaSeca() {
  return analogRead(PIN_SENSOR) > UMBRAL_SECO;  // mayor valor = mas seco
}

void leerBluetooth() {
  while (Serial1.available()) {
    char c = Serial1.read();
    ultimoContacto = millis();           // cualquier dato = conectado
    if (c == '\n') {
      mensaje.trim();
      mensaje.toLowerCase();
      procesar(mensaje);
      mensaje = "";
    } else {
      mensaje += c;
    }
  }
}

void procesar(String m) {
  Serial.println("Recibido: " + m);

  if (m == "estado") {
    if (estaSeca()) {
      Serial1.println("La planta esta SECA. Quieres regar? (si/no)");
      esperandoRespuesta = true;
    } else {
      Serial1.println("La planta esta HUMEDA");
      esperandoRespuesta = false;
    }
  } else if ((m == "si" || m == "sí") && esperandoRespuesta) {
    esperandoRespuesta = false;
    Serial1.println("Regando...");
    regar();
    Serial1.println("Listo");
  } else if (m == "no" && esperandoRespuesta) {
    esperandoRespuesta = false;
    Serial1.println("Ok, no riego");
  } else if (m == "valor") {
    Serial1.println("Lectura sensor: " + String(analogRead(PIN_SENSOR)));
  } else {
    Serial1.println("Comandos: estado, valor");
  }
}

void riegoAutomatico() {
  bool pasoEspera = (ultimoRiego == 0) || (millis() - ultimoRiego > ESPERA_ENTRE_RIEGOS);
  if (estaSeca() && pasoEspera) {
    regar();
  }
}

void regar() {
  matrix.renderBitmap(REGANDO, 8, 12);
  digitalWrite(PIN_BOMBA, RELE_ON);
  delay(TIEMPO_RIEGO);
  digitalWrite(PIN_BOMBA, RELE_OFF);
  ultimoRiego = millis();
  estadoMostrado = -1;   // fuerza refrescar la matriz
}

void actualizarMatriz() {
  int estado = estaSeca() ? 1 : 0;
  if (estado != estadoMostrado) {
    if (estado) {
      matrix.renderBitmap(EQUIS, 8, 12);
    } else {
      matrix.renderBitmap(GOTA, 8, 12);
    }
    estadoMostrado = estado;
  }
}
