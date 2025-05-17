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
package org.openhab.binding.teslapowerwallcloud.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Class for holding the set of parameters used to read the Site Info.
 *
 * @author Paul Smedley - Initial Contribution
 *
 */
@NonNullByDefault
public class LiveStatus {
    private static Logger LOGGER = LoggerFactory.getLogger(LiveStatus.class);

    public double solar_power;
    public double battery_power;
    public double load_power;
    public double grid_power;
    public float percentage_charged;
    public String grid_status = "";
    public String island_status = "";
    public String storm_mode_active = "";

    private LiveStatus() {
    }

    public static LiveStatus parse(String response) {
        LOGGER.debug("Parsing string: \"{}\"", response);
        /* parse json string */
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
        JsonObject jsonResponse = jsonObject.getAsJsonObject("response");
        LiveStatus info = new LiveStatus();
        info.solar_power = jsonResponse.get("solar_power").getAsDouble() / 1000;
        info.battery_power = jsonResponse.get("battery_power").getAsDouble() / 1000;
        info.load_power = jsonResponse.get("load_power").getAsDouble() / 1000;
        info.grid_power = jsonResponse.get("grid_power").getAsDouble() / 1000;
        info.percentage_charged = jsonResponse.get("percentage_charged").getAsFloat();
        info.grid_status = jsonResponse.get("grid_status").getAsString();
        info.island_status = jsonResponse.get("island_status").getAsString();
        info.storm_mode_active = jsonResponse.get("storm_mode_active").getAsString();
        return info;
    }
}
