package com.iotconsole.mqtt;

import com.iotconsole.service.DeviceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MqttBrokerClient implements CommandPublisher, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttBrokerClient.class);

    private final DeviceService deviceService;
    private final String host;
    private final int port;
    private final String clientId;
    private final String username;
    private final String password;
    private final boolean autoConnect;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "mqtt-connect");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService inbound = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "mqtt-inbound");
        thread.setDaemon(true);
        return thread;
    });

    private volatile MqttClient client;

    public MqttBrokerClient(
            DeviceService deviceService,
            @Value("${app.mqtt.host}") String host,
            @Value("${app.mqtt.port}") int port,
            @Value("${app.mqtt.client-id}") String clientId,
            @Value("${app.mqtt.username:}") String username,
            @Value("${app.mqtt.password:}") String password,
            @Value("${app.mqtt.auto-connect:true}") boolean autoConnect
    ) {
        this.deviceService = deviceService;
        this.host = host;
        this.port = port;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.autoConnect = autoConnect;
    }

    @PostConstruct
    public void start() {
        if (!autoConnect) {
            log.info("MQTT auto-connect disabled");
            return;
        }
        io.execute(this::connectWithRetry);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        io.shutdownNow();
        inbound.shutdownNow();
        MqttClient current = client;
        if (current != null && current.isConnected()) {
            try {
                current.disconnect();
            } catch (MqttException ignored) {
                // shutting down
            }
        }
        if (current != null) {
            try {
                current.close();
            } catch (MqttException ignored) {
                // shutting down
            }
        }
    }

    public boolean isConnected() {
        MqttClient current = client;
        return current != null && current.isConnected();
    }

    @Override
    public void publishLed(String deviceId, boolean led) {
        MqttClient current = client;
        if (current == null || !current.isConnected()) {
            log.warn("MQTT is disconnected; LED command for {} was not published", deviceId);
            return;
        }
        try {
            MqttMessage message = new MqttMessage(MqttPayloads.commandJson(led).getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            message.setRetained(false);
            current.publish(MqttTopics.command(deviceId), message);
            log.info("Published LED command device={} led={}", deviceId, led);
        } catch (MqttException ex) {
            log.warn("Failed to publish LED command to {}: {}", deviceId, ex.getMessage());
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT {} to {}", reconnect ? "reconnected" : "connected", serverURI);
        subscribe();
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        inbound.execute(() -> handleMessage(topic, payload));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not used
    }

    private void connectWithRetry() {
        while (running.get()) {
            try {
                connectOnce();
                return;
            } catch (Exception ex) {
                log.warn("MQTT broker {}:{} not ready ({}). Retrying in 3s", host, port, ex.getMessage());
                sleepQuietly(3_000);
            }
        }
    }

    private void connectOnce() throws MqttException {
        String uri = "tcp://" + host + ":" + port;
        MqttClient mqttClient = new MqttClient(uri, clientId, new MemoryPersistence());
        mqttClient.setCallback(this);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setKeepAliveInterval(20);
        options.setConnectionTimeout(8);
        if (username != null && !username.isBlank()) {
            options.setUserName(username);
            options.setPassword(password == null ? new char[0] : password.toCharArray());
        }
        mqttClient.connect(options);
        this.client = mqttClient;
        subscribe();
        log.info("MQTT connected to {}", uri);
    }

    private void subscribe() {
        MqttClient current = client;
        if (current == null || !current.isConnected()) {
            return;
        }
        try {
            current.subscribe(MqttTopics.STATUS_FILTER, 1);
            current.subscribe(MqttTopics.TELEMETRY_FILTER, 1);
            log.info("Subscribed to {} and {}", MqttTopics.STATUS_FILTER, MqttTopics.TELEMETRY_FILTER);
        } catch (MqttException ex) {
            log.warn("MQTT subscribe failed: {}", ex.getMessage());
        }
    }

    private void handleMessage(String topic, String payload) {
        MqttTopics.parse(topic).ifPresent(parsed -> {
            try {
                if (parsed.isStatus()) {
                    deviceService.onStatus(parsed.deviceId(), MqttPayloads.parseStatus(payload));
                } else if (parsed.isTelemetry()) {
                    deviceService.onTelemetry(parsed.deviceId(), MqttPayloads.parseTelemetry(payload));
                }
            } catch (Exception ex) {
                log.warn("Failed to handle MQTT {} ({}): {}", topic, payload, ex.getMessage());
            }
        });
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
