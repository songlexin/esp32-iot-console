#pragma once

// Copy this file to secrets.h (gitignored) and fill in local values.
// Do not commit real Wi-Fi passwords.

#ifndef WIFI_SSID
#define WIFI_SSID "YOUR_WIFI_SSID"
#endif

#ifndef WIFI_PASSWORD
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
#endif

// LAN IP of the machine running Mosquitto, or later the public host:
// #define MQTT_HOST "116.62.158.35"
#ifndef MQTT_HOST
#define MQTT_HOST "192.168.1.100"
#endif

#ifndef MQTT_PORT
#define MQTT_PORT 1883
#endif

#ifndef MQTT_USER
#define MQTT_USER ""
#endif

#ifndef MQTT_PASSWORD
#define MQTT_PASSWORD ""
#endif

#ifndef DEVICE_ID
#define DEVICE_ID "esp32-devkit-01"
#endif

#ifndef DEVICE_NAME
#define DEVICE_NAME "ESP32 DevKit"
#endif
