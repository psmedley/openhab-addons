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

import com.google.gson.annotations.SerializedName;

/**
 * Class for holding the set of parameters used to read the Site Info.
 *
 * @author Paul Smedley - Initial Contribution
 *
 */
@NonNullByDefault
public class LiveStatus {
    @SerializedName("response")
    public @NonNullByDefault({}) LiveStatusResponse liveStatusResponse;

    public class LiveStatusResponse {

        @SerializedName("solar_power")
        public double solarPower;

        @SerializedName("battery_power")
        public double batteryPower;

        @SerializedName("load_power")
        public double loadPower;

        @SerializedName("grid_power")
        public double gridPower;

        @SerializedName("percentage_charged")
        public float percentageCharged;

        @SerializedName("grid_status")
        public String gridStatus = "";

        @SerializedName("island_power")
        public String islandStatus = "";

        @SerializedName("storm_mode_active")
        public boolean stormModeActive;
    }

    private LiveStatus() {
    }
}
