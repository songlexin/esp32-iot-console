package com.iotconsole.mqtt;

public interface CommandPublisher {

    void publishLed(String deviceId, boolean led);
}
