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
public class SiteInfo {
    private static Logger LOGGER = LoggerFactory.getLogger(SiteInfo.class);

    public int reserve;
    public String default_real_mode = "";
    public String site_name = "";
    public String storm_mode_enabled = "";
    public String version = "";

    private SiteInfo() {
    }

    public static SiteInfo parse(String response) {
        LOGGER.debug("Parsing string: \"{}\"", response);
        /* parse json string */
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
        JsonObject jsonResponse = jsonObject.getAsJsonObject("response");
        JsonObject jsonUserSettings = jsonResponse.getAsJsonObject("user_settings");
        SiteInfo info = new SiteInfo();
        info.reserve = jsonResponse.get("backup_reserve_percent").getAsInt();
        info.default_real_mode = jsonResponse.get("default_real_mode").getAsString();
        info.site_name = jsonResponse.get("site_name").getAsString();
        info.storm_mode_enabled = jsonUserSettings.get("storm_mode_enabled").getAsString();
        info.version = jsonResponse.get("version").getAsString();
        return info;
    }
}
