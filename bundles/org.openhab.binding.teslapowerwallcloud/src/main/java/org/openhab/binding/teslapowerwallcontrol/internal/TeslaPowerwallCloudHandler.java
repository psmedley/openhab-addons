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
package org.openhab.binding.teslapowerwallcloud.internal;

import static org.openhab.binding.teslapowerwallcloud.internal.TeslaPowerwallCloudBindingConstants.*;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.teslapowerwallcloud.internal.api.LiveStatus;
import org.openhab.binding.teslapowerwallcloud.internal.api.SiteInfo;
import org.openhab.binding.teslapowerwallcloud.internal.api.TokenResponse;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TeslaPowerwallCloudHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Paul Smedley - Initial contribution
 */
@NonNullByDefault
public class TeslaPowerwallCloudHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(TeslaPowerwallCloudHandler.class);

    private @Nullable TeslaPowerwallCloudConfiguration config;

    private long refreshInterval;

    private String accessToken = "";
    private String refreshToken = "";
    private String clientID = "";
    private String siteID = "";

    private long tokenExpiry;

    private @NonNullByDefault({}) TeslaPowerwallCloudWebTargets webTargets;
    private @Nullable ScheduledFuture<?> pollFuture;

    public TeslaPowerwallCloudHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            if (CHANNEL_POWERWALLCLOUD_POWERWALL_MODE.equals(channelUID.getId())) {
                logger.debug("Setting operating mode to: {}", command);
                webTargets.setOperatingMode(accessToken, siteID, command);

                // Note: if communication with thing fails for some reason,
                // indicate that by setting the status with detail information:
                // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                // "Could not control device at IP address x.x.x.x");
            }
            if (CHANNEL_POWERWALLCLOUD_BATTERY_RESERVE.equals(channelUID.getId())) {
                logger.debug("Setting reserve to: {}", command);
                webTargets.setReserve(accessToken, siteID, command);

                // Note: if communication with thing fails for some reason,
                // indicate that by setting the status with detail information:
                // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                // "Could not control device at IP address x.x.x.x");
            }
            if (CHANNEL_POWERWALLCLOUD_STORM_MODE.equals(channelUID.getId())) {
                if (command instanceof OnOffType) {
                    logger.debug("Setting storm mode to: {}", command);
                    if (command == OnOffType.ON) {
                        webTargets.setStormMode(accessToken, siteID, "true");
                    } else {
                        webTargets.setStormMode(accessToken, siteID, "false");
                    }
                }
                // Note: if communication with thing fails for some reason,
                // indicate that by setting the status with detail information:
                // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                // "Could not control device at IP address x.x.x.x");
            }
        } catch (TeslaPowerwallCloudCommunicationException ex) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
        }
    }

    @Override
    public void initialize() {
        TeslaPowerwallCloudConfiguration config = getConfigAs(TeslaPowerwallCloudConfiguration.class);
        logger.debug("config.siteID = {}, refresh = {}", config.siteID, config.refreshInterval);
        if (config.refreshToken == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "SSO Refresh Token must be set");
        } else {
            webTargets = new TeslaPowerwallCloudWebTargets();
            refreshInterval = config.refreshInterval;
            siteID = config.siteID;
            clientID = config.clientID;
            schedulePoll();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        stopPoll();
    }

    private void schedulePoll() {
        if (pollFuture != null) {
            pollFuture.cancel(false);
        }
        logger.debug("Scheduling poll for 1 second out, then every {} s", refreshInterval);
        pollFuture = scheduler.scheduleWithFixedDelay(this::poll, 1, refreshInterval, TimeUnit.SECONDS);
    }

    private void poll() {
        try {
            logger.debug("Polling for state");
            pollStatus();
        } catch (IOException e) {
            logger.debug("Could not connect to Tesla API", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (RuntimeException e) {
            logger.warn("Unexpected error connecting to Tesla API", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void stopPoll() {
        final Future<?> future = pollFuture;
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
            pollFuture = null;
        }
    }

    private void pollStatus() throws IOException {
        TeslaPowerwallCloudConfiguration config = getConfigAs(TeslaPowerwallCloudConfiguration.class);
        if (java.time.Instant.now().getEpochSecond() >= tokenExpiry) {
            logger.debug("accessToken will expire at {},  which is in < 30 min, renewing", tokenExpiry);
            TokenResponse tokenResponse = webTargets.generateAccessToken(config.refreshToken, config.clientID);
            if (tokenResponse != null) {
                accessToken = tokenResponse.accessToken;
                Configuration configuration = editConfiguration();
                configuration.put("refreshToken", tokenResponse.refreshToken);
                updateConfiguration(configuration);
                tokenExpiry = java.time.Instant.now().getEpochSecond() + tokenResponse.expiresIn - 1000;
            }
        }

        if (siteID.isEmpty()) {
            siteID = webTargets.getSiteID(accessToken);
            logger.debug("Detected energy_site_id is {}", siteID);
        }
        SiteInfo siteInfo = webTargets.getSiteInfo(accessToken, siteID);
        LiveStatus liveStatus = webTargets.getLiveStatus(accessToken, siteID);
        updateStatus(ThingStatus.ONLINE);

        if (siteInfo != null) {
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_POWERWALL_MODE,
                    new StringType(siteInfo.defaultRealMode));
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_SITE_NAME,
                    new StringType(siteInfo.siteName));
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_VERSION,
                    new StringType(siteInfo.version));
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_BATTERY_RESERVE,
                    new QuantityType<>(siteInfo.reserve, Units.PERCENT));
            switch (siteInfo.stormModeEnabled) {
                case "true":
                    updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_STORM_MODE, OnOffType.ON);
                    break;
                case "false":
                    updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_STORM_MODE, OnOffType.OFF);
                    break;
            }
        }

        if (liveStatus != null) {
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_SOLAR_POWER,
                    new QuantityType<>(liveStatus.solarPower, MetricPrefix.KILO(Units.WATT)));
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_HOME_POWER,
                    new QuantityType<>(liveStatus.loadPower, MetricPrefix.KILO(Units.WATT)));
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_GRID_POWER,
                    new QuantityType<>(liveStatus.gridPower, MetricPrefix.KILO(Units.WATT)));
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_BATTERY_POWER,
                    new QuantityType<>(liveStatus.batteryPower, MetricPrefix.KILO(Units.WATT)));
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_PERCENT_CHARGED,
                    new QuantityType<>(liveStatus.percentageCharged, Units.PERCENT));
            switch (liveStatus.gridStatus) {
                case "Active":
                    updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_GRID_STATUS, OnOffType.ON);
                    break;
                case "Inactive":
                    updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_GRID_STATUS, OnOffType.OFF);
                    break;
            }
            updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_ISLAND_STATUS,
                    new StringType(liveStatus.islandStatus));
            switch (liveStatus.stormModeActive) {
                case "true":
                    updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_STORM_MODE_ACTIVE,
                            OnOffType.ON);
                    break;
                case "false":
                    updateState(TeslaPowerwallCloudBindingConstants.CHANNEL_POWERWALLCLOUD_STORM_MODE_ACTIVE,
                            OnOffType.OFF);
                    break;
            }
        }
    }
}
