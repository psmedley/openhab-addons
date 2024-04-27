/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openhab.binding.teslapowerwallcloud.internal.api.LiveStatus;
import org.openhab.binding.teslapowerwallcloud.internal.api.SiteInfo;
import org.openhab.core.io.net.http.HttpUtil;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Handles performing the actual HTTP requests for communicating with the Tesla API.
 *
 * @author Paul Smedley - Initial Contribution
 *
 */

public class TeslaPowerwallCloudWebTargets {
    private static final int TIMEOUT_MS = 30000;

    private String getLiveStatusUri = "";
    private String getSiteInfoUri = "";
    private final Logger logger = LoggerFactory.getLogger(TeslaPowerwallCloudWebTargets.class);

    public TeslaPowerwallCloudWebTargets(String accessToken, String siteID) {
        String baseUri = "https://fleet-api.prd.na.vn.cloud.tesla.com/api/1/energy_sites/" + siteID;
        getLiveStatusUri = baseUri + "/live_status";
        getSiteInfoUri = baseUri + "/site_info";
    }

    public String getSiteID(String accessToken) throws TeslaPowerwallCloudCommunicationException {
        String response = invoke("GET", "https://fleet-api.prd.na.vn.cloud.tesla.com/api/1/products", accessToken);
        logger.debug("Product List response = {}", response);
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
        JsonArray jsonResponse = jsonObject.getAsJsonArray("response");
        String siteID = "";
        int i = 0;
        while (siteID.isEmpty()) {
            jsonObject = jsonResponse.get(i).getAsJsonObject();
            if (jsonObject.has("energy_site_id")) {
                siteID = jsonObject.get("energy_site_id").getAsString();
            }
            i++;
        }
        logger.debug("Selected siteID is = {}", siteID);
        return siteID;
    }

    public String[] generateAccessToken(String refreshToken, String client_id) {
        String accessToken = "";
        String newRefreshToken = "";
        String response;
        String payload = "{\"grant_type\":\"refresh_token\",\"client_id\":\"" + client_id + "\",\"refresh_token\":\""
                + refreshToken + "\"}";
        Properties httpHeaders = new Properties();
        httpHeaders.put("Content-Type", "application/json");
        logger.debug("payload = {}", payload);
        ByteArrayInputStream input = new ByteArrayInputStream(payload.getBytes());
        try {
            response = HttpUtil.executeUrl("POST", "https://auth.tesla.com/oauth2/v3/token", httpHeaders, input,
                    "application/text", TIMEOUT_MS);
        } catch (IOException ex) {
            logger.debug("Exception during generateAccessToken{}", ex.getLocalizedMessage(), ex);
            // Response will also be set to null if parsing in executeUrl fails so we use null here to make the
            // error check below consistent.
            response = null;
        }
        logger.debug("response = {}", response);

        if (!(response == null)) {
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            accessToken = jsonObject.get("access_token").getAsString();
            newRefreshToken = jsonObject.get("refresh_token").getAsString();
        }
        logger.debug("accessToken = {}", accessToken);
        logger.debug("newRefreshToken = {}", newRefreshToken);

        return new String[] { accessToken, newRefreshToken };
    }

    public SiteInfo getSiteInfo(String accessToken, String siteID) throws TeslaPowerwallCloudCommunicationException {
        String response = invoke("GET",
                "https://fleet-api.prd.na.vn.cloud.tesla.com/api/1/energy_sites/" + siteID + "/site_info", accessToken);
        return SiteInfo.parse(response);
    }

    public LiveStatus getLiveStatus(String accessToken, String siteID)
            throws TeslaPowerwallCloudCommunicationException {
        String response = invoke("GET",
                "https://fleet-api.prd.na.vn.cloud.tesla.com/api/1/energy_sites/" + siteID + "/live_status",
                accessToken);
        return LiveStatus.parse(response);
    }

    public String setOperatingMode(String accessToken, String proxyAddress, String siteID, Command newMode)
            throws TeslaPowerwallCloudCommunicationException {

        String payload = "{\"default_real_mode\":\"" + newMode + "\"}";

        logger.debug("payload = {}", payload);
        ByteArrayInputStream input = new ByteArrayInputStream(payload.getBytes());
        String response = invoke("POST", proxyAddress + "/api/1/energy_sites/" + siteID + "/operation", accessToken,
                input, "application/json");
        logger.debug("response to Operating Mode change = {}", response);
        return response;
    }

    public String setReserve(String accessToken, String proxyAddress, String siteID, Command newReserve)
            throws TeslaPowerwallCloudCommunicationException {

        String payload = "{\"backup_reserve_percent\":" + newReserve + "}";

        logger.debug("payload = {}", payload);
        ByteArrayInputStream input = new ByteArrayInputStream(payload.getBytes());
        String response = invoke("POST", proxyAddress + "/api/1/energy_sites/" + siteID + "/backup", accessToken, input,
                "application/json");
        logger.debug("response to reserve change = {}", response);
        return response;
    }

    public String setStormMode(String accessToken, String proxyAddress, String siteID, String newStormMode)
            throws TeslaPowerwallCloudCommunicationException {

        String payload = "{\"enabled\":" + newStormMode + "}";

        logger.debug("payload = {}", payload);
        ByteArrayInputStream input = new ByteArrayInputStream(payload.getBytes());
        String response = invoke("POST", proxyAddress + "/api/1/energy_sites/" + siteID + "/storm_mode", accessToken,
                input, "application/json");
        logger.debug("response to Storm Mode change = {}", response);
        return response;
    }

    protected Properties getHttpHeaders(String accessToken) {
        Properties httpHeaders = new Properties();
        httpHeaders.put("Authorization", "Bearer " + accessToken);
        httpHeaders.put("Content-Type", "application/json");
        return httpHeaders;
    }

    private String invoke(String httpMethod, String uri, String accessToken)
            throws TeslaPowerwallCloudCommunicationException {
        return invoke(httpMethod, uri, accessToken, null, null);
    }

    private String invoke(String httpMethod, String uri, String accessToken, InputStream content, String contentType)
            throws TeslaPowerwallCloudCommunicationException {
        logger.debug("Calling url: {}", uri);
        String response;
        synchronized (this) {
            try {
                response = HttpUtil.executeUrl(httpMethod, uri, getHttpHeaders(accessToken), content, contentType,
                        TIMEOUT_MS);
            } catch (IOException ex) {
                logger.debug("{}", ex.getLocalizedMessage(), ex);
                // Response will also be set to null if parsing in executeUrl fails so we use null here to make the
                // error check below consistent.
                response = null;
            }
        }

        if (response.isEmpty()) {
            throw new TeslaPowerwallCloudCommunicationException(
                    String.format("TeslaPowerwallCloudcontroller returned no response while invoking %s", uri));
        }
        return response;
    }
}
