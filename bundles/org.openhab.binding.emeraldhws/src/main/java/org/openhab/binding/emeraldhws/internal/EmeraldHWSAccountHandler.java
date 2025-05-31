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
package org.openhab.binding.emeraldhws.internal;

import static org.openhab.binding.emeraldhws.internal.EmeraldHWSBindingConstants.*;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.emeraldhws.internal.api.List;
import org.openhab.binding.emeraldhws.internal.api.Login;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link EmeraldHWSHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author paul@smedley.id.au - Initial contribution
 */
@NonNullByDefault
public class EmeraldHWSAccountHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(EmeraldHWSAccountHandler.class);

    private @Nullable EmeraldHWSConfiguration config;
    protected ScheduledExecutorService executorService = this.scheduler;
    private @Nullable ScheduledFuture<?> pollingJob;
    private @NonNullByDefault({}) EmeraldHWSWebTargets webTargets;
    private HttpClient httpClient = new HttpClient();

    private final Gson gson = new Gson();

    String token = "";

    public EmeraldHWSAccountHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        config = getConfigAs(EmeraldHWSConfiguration.class);
        webTargets = new EmeraldHWSWebTargets(httpClient);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // we do not have any channels -> nothing to do here
    }

    @Override
    public void initialize() {
        config = getConfigAs(EmeraldHWSConfiguration.class);

        if (configure()) {
            updateStatus(ThingStatus.UNKNOWN);
            pollingJob = executorService.scheduleWithFixedDelay(this::pollingCode, 0, config.refreshInterval,
                    TimeUnit.SECONDS);
        }

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });

        // These logging types should be primarily used by bindings
        // logger.trace("Example trace message");
        // logger.debug("Example debug message");
        // logger.warn("Example warn message");
        //
        // Logging to INFO should be avoided normally.
        // See https://www.openhab.org/docs/developer/guidelines.html#f-logging

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    /**
     * Check the current configuration
     *
     * @return true if the configuration is ok to start polling, false otherwise
     */
    private boolean configure() {
        if (config.email.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Missing email configuration");
            return false;
        }
        if (config.password.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Missing password configuration");
            return false;
        }
        return true;
    }

    protected void pollData() {
        try {
            if ("".equals(token)) {
                Login loginResponse;
                loginResponse = webTargets.getToken(config.email, config.password);
                if (loginResponse != null) {
                    token = loginResponse.token;
                }
            }
            List listResponse;
            listResponse = webTargets.getList(config.email, config.password);
            for (int i = 0; i < listResponse.info.property.length; i++) {
                for (int j = 0; j < listResponse.info.property[i].heatpump.length; j++) {
                    logger.info("Found Heat Pump id = {}", listResponse.info.property[i].heatpump[j].id);
                }
            }
            updateStatus(ThingStatus.ONLINE);
        } catch (EmeraldHWSAuthenticationException e) {
            logger.debug("Unexpected authentication error connecting to Emerald API", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            return;
        } catch (EmeraldHWSCommunicationException e) {
            logger.debug("Unexpected error connecting to Emerald API", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            return;
        }
    }

    /**
     * The actual polling loop
     */
    protected void pollingCode() {
        pollData();
    }
}
