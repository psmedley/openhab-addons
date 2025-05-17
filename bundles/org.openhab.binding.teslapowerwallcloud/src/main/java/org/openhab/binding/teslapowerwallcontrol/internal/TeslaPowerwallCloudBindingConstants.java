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

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link TeslaPowerwallCloudBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Paul Smedley - Initial contribution
 */
@NonNullByDefault
public class TeslaPowerwallCloudBindingConstants {

    private static final String BINDING_ID = "teslapowerwallcloud";

    // List of all Thing Type UIDs
    public static final ThingTypeUID POWERWALL_THING = new ThingTypeUID(BINDING_ID, "powerwallcloud");

    // List of all Channel ids
    public static final String CHANNEL_POWERWALLCLOUD_SOLAR_POWER = "solar-power";
    public static final String CHANNEL_POWERWALLCLOUD_HOME_POWER = "home-power";
    public static final String CHANNEL_POWERWALLCLOUD_GRID_POWER = "grid-power";
    public static final String CHANNEL_POWERWALLCLOUD_BATTERY_POWER = "battery-power";
    public static final String CHANNEL_POWERWALLCLOUD_POWERWALL_MODE = "powerwall-mode";
    public static final String CHANNEL_POWERWALLCLOUD_STORM_MODE = "storm-mode";
    public static final String CHANNEL_POWERWALLCLOUD_GRID_STATUS = "grid-status";
    public static final String CHANNEL_POWERWALLCLOUD_PERCENT_CHARGED = "percent-charged";
    public static final String CHANNEL_POWERWALLCLOUD_BATTERY_RESERVE = "battery-reserve";
    public static final String CHANNEL_POWERWALLCLOUD_SITE_NAME = "site-name";
    public static final String CHANNEL_POWERWALLCLOUD_VERSION = "version";
    public static final String CHANNEL_POWERWALLCLOUD_ISLAND_STATUS = "island-status";
    public static final String CHANNEL_POWERWALLCLOUD_STORM_MODE_ACTIVE = "storm-mode-active";

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(POWERWALL_THING);
}
