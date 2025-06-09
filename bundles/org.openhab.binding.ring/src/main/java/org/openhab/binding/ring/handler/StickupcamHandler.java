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
package org.openhab.binding.ring.handler;

import static org.openhab.binding.ring.RingBindingConstants.*;

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.ring.internal.RestClient;
import org.openhab.binding.ring.internal.RingDeviceRegistry;
import org.openhab.binding.ring.internal.data.Profile;
import org.openhab.binding.ring.internal.data.RingDeviceTO;
import org.openhab.binding.ring.internal.data.RingEventTO;
import org.openhab.binding.ring.internal.data.Stickupcam;
import org.openhab.binding.ring.internal.errors.AuthenticationException;
import org.openhab.binding.ring.internal.errors.DeviceNotFoundException;
import org.openhab.binding.ring.internal.errors.IllegalDeviceClassException;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * The handler for a Ring Video Stickup Cam.
 *
 * @author Chris Milbert - Initial contribution
 * @author Ben Rosenblum - Updated for OH4 / New Maintainer
 *
 */

@NonNullByDefault
public class StickupcamHandler extends RingDeviceHandler {
    @Nullable
    AccountHandler bridgeHandler;

    private int lastBattery = -1;

    private Profile userProfile = new Profile();

    /**
     * The list with events.
     */
    private List<RingEventTO> lastEvents = List.of();
    /**
     * The index to the current event.
     */
    private int eventIndex = 0;

    public StickupcamHandler(Thing thing, Gson gson) {
        super(thing, gson);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Stickupcam handler");
        super.initialize();

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No Ring Bridge thing selected");
            return;
        }
        bridgeHandler = (AccountHandler) bridge.getHandler();

        RingDeviceRegistry registry = RingDeviceRegistry.getInstance();
        String id = getThing().getUID().getId();
        if (registry.isInitialized()) {
            try {
                linkDevice(id, Stickupcam.class);
                updateStatus(ThingStatus.ONLINE);
            } catch (DeviceNotFoundException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Device with id '" + id + "' not found");
            } catch (IllegalDeviceClassException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Device with id '" + id + "' of wrong type");
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                    "Waiting for RingAccount to initialize");
        }

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
        if (this.refreshJob == null) {
            Configuration config = getThing().getConfiguration();
            int refreshInterval = ConfigParser
                    .valueAsOrElse(config.get("refreshInterval"), BigDecimal.class, BigDecimal.valueOf(500)).intValue();
            startAutomaticRefresh(refreshInterval);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Do Nothing
    }

    @Override
    protected void refreshState() {
        // Do Nothing
    }

    @Override
    protected void minuteTick() {
        logger.debug("StickupcamHandler - minuteTick - device {}", getThing().getUID().getId());
        if (device == null) {
            initialize();
        }
        RingDeviceTO deviceTO = gson.fromJson(device.getJsonObject(), RingDeviceTO.class);
        if ((deviceTO != null) && (deviceTO.health.batteryPercentage != lastBattery)) {
            logger.debug("Battery Level: {}", deviceTO.battery);
            ChannelUID channelUID = new ChannelUID(thing.getUID(), CHANNEL_STATUS_BATTERY);
            updateState(channelUID, new DecimalType(deviceTO.health.batteryPercentage));
            lastBattery = deviceTO.health.batteryPercentage;
        } else if (deviceTO != null) {
            logger.debug("Battery Level Unchanged for {} - {} vs {}", getThing().getUID().getId(),
                    deviceTO.health.batteryPercentage, lastBattery);

        }
        eventTick();
    }

    protected Profile getProfile() {
        AccountHandler localBridge = bridgeHandler;
        if (localBridge == null) {
            return new Profile();
        }
        try {
            return localBridge.getProfile();
        } catch (IllegalStateException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, e.getMessage());
            return new Profile();
        }
    }

    protected @Nullable RestClient getRestClient() {
        AccountHandler localBridge = bridgeHandler;
        if (localBridge == null) {
            return new RestClient();
        }
        try {
            return localBridge.getRestClient();
        } catch (IllegalStateException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, e.getMessage());
            return new RestClient();
        }
    }

    protected void eventTick() {
        try {
            long id = lastEvents.isEmpty() ? 0 : lastEvents.get(0).id;
            userProfile = getProfile();
            RestClient restClient = getRestClient();
            lastEvents = restClient.getDeviceHistory(userProfile, getThing().getUID().getId(), 1);
            if (!lastEvents.isEmpty()) {
                logger.debug("StickupcamHandler - eventTick - Event id: {} lastEvents: {}", id,
                        lastEvents.get(0).id == id);
                if (lastEvents.get(0).id != id) {
                    updateState(new ChannelUID(thing.getUID(), CHANNEL_STATUS_CREATED_AT),
                            lastEvents.get(0).getCreatedAt());
                    updateState(new ChannelUID(thing.getUID(), CHANNEL_STATUS_KIND),
                            new StringType(lastEvents.get(0).kind));
                    // runnableVideo = () -> getVideo(lastEvents.get(0));
                    // ExecutorService service = videoExecutorService;
                    // if (service != null) {
                    // service.submit(runnableVideo);
                    // }
                }
            } else {
                logger.debug("StickupcamHandler - eventTick - lastEvents null");
            }
        } catch (AuthenticationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "AuthenticationException response from ring.com");
            logger.debug(
                    "RestClient reported AuthenticationExceptionfrom api.ring.com when retrying refreshRegistry for the second time: {}",
                    ex.getMessage());
        } catch (JsonParseException ignored) {
            logger.debug(
                    "RestClient reported JsonParseException api.ring.com when retrying refreshRegistry for the second time: {}",
                    ignored.getMessage());

        }
    }
}
