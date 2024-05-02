#include <SPI.h>
#include <Wire.h>
#include <MFRC522.h>
#include <EEPROM.h>
#include <ezButton.h>
#include <ESP8266WiFi.h>
#include <WebSocketsClient.h>  // Thư viện WebSocketsServer
#include <ArduinoJson.h>

#define WIFI_SSID "phuc"
#define WIFI_PASSWORD "phuc1206"

WebSocketsClient webSocket;

#define SS_PIN D8   // The ESP8266 pin D8
#define RST_PIN D2  // The ESP8266 pin D2
#define RELAY D0    // The ESP8266 pin connects to relay
#define BUZZER D1
#define REDLED 10
// #define GREENLED 10

#define SHORT_PRESS_TIME 1000  // 1000 milliseconds
#define LONG_PRESS_TIME 1000   // 1000 milliseconds
#define btn D3

ezButton button(btn);

int lastState = LOW;
int NewState;
String jsonData;
boolean isAdminMode;

#define STATE_STARTUP 0
#define STATE_STARTING 1
#define STATE_WAITING 2
#define STATE_SCAN_INVALID 3
#define STATE_SCAN_VALID 4
#define STATE_SCAN_MASTER 5
#define STATE_ADDED_CARD 6
#define STATE_REMOVED_CARD 7

const int cardArrSize = 10;
const int cardSize = 4;
byte cardArr[cardArrSize][cardSize];
byte masterCard[cardSize] = { 211, 110, 62, 20 };  //Change Master Card ID
byte readCard[cardSize];
byte cardsStored = 0;

MFRC522 mfrc522(SS_PIN, RST_PIN);

// byte keyTagUID[4] = { 0xD3, 0x6E, 0x3E, 0x14 };

byte currentState = STATE_STARTUP;
unsigned long LastStateChangeTime;
unsigned long StateWaitTime;
unsigned long time_pressed = 0;
unsigned long time_released = 0;
bool is_pressing = false;
bool is_long_detected = false;
String createDoorStatusJsonObject(String doorStatus) {
  JsonDocument doc;  // Allocate memory for the JSON object
  doc["doorStatus"] = doorStatus;
  String jsonString;
  serializeJson(doc, jsonString);  // Convert JSON object to string
  return jsonString;
}
void webSocketEvent(WStype_t type, uint8_t* payload, size_t length) {
  switch (type) {
    case WStype_DISCONNECTED:  // Sự kiện khi client ngắt kết nối
      Serial.printf("[WSc] Disconnected!\n");
      break;
    case WStype_CONNECTED:  // Sự kiện khi client kết nối
      Serial.printf("[WSc] Connected to url: %s\n", payload);

      break;
    case WStype_TEXT:  // Sự kiện khi nhận được thông điệp dạng TEXT
      Serial.printf("[WSc] get text: %s\n", payload);
      {
        StaticJsonDocument<200> doc;
        DeserializationError error = deserializeJson(doc, payload);
        if(!doc["doorStatus"]){
          break;
        }
        const char* doorStatus = doc["doorStatus"];
        if (strcmp(doorStatus, "DOOR_OPEN") == 0) {
          digitalWrite(RELAY, HIGH);
          beep();
          delay(3000);
          digitalWrite(RELAY, LOW);
          jsonData = createDoorStatusJsonObject("DOOR_CLOSE");
          webSocket.sendTXT(jsonData.c_str());
        } 
        
      }

      break;
    case WStype_BIN:  // Sự kiện khi nhận được thông điệp dạng BINARY
      Serial.printf("[WSc] get binary length: %u\n", length);
      hexdump(payload, length);
      // webSocket.sendBIN(payload, length);
      break;
  }
}
void startWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);  // Kết nối vào mạng WiFi
  Serial.print("Connecting to ");
  Serial.print(WIFI_SSID);
  // Chờ kết nối WiFi được thiết lập
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }
  Serial.println("\n");
  Serial.println("Connection established!");
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());  // Gởi địa chỉ IP đến máy tinh
}
void connectWebSocket() {
  webSocket.begin("192.168.103.191", 3000, "/");  // Địa chỉ websocket server, port và URL
  webSocket.onEvent(webSocketEvent);

  // webSocket.setAuthorization("user", "password");        // Sử dụng thông tin chứng thực nếu cần

  webSocket.setReconnectInterval(5000);  // Thử lại sau 5s nếu kết nối không thành công
}

void success_buzzer() {
  digitalWrite(BUZZER, HIGH);
  delay(2000);
  digitalWrite(BUZZER, LOW);
}

void Failure_buzzer() {
  for (int i = 0; i < 3; i++) {
    digitalWrite(BUZZER, HIGH);
    delay(100);
    digitalWrite(BUZZER, LOW);
    delay(50);
  }
}

void Admin_buzzer() {
  digitalWrite(BUZZER, HIGH);
  delay(500);
  digitalWrite(BUZZER, LOW);
  delay(50);
  digitalWrite(BUZZER, HIGH);
  delay(100);
  digitalWrite(BUZZER, LOW);
  delay(50);
  digitalWrite(BUZZER, HIGH);
  delay(100);
  digitalWrite(BUZZER, LOW);
  delay(50);
}

void beep() {
  digitalWrite(BUZZER, HIGH);
  delay(100);
  digitalWrite(BUZZER, LOW);
}

int readCardState() {
  int index;
  Serial.print("Card Data - ");
  for (index = 0; index < 4; index++) {
    readCard[index] = mfrc522.uid.uidByte[index];
    Serial.print(readCard[index]);
    if (index < 3) {
      Serial.print(",");
    }
  }
  Serial.println(" ");
  //Check Master Card
  if ((memcmp(readCard, masterCard, 4)) == 0) {
    return STATE_SCAN_MASTER;
  }
  if (cardsStored == 0) {
    return STATE_SCAN_INVALID;
  }
  for (index = 0; index < cardsStored; index++) {
    if ((memcmp(readCard, cardArr[index], 4)) == 0) {
      return STATE_SCAN_VALID;
    }
  }
  return STATE_SCAN_INVALID;
}
//------------------------------------------------------------------------------------
void addReadCard() {
  int cardIndex;
  int index;
  if (cardsStored <= 20) {
    cardsStored++;
    cardIndex = cardsStored;
    cardIndex--;
  }
  for (index = 0; index < 4; index++) {
    cardArr[cardIndex][index] = readCard[index];
  }
  // Write the updated card list to EEPROM
  EEPROM.write(0, cardsStored);
  for (int i = 0; i < cardsStored; i++) {
    for (int j = 0; j < 4; j++) {
      EEPROM.write((i * 4) + j + 1, cardArr[i][j]);
    }
  }
  EEPROM.commit();
}
//------------------------------------------------------------------------------------
void removeReadCard() {
  int cardIndex;
  int index;
  boolean found = false;
  if (cardsStored == 0) {
    return;
  }
  for (index = 0; index < cardsStored; index++) {
    if ((memcmp(readCard, cardArr[index], 4)) == 0) {
      found = true;
      cardIndex = index;
    }
  }
  if (found == true) {
    // Remove the card from the array
    for (index = cardIndex; index < (cardsStored - 1); index++) {
      for (int j = 0; j < 4; j++) {
        cardArr[index][j] = cardArr[index + 1][j];
      }
    }
    cardsStored--;
    // Write the updated card list to EEPROM
    EEPROM.write(0, cardsStored);
    for (int i = 0; i < cardsStored; i++) {
      for (int j = 0; j < 4; j++) {
        EEPROM.write((i * 4) + j + 1, cardArr[i][j]);
      }
    }
    EEPROM.commit();
  }
}
//------------------------------------------------------------------------------------
void updateState(byte aState) {
  if (aState == currentState) {
    return;
  }
  // do state change
  if (isAdminMode) {
    switch (aState) {
      case STATE_STARTING:
        StateWaitTime = 1000;
        // digitalWrite(REDLED, HIGH);
        // digitalWrite(GREENLED, LOW);
        break;
      case STATE_WAITING:
        StateWaitTime = 0;
        // digitalWrite(REDLED, LOW);
        // digitalWrite(GREENLED, LOW);
        break;
      case STATE_SCAN_INVALID:
        if (currentState == STATE_SCAN_MASTER) {
          addReadCard();
          aState = STATE_ADDED_CARD;
          StateWaitTime = 2000;
          // digitalWrite(REDLED, LOW);
          // digitalWrite(GREENLED, HIGH);
          Serial.println("Access Granted. Card added");
          success_buzzer();

        } else if (currentState == STATE_REMOVED_CARD) {
          return;
        }
        break;
      case STATE_SCAN_VALID:
        if (currentState == STATE_SCAN_MASTER) {
          removeReadCard();
          aState = STATE_REMOVED_CARD;
          StateWaitTime = 2000;
          // digitalWrite(REDLED, LOW);
          // digitalWrite(GREENLED, HIGH);
          Serial.println("Access Granted. Card removed");
          success_buzzer();
        } else if (currentState == STATE_ADDED_CARD) {
          return;
        }
        break;
      case STATE_SCAN_MASTER:
        StateWaitTime = 5000;
        // digitalWrite(REDLED, LOW);
        // digitalWrite(GREENLED, HIGH);
        Serial.println("Access Granted. Master Card detected");
        beep();
        break;
    }
  } else {
    switch (aState) {
      case STATE_STARTING:
        StateWaitTime = 1000;
        // digitalWrite(REDLED, HIGH);
        // digitalWrite(GREENLED, LOW);
        break;
      case STATE_WAITING:
        StateWaitTime = 0;
        // digitalWrite(REDLED, LOW);
        // digitalWrite(GREENLED, LOW);
        break;
      case STATE_SCAN_INVALID:
        StateWaitTime = 2000;
        digitalWrite(REDLED, HIGH);
        // digitalWrite(GREENLED, LOW);
        Serial.println("Access Denied. Invalid Card detected");
        Failure_buzzer();
        jsonData = createDoorStatusJsonObject("DOOR_OPEN_FAIL");
        webSocket.sendTXT(jsonData.c_str());
        break;
      case STATE_SCAN_VALID:
        StateWaitTime = 2000;
        // digitalWrite(REDLED, LOW);
        // digitalWrite(GREENLED, HIGH);
        Serial.println("Access Granted. Valid Card detected");
        jsonData = createDoorStatusJsonObject("DOOR_OPEN");
        webSocket.sendTXT(jsonData.c_str());
        beep();
        digitalWrite(RELAY, HIGH);
        delay(StateWaitTime);
        digitalWrite(RELAY, LOW);
        jsonData = createDoorStatusJsonObject("DOOR_CLOSE");
        webSocket.sendTXT(jsonData.c_str());
        break;
      case STATE_SCAN_MASTER:
        StateWaitTime = 2000;
        // digitalWrite(REDLED, LOW);
        // digitalWrite(GREENLED, HIGH);
        jsonData = createDoorStatusJsonObject("DOOR_OPEN");
        webSocket.sendTXT(jsonData.c_str());
        Serial.println("Access Granted. Master Card detected");
        digitalWrite(RELAY, HIGH);
        beep();
        delay(StateWaitTime);
        digitalWrite(RELAY, LOW);
        jsonData = createDoorStatusJsonObject("DOOR_CLOSE");
        webSocket.sendTXT(jsonData.c_str());
        break;
    }
  }
  currentState = aState;
  LastStateChangeTime = millis();
}

void setup() {
  startWiFi();
  connectWebSocket();
  // Start serial communication
  Serial.begin(115200);
  // Initialize the SPI bus
  SPI.begin();
  // Initialize the  MFRC522 reader
  mfrc522.PCD_Init();
  // Initialize the EEPROM
  EEPROM.begin(512);
  // Read the stored card list from EEPROM
  cardsStored = EEPROM.read(0);
  if (cardsStored > cardArrSize) {
    cardsStored = 0;
  }
  for (int i = 0; i < cardsStored; i++) {
    for (int j = 0; j < 4; j++) {
      cardArr[i][j] = EEPROM.read((i * 4) + j + 1);
    }
  }
  LastStateChangeTime = millis();
  isAdminMode = false;
  updateState(STATE_STARTING);
  pinMode(REDLED, OUTPUT);
  // pinMode(GREENLED, OUTPUT);
  pinMode(RELAY, OUTPUT);
  pinMode(BUZZER, OUTPUT);
  digitalWrite(RELAY, LOW);
  button.setDebounceTime(50);  // set debounce time to 50 milliseconds
  Serial.println("Put your card to the reader...");
  Serial.println();
}
void loop() {
  button.loop();

  webSocket.loop();
  if (button.isPressed()) {
    time_pressed = millis();
    is_pressing = true;
    is_long_detected = false;
  }

  if (button.isReleased()) {
    is_pressing = false;
    time_released = millis();

    long press_duration = time_released - time_pressed;

    if (press_duration < SHORT_PRESS_TIME) {
      Serial.println("Button Pressed. Access Granted");
      Serial.println();
      // digitalWrite(REDLED, LOW);
      // digitalWrite(GREENLED, HIGH);
      jsonData = createDoorStatusJsonObject("DOOR_OPEN");
      webSocket.sendTXT(jsonData.c_str());
      digitalWrite(RELAY, HIGH);
      beep();
      delay(3000);
      digitalWrite(RELAY, LOW);
      jsonData = createDoorStatusJsonObject("DOOR_CLOSE");
      webSocket.sendTXT(jsonData.c_str());
      // digitalWrite(GREENLED, LOW);
    }
  }

  if (is_pressing == true && is_long_detected == false) {
    long press_duration = millis() - time_pressed;

    if (press_duration > LONG_PRESS_TIME) {
      // Serial.println("A long press is detected");
      isAdminMode = !isAdminMode;
      Admin_buzzer();
      is_long_detected = true;
    }
  }
  byte cardState;
  if ((currentState != STATE_WAITING) && (StateWaitTime > 0) && (LastStateChangeTime + StateWaitTime < millis())) {
    updateState(STATE_WAITING);
  }
  // Look for new cards
  if (!mfrc522.PICC_IsNewCardPresent()) {
    return;
  }
  // Select one of the cards
  if (!mfrc522.PICC_ReadCardSerial()) {
    return;
  }

  cardState = readCardState();
  updateState(cardState);
  lastState = NewState;
}
