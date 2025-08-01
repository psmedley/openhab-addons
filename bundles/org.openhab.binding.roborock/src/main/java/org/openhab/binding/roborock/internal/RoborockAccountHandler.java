/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.roborock.internal;

import static org.openhab.binding.roborock.internal.RoborockBindingConstants.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.openhab.binding.roborock.internal.api.Home;
import org.openhab.binding.roborock.internal.api.HomeData;
import org.openhab.binding.roborock.internal.api.Login;
import org.openhab.binding.roborock.internal.api.Login.Rriot;
import org.openhab.binding.roborock.internal.discovery.RoborockVacuumDiscoveryService;
import org.openhab.binding.roborock.internal.util.ProtocolUtils;
import org.openhab.binding.roborock.internal.util.SchedulerTask;
import org.openhab.core.storage.Storage;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * The {@link RoborockAccountHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Paul Smedley - Initial contribution
 */
@NonNullByDefault
public class RoborockAccountHandler extends BaseBridgeHandler implements MqttCallbackExtended {

    private final Logger logger = LoggerFactory.getLogger(RoborockAccountHandler.class);

    private final Storage<String> sessionStorage;
    private @Nullable RoborockAccountConfiguration config;
    private final SchedulerTask initTask;
    private final SchedulerTask mqttConnectTask;
    private final RoborockWebTargets webTargets;
    private @Nullable MqttClient mqttClient;
    private String token = "";
    private String baseUri = "";
    private Rriot rriot = new Login().new Rriot();
    private final SecureRandom secureRandom = new SecureRandom();

    /** The file we store definitions in */
    protected final Map<Thing, RoborockVacuumHandler> childDevices = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();

    public RoborockAccountHandler(Bridge bridge, Storage<String> stateStorage, HttpClient httpClient) {
        super(bridge);
        webTargets = new RoborockWebTargets(httpClient);
        sessionStorage = stateStorage;
        initTask = new SchedulerTask(scheduler, logger, "API Init", this::initAPI);
        mqttConnectTask = new SchedulerTask(scheduler, logger, "MQTT Connection", this::establishMQTTConnection);
    }

    public String getToken() {
        return token;
    }

    public ThingUID getUID() {
        return thing.getUID();
    }

    public Login doLogin() {
        try {
            Login login = webTargets.doLogin(baseUri, config.email, config.password);
            if (login != null) {
                return login;
            }
        } catch (RoborockAuthenticationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Authentication error " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "NoSuchAlgorithmException error " + e.getMessage());
        } catch (RoborockCommunicationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Communication error " + e.getMessage());
        }
        return new Login();
    }

    @Nullable
    public Home getHomeDetail() {
        try {
            return webTargets.getHomeDetail(baseUri, token);
        } catch (RoborockAuthenticationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Authentication error " + e.getMessage());
            return new Home();
        } catch (RoborockCommunicationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Communication error " + e.getMessage());
            return new Home();
        }
    }

    @Nullable
    public HomeData getHomeData(String rrHomeId) {
        try {
            return webTargets.getHomeData(rrHomeId, rriot);
        } catch (RoborockAuthenticationException | NoSuchAlgorithmException | InvalidKeyException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Authentication error " + e.getMessage());
            return new HomeData();
        } catch (RoborockCommunicationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Communication error " + e.getMessage());
            return new HomeData();
        }
    }

    @Nullable
    public String getRoutines(String deviceId) {
        if (rriot != null) {
            try {
                return webTargets.getRoutines(deviceId, rriot);
            } catch (RoborockAuthenticationException | NoSuchAlgorithmException | InvalidKeyException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Authentication error " + e.getMessage());
                return "";
            } catch (RoborockCommunicationException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Communication error " + e.getMessage());
                return "";
            }
        } else {
            return "";
        }
    }

    @Nullable
    public String setRoutine(String sceneID) {
        try {
            return webTargets.setRoutine(sceneID, rriot);
        } catch (RoborockAuthenticationException | NoSuchAlgorithmException | InvalidKeyException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Authentication error " + e.getMessage());
            return "";
        } catch (RoborockCommunicationException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Communication error " + e.getMessage());
            return "";
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // we do not have any channels -> nothing to do here
    }

    @Override
    public void initialize() {
        config = getConfigAs(RoborockAccountConfiguration.class);
        if (config == null || config.email == null || config.email.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Missing email address configuration");
            return;
        }
        updateStatus(ThingStatus.UNKNOWN);
        initTask.setNamePrefix(getThing().getUID().getId());
        mqttConnectTask.setNamePrefix(getThing().getUID().getId());
        initTask.submit();
    }

    @Override
    public void handleRemoval() {
        teardown(false);
        super.handleRemoval();
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        logger.debug("Child registered with gateway: {}  {} -> {} {}", childThing.getUID(), childThing.getLabel(),
                getThing().getUID(), getThing().getLabel());
        if (childHandler instanceof RoborockVacuumHandler) {
            childDevices.put(childThing, (RoborockVacuumHandler) childHandler);
        } else {
            logger.warn("Initialized child handler is not a RoborockVacuumHandler: {}",
                    childHandler.getClass().getName());
        }
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        logger.debug("Child released from gateway: {}  {} -> {} {}", childThing.getUID(), childThing.getLabel(),
                getThing().getUID(), getThing().getLabel());
        childDevices.remove(childThing);
    }

    private void initAPI() {
        if (baseUri.isEmpty()) {
            try {
                baseUri = webTargets.getUrlByEmail(config.email);
            } catch (RoborockAuthenticationException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Authentication error " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "NoSuchAlgorithmException error " + e.getMessage());
            } catch (RoborockCommunicationException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Communication error " + e.getMessage());
            }
        }
        String sessionStoreToken = sessionStorage.get("token");
        String sessionStoreRriot = sessionStorage.get("rriot");

        if (!(sessionStoreToken == null) && !(sessionStoreRriot == null)) {
            logger.trace("Retrieved token and rriot values from sessionStorage");
            token = sessionStoreToken;
            @Nullable
            Rriot rriotTemp = gson.fromJson(sessionStoreRriot, Rriot.class);
            if (rriotTemp != null) {
                rriot = rriotTemp;
            }
        } else {
            logger.trace("No available token or rriot values from sessionStorage, logging in");
            Login loginResponse = doLogin();
            if (loginResponse.code.equals("200")) {
                sessionStorage.put("token", loginResponse.data.token);
                sessionStorage.put("rriot", gson.toJson(loginResponse.data.rriot));
                token = loginResponse.data.token;
                rriot = loginResponse.data.rriot;
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Error code " + loginResponse.code + " reported");
            }
        }
        updateStatus(ThingStatus.ONLINE);
        mqttConnectTask.submit();
    }

    private void teardownAndScheduleReconnection() {
        teardown(true);
    }

    private synchronized void teardown(boolean scheduleReconnection) {
        disconnectMqttClient();

        mqttConnectTask.cancel();
        initTask.cancel();

        if (scheduleReconnection) {
            initTask.submit();
        }
    }

    private void establishMQTTConnection() {
        if (token.isEmpty() || rriot.r == null || rriot.r.m.isEmpty() || rriot.k.isEmpty() || rriot.s.isEmpty()
                || rriot.u.isEmpty()) {
            logger.trace("token and/or rriot are empty, delay connection to MQTT server");
            return;
        }

        try {
            connectMqttClient();
            logger.debug("Bridge connected to MQTT");
            updateStatus(ThingStatus.ONLINE);
        } catch (MqttException e) {
            logger.debug("Failed to connect to MQTT: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "MQTT Connection failed: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        teardown(false);
    }

    public void connectMqttClient() throws MqttException {
        try {
            URI mqttURL = new URI(rriot.r.m);
            String mqttUser = ProtocolUtils.md5Hex(rriot.u + ':' + rriot.k).substring(2, 10);
            String mqttPassword = ProtocolUtils.md5Hex(rriot.s + ':' + rriot.k).substring(16);

            String serverURI = "ssl://" + mqttURL.getHost() + ":" + mqttURL.getPort();
            String clientId = mqttUser;

            mqttClient = new MqttClient(serverURI, clientId, new MemoryPersistence());
            mqttClient.setCallback(this); // Set this class to handle all callbacks

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(false);
            connOpts.setUserName(mqttUser);
            connOpts.setPassword(mqttPassword.toCharArray());
            connOpts.setAutomaticReconnect(true); // Use Paho's automatic reconnect feature
            connOpts.setConnectionTimeout(60);
            connOpts.setKeepAliveInterval(0);

            logger.debug("Connecting to MQTT broker at {}", serverURI);
            mqttClient.connect(connOpts);
            logger.debug("Established MQTT connection.");
        } catch (URISyntaxException e) {
            logger.error("Malformed MQTT URL", e);
            throw new MqttException(e);
        } catch (MqttException e) {
            logger.error("MQTT connection or subscription failed", e);
            // Handle specific authentication failures to trigger re-login
            if (e.getReasonCode() == MqttException.REASON_CODE_FAILED_AUTHENTICATION) {
                logger.warn("MQTT Authorization failed. Clearing session token and forcing re-login.");
                // sessionStorage.put("token", null);
                // sessionStorage.put("rriot", null);
                // token = "";
                // teardownAndScheduleReconnection();
            }
            throw e;
        }
    }

    @Override
    public void connectComplete(boolean reconnect, @Nullable String serverURI) {
        logger.debug("MQTT connection established. Reconnect: {}, Server URI: {}", reconnect, serverURI);

        // Subscribe to topics after a successful connection
        try {
            String mqttUser = ProtocolUtils.md5Hex(rriot.u + ':' + rriot.k).substring(2, 10);
            String topic = "rr/m/o/" + rriot.u + "/" + mqttUser + "/#";
            mqttClient.subscribe(topic, 0);
            logger.debug("Subscribed to topic {}", topic);
            updateStatus(ThingStatus.ONLINE); // Update status to ONLINE here
        } catch (MqttException e) {
            logger.error("Failed to subscribe to MQTT topic after connection complete", e);
        }
    }

    @Override
    public void connectionLost(@Nullable Throwable cause) {
        logger.warn("MQTT connection lost: {}. Automatic reconnect is enabled.", cause.getMessage());
        // Additional logic can be placed here if specific actions are needed on disconnect
    }

    @Override
    public void messageArrived(@Nullable String topic, @Nullable MqttMessage message) throws Exception {
        byte[] payload = message.getPayload();
        if (payload == null || payload.length == 0) {
            logger.debug("Empty payload received on topic {}", topic);
            return;
        }

        String destination = topic.substring(topic.lastIndexOf('/') + 1);
        logger.debug("Received MQTT message for device {}", destination);

        // Check list of child handlers and send message to the right one
        for (Entry<Thing, RoborockVacuumHandler> entry : childDevices.entrySet()) {
            if (entry.getKey().getUID().getAsString().contains(destination)) {
                try {
                    logger.trace("Submit response to child {} -> {}", destination, entry.getKey().getUID());
                    entry.getValue().handleMessage(payload);
                } catch (RuntimeException e) {
                    logger.debug(
                            "Unhandled exception processing MQTT message for device {}. Message will be discarded.",
                            destination, e);
                }
                logger.trace("MQTT message processed - from AccountHandler");
                return;
            }
        }
        logger.warn("Received MQTT message for unknown device destination: {}", destination);
    }

    @Override
    public void deliveryComplete(@Nullable IMqttDeliveryToken token) {
        logger.trace("MQTT message delivery complete.");
    }

    public void disconnectMqttClient() {
        if (mqttClient != null) {
            try {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                mqttClient.close();
                logger.debug("MQTT client disconnected and closed.");
            } catch (MqttException e) {
                logger.error("Error while disconnecting MQTT client.", e);
            }
        }
        this.mqttClient = null;
    }

    private String getEndpoint() {
        try {
            byte[] md5Bytes = MessageDigest.getInstance("MD5").digest(rriot.k.getBytes());
            byte[] subArray = Arrays.copyOfRange(md5Bytes, 8, 14);
            return Base64.getEncoder().encodeToString(subArray);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public int sendCommand(String method, String params, String thingID, String localKey, byte[] nonce)
            throws UnsupportedEncodingException {
        int timestamp = (int) Instant.now().getEpochSecond();
        int protocol = 101;
        int id = secureRandom.nextInt(22767 + 1) + 10000;

        String nonceHex = bytesToHex(nonce);

        Map<String, Object> security = new HashMap<>();
        security.put("endpoint", getEndpoint());
        security.put("nonce", nonceHex.toLowerCase());

        JsonElement paramsElement = JsonParser.parseString(params);
        Map<String, Object> inner = new HashMap<>();
        inner.put("id", id);
        inner.put("method", method);
        inner.put("params", paramsElement);
        inner.put("security", security);

        Map<String, Object> dps = new HashMap<>();
        dps.put(Integer.toString(protocol), gson.toJson(inner));

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("t", timestamp);
        payloadMap.put("dps", dps);

        String payload = gson.toJson(payloadMap);
        logger.trace("MQTT payload = {}", payload);

        byte[] messageBytes = build(thingID, localKey, protocol, timestamp, payload.getBytes(StandardCharsets.UTF_8));
        // now send message via mqtt
        String mqttUser = ProtocolUtils.md5Hex(rriot.u + ':' + rriot.k).substring(2, 10);

        String topic = "rr/m/i/" + rriot.u + "/" + mqttUser + "/" + thingID;
        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            logger.debug("Publishing {} message to {}", method, topic);
            try {
                MqttMessage message = new MqttMessage(messageBytes);
                message.setQos(0);
                message.setRetained(false);
                mqttClient.publish(topic, message);
                return id;
            } catch (MqttException e) {
                logger.error("MQTT publish failed", e);
                return -1;
            }
        } else {
            logger.debug("Failed to publish {} message to {}, this.mqttClient == null", method, topic);
            return -1;
        }
    }

    /**
     * Converts a byte array to its hexadecimal string representation.
     *
     * @param bytes The byte array to convert.
     * @return The hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    byte[] build(String thingID, String localKey, int protocol, int timestamp, byte[] payload) {
        try {
            String key = ProtocolUtils.encodeTimestamp(timestamp) + localKey + SALT;
            byte[] encryptedPayload = ProtocolUtils.encrypt(payload, key);

            int randomInt = secureRandom.nextInt(90000) + 10000;
            int seq = secureRandom.nextInt(900000) + 100000;

            int totalLength = 19 + encryptedPayload.length + 4;
            byte[] message = new byte[totalLength];

            // Write fixed string '1.0'
            message[0] = '1';
            message[1] = '.';
            message[2] = '0';

            // Write integer fields
            ProtocolUtils.writeInt32BE(message, seq, 3);
            ProtocolUtils.writeInt32BE(message, randomInt, 7);
            ProtocolUtils.writeInt32BE(message, timestamp, 11);
            ProtocolUtils.writeInt16BE(message, protocol, 15);
            ProtocolUtils.writeInt16BE(message, encryptedPayload.length, 17);

            // Copy encrypted payload
            System.arraycopy(encryptedPayload, 0, message, 19, encryptedPayload.length);

            // Calculate CRC32 and write to the end
            CRC32 crc32 = new CRC32();
            crc32.update(message, 0, message.length - 4); // Calculate CRC for all bytes except the last 4 (CRC field
                                                          // itself)
            ProtocolUtils.writeInt32BE(message, (int) crc32.getValue(), message.length - 4);
            return message;
        } catch (Exception e) {
            logger.debug("Exception encrypting payload, {}", e.getMessage());
            return new byte[0];
        }
    }

    public void onEventStreamFailure(Throwable error) {
        logger.debug("Device connection failed, reconnecting", error);
        mqttConnectTask.schedule(60);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(RoborockVacuumDiscoveryService.class);
    }
}
