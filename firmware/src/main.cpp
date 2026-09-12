#include <Arduino.h>
#include <math.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <DHT.h>

#if __has_include("secrets.h")
#include "secrets.h"
#else
#include "secrets.example.h"
#endif

// Typical ESP32-WROOM DevKit: onboard LED on GPIO 2, DHT11 data on GPIO 4.
static const int LED_PIN = 2;
static const int DHT_PIN = 4;
static const uint8_t DHT_TYPE = DHT11;
static const unsigned long TELEMETRY_INTERVAL_MS = 5000;
static const unsigned long WIFI_RETRY_MS = 4000;
static const unsigned long MQTT_RETRY_MS = 3000;

DHT dht(DHT_PIN, DHT_TYPE);
WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);

bool ledOn = false;
bool usingSimulatedTelemetry = false;
unsigned long lastTelemetryMs = 0;
unsigned long lastWifiAttemptMs = 0;
unsigned long lastMqttAttemptMs = 0;

String statusTopic() {
  return String("devices/") + DEVICE_ID + "/status";
}

String telemetryTopic() {
  return String("devices/") + DEVICE_ID + "/telemetry";
}

String commandTopic() {
  return String("devices/") + DEVICE_ID + "/command";
}

void applyLed(bool on) {
  ledOn = on;
  digitalWrite(LED_PIN, on ? HIGH : LOW);
}

void publishStatus(bool online) {
  JsonDocument doc;
  doc["state"] = online ? "online" : "offline";
  doc["name"] = DEVICE_NAME;
  doc["fw"] = "1.0.0";
  char buffer[192];
  serializeJson(doc, buffer, sizeof(buffer));
  mqtt.publish(statusTopic().c_str(), buffer, true);
}

bool readDht(float &tempC, float &humidity) {
  humidity = dht.readHumidity();
  tempC = dht.readTemperature();
  return !isnan(humidity) && !isnan(tempC);
}

void publishTelemetry() {
  float tempC = 0;
  float humidity = 0;
  const bool sensorOk = readDht(tempC, humidity);

  // Fallback is intentional: a board without DHT11 still demos the full stack.
  if (!sensorOk) {
    usingSimulatedTelemetry = true;
    const float t = millis() / 1000.0f;
    tempC = 23.5f + sinf(t / 17.0f) * 1.8f;
    humidity = 56.0f + cosf(t / 13.0f) * 6.0f;
  } else {
    usingSimulatedTelemetry = false;
  }

  JsonDocument doc;
  doc["tempC"] = roundf(tempC * 10.0f) / 10.0f;
  doc["humidity"] = roundf(humidity * 10.0f) / 10.0f;
  doc["led"] = ledOn;
  doc["rssi"] = WiFi.RSSI();
  doc["source"] = usingSimulatedTelemetry ? "SIMULATED" : "SENSOR";

  char buffer[256];
  serializeJson(doc, buffer, sizeof(buffer));
  mqtt.publish(telemetryTopic().c_str(), buffer, false);

  Serial.printf(
      "[telemetry] temp=%.1fC hum=%.1f%% led=%s rssi=%d source=%s\n",
      tempC,
      humidity,
      ledOn ? "on" : "off",
      WiFi.RSSI(),
      usingSimulatedTelemetry ? "SIMULATED" : "SENSOR");
}

void onCommand(char *topic, byte *payload, unsigned int length) {
  String body;
  body.reserve(length);
  for (unsigned int i = 0; i < length; i++) {
    body += static_cast<char>(payload[i]);
  }

  JsonDocument doc;
  DeserializationError error = deserializeJson(doc, body);
  if (error) {
    Serial.printf("[command] ignore invalid JSON on %s: %s\n", topic, body.c_str());
    return;
  }

  if (doc["led"].is<bool>()) {
    applyLed(doc["led"].as<bool>());
    Serial.printf("[command] LED -> %s\n", ledOn ? "on" : "off");
    publishTelemetry();
  }
}

void connectWifi() {
  if (WiFi.status() == WL_CONNECTED) {
    return;
  }
  const unsigned long now = millis();
  if (now - lastWifiAttemptMs < WIFI_RETRY_MS) {
    return;
  }
  lastWifiAttemptMs = now;

  Serial.printf("[wifi] connecting to %s\n", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}

void connectMqtt() {
  if (mqtt.connected() || WiFi.status() != WL_CONNECTED) {
    return;
  }
  const unsigned long now = millis();
  if (now - lastMqttAttemptMs < MQTT_RETRY_MS) {
    return;
  }
  lastMqttAttemptMs = now;

  const String offline = "{\"state\":\"offline\",\"name\":\"" + String(DEVICE_NAME) + "\"}";
  Serial.printf("[mqtt] connecting to %s:%d as %s\n", MQTT_HOST, MQTT_PORT, DEVICE_ID);

  bool ok;
  if (strlen(MQTT_USER) > 0) {
    ok = mqtt.connect(DEVICE_ID, MQTT_USER, MQTT_PASSWORD, statusTopic().c_str(), 1, true, offline.c_str());
  } else {
    ok = mqtt.connect(DEVICE_ID, statusTopic().c_str(), 1, true, offline.c_str());
  }

  if (!ok) {
    Serial.printf("[mqtt] failed, rc=%d\n", mqtt.state());
    return;
  }

  mqtt.subscribe(commandTopic().c_str(), 1);
  publishStatus(true);
  Serial.println("[mqtt] connected, subscribed to command topic");
}

void setup() {
  pinMode(LED_PIN, OUTPUT);
  applyLed(false);
  Serial.begin(115200);
  delay(200);
  Serial.println();
  Serial.println("ESP32 IoT console firmware");
  Serial.printf("deviceId=%s broker=%s:%d\n", DEVICE_ID, MQTT_HOST, MQTT_PORT);

  dht.begin();
  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  mqtt.setCallback(onCommand);
  mqtt.setBufferSize(512);
  mqtt.setKeepAlive(20);
  connectWifi();
}

void loop() {
  connectWifi();
  if (WiFi.status() == WL_CONNECTED) {
    connectMqtt();
    mqtt.loop();
  }

  if (mqtt.connected() && millis() - lastTelemetryMs >= TELEMETRY_INTERVAL_MS) {
    lastTelemetryMs = millis();
    publishTelemetry();
  }
}
