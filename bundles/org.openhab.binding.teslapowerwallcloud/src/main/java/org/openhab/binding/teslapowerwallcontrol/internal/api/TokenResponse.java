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

public class TokenResponse {
    private static Logger LOGGER = LoggerFactory.getLogger(TokenResponse.class);

    public String access_token = "";
    public String token_type = "";
    public Long expires_in;
    public String refresh_token = "";

    private TokenResponse() {
    }

    public static TokenResponse parse(String response) {
        LOGGER.debug("Parsing string: \"{}\"", response);
        /* parse json string */
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.access_token = jsonObject.get("access_token").getAsString();
        tokenResponse.token_type = jsonObject.get("token_type").getAsString();
        tokenResponse.expires_in = jsonObject.get("expires_in").getAsLong();
        tokenResponse.refresh_token = jsonObject.get("refresh_token").getAsString();
        return tokenResponse;
    }
}
