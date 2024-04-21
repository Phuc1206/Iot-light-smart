#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <WebSocketsClient.h> //https://github.com/Links2004/arduinoWebSockets
#include <ArduinoJson.h>
WebSocketsClient webSocket;
const char* ssid = "phuc"; //Đổi thành wifi của bạn
const char* password = "phuc1206"; //Đổi pass luôn
const char* ip_host = "192.168.138.191"; //Đổi luôn IP host của PC nha
const uint16_t port = 3000; //Port thích đổi thì phải đổi ở server nữa
const int LED_sensor = D4;
const int LED_pk = D1;
const int LED_pn = D2;
const int Sensor_pin = A0;
// const int BTN = 0;
bool clientConnect = false;

unsigned long CurrentMillis, PreviousMillis, DataSendingTime = (unsigned long) 1000 * 10;
// Function to create a JSON object with sensor data
String createSensorJsonObject(int sensorValue) {
  JsonDocument doc; // Allocate memory for the JSON object
  doc["sensorValue"] = sensorValue;
  String jsonString;
  serializeJson(doc, jsonString); // Convert JSON object to string
  return jsonString;
}
// Function to create a JSON object with LED status
String createLEDStatusJsonObject(String ledStatus) {
  JsonDocument doc; // Allocate memory for the JSON object
  doc["ledStatus"] = ledStatus;
  String jsonString;
  serializeJson(doc, jsonString); // Convert JSON object to string
  return jsonString;
}
void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
  switch (type) {
    case WStype_DISCONNECTED:
      Serial.printf("[WSc] Disconnected!\n");
      break;
    case WStype_CONNECTED:
      {
        Serial.printf("[WSc] Connected to url: %s\n", payload);
        clientConnect = true;
      }
      break;
    case WStype_TEXT:
      Serial.printf("[WSc] get text: %s\n", payload);
      if (strcmp((char*)payload, "LED_ON_PK") == 0) {
        digitalWrite(LED_pk, HIGH); // Khi client phát sự kiện "LED_ON" thì server sẽ bật LED
        String jsonData = createLEDStatusJsonObject("LED_ON_PK");
        webSocket.sendTXT(jsonData.c_str());
      } else if (strcmp((char*)payload, "LED_OFF_PK") == 0) {
        digitalWrite(LED_pk, LOW); // Khi client phát sự kiện "LED_OFF" thì server sẽ tắt LED
        String jsonData = createLEDStatusJsonObject("LED_OFF_PK");
        webSocket.sendTXT(jsonData.c_str());
      } else if (strcmp((char*)payload, "LED_ON_PN") == 0) {
        digitalWrite(LED_pn, HIGH); 
        String jsonData = createLEDStatusJsonObject("LED_ON_PN");
        webSocket.sendTXT(jsonData.c_str());
      } else if (strcmp((char*)payload, "LED_OFF_PN") == 0) {
        digitalWrite(LED_pn, LOW); 
        String jsonData = createLEDStatusJsonObject("LED_OFF_PN");
        webSocket.sendTXT(jsonData.c_str());
      }
      
      break;
    case WStype_BIN:
      Serial.printf("[WSc] get binary length: %u\n", length);
      break;
  }
}
void setup() {
  pinMode(LED_sensor, OUTPUT);
  pinMode(LED_pk, OUTPUT);
  pinMode(LED_pn, OUTPUT);

  // pinMode(BTN, INPUT);
  Serial.begin(115200);
  Serial.println("ESP8266 Websocket Client");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  webSocket.begin(ip_host, port);
  webSocket.onEvent(webSocketEvent);
}
void loop() {
  webSocket.loop();
   CurrentMillis = millis();
  if (CurrentMillis - PreviousMillis > DataSendingTime) {
    PreviousMillis = CurrentMillis;

    if (clientConnect) {
      int sensorValue = analogRead(Sensor_pin); // Read sensor value
      String jsonData = createSensorJsonObject(sensorValue);
      Serial.println("\n\nSending sensor data (JSON): " + jsonData);
      webSocket.sendTXT(jsonData.c_str());
      if (sensorValue > 1000){
        digitalWrite(LED_sensor, HIGH);
      } else{
        digitalWrite(LED_sensor, LOW); 
      }
    }
  }

  // delay(200);
  // static bool isPressed = false;
  // if (!isPressed && digitalRead(BTN) == 0) { //Nhấn nút nhấn GPIO0
  //   isPressed = true;
  //   webSocket.sendTXT("BTN_PRESSED");
  // } else if (isPressed && digitalRead(BTN)) { //Nhả nút nhấn GPIO0
  //   isPressed = false;
  //   webSocket.sendTXT("BTN_RELEASE");
  // }
  
}