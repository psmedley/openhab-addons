/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.openhab.binding.teslapowerwallcloud.internal.api.LiveStatus;
import org.openhab.binding.teslapowerwallcloud.internal.api.SiteInfo;
import org.openhab.binding.teslapowerwallcloud.internal.api.TokenResponse;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@NonNullByDefault
public class TeslaPowerwallCloudWebTargets {

    private static final int TIMEOUT_MS = 30000;
    private final Logger logger = LoggerFactory.getLogger(TeslaPowerwallCloudWebTargets.class);
    private static final String BASE_URI = "https://fleet-api.prd.na.vn.cloud.tesla.com/api/1/energy_sites/";
    private final Gson gson = new Gson();

    private final HttpClient httpClient;

    public TeslaPowerwallCloudWebTargets(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getSiteID(String accessToken) throws TeslaPowerwallCloudCommunicationException {
        String response = invoke("GET", "https://fleet-api.prd.na.vn.cloud.tesla.com/api/1/products", accessToken);
        logger.debug("Product List response = {}", response);

        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
        JsonArray jsonResponse = jsonObject.getAsJsonArray("response");
        String siteID = "";

        for (int i = 0; i < jsonResponse.size(); i++) {
            JsonObject productObj = jsonResponse.get(i).getAsJsonObject();
            if (productObj.has("energy_site_id")) {
                siteID = productObj.get("energy_site_id").getAsString();
                break;
            }
        }

        logger.debug("Selected siteID is = {}", siteID);
        return siteID;
    }

    @Nullable
    public TokenResponse generateAccessToken(String refreshToken, String client_id) {
        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("grant_type", "refresh_token");
        jsonPayload.addProperty("client_id", client_id);
        jsonPayload.addProperty("refresh_token", refreshToken);
        String payload = gson.toJson(jsonPayload);

        logger.debug("payload = {}", payload);

        try {
            // Note: Tesla auth endpoint does not require the Bearer token, passing empty string
            String response = invoke("POST", "https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/token", "", payload,
                    "application/json");
            logger.trace("response = {}", response);
            return gson.fromJson(response, TokenResponse.class);
        } catch (TeslaPowerwallCloudCommunicationException ex) {
            logger.debug("Exception during generateAccessToken: {}", ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    @Nullable
    public SiteInfo getSiteInfo(String accessToken, String siteID) throws TeslaPowerwallCloudCommunicationException {
        String response = invoke("GET", BASE_URI + siteID + "/site_info", accessToken);
        logger.trace("getSiteInfo response = {}", response);
        return gson.fromJson(response, SiteInfo.class);
    }

    @Nullable
    public LiveStatus getLiveStatus(String accessToken, String siteID)
            throws TeslaPowerwallCloudCommunicationException {
        String response = invoke("GET", BASE_URI + siteID + "/live_status", accessToken);
        logger.trace("getLiveStatus response = {}", response);
        return gson.fromJson(response, LiveStatus.class);
    }

    public String setOperatingMode(String accessToken, String siteID, Command newMode)
            throws TeslaPowerwallCloudCommunicationException {
        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("default_real_mode", newMode.toString());
        String payload = gson.toJson(jsonPayload);

        logger.debug("payload = {}", payload);
        String response = invoke("POST", BASE_URI + siteID + "/operation", accessToken, payload, "application/json");
        logger.trace("response to Operating Mode change = {}", response);
        return response;
    }

    public String setReserve(String accessToken, String siteID, Command newReserve)
            throws TeslaPowerwallCloudCommunicationException {
        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("backup_reserve_percent", Float.parseFloat(newReserve.toString()));
        String payload = gson.toJson(jsonPayload);

        logger.debug("payload = {}", payload);
        String response = invoke("POST", BASE_URI + siteID + "/backup", accessToken, payload, "application/json");
        logger.trace("response to reserve change = {}", response);
        return response;
    }

    public String setStormMode(String accessToken, String siteID, String newStormMode)
            throws TeslaPowerwallCloudCommunicationException {
        JsonObject jsonPayload = new JsonObject();
        jsonPayload.addProperty("enabled", Boolean.parseBoolean(newStormMode));
        String payload = gson.toJson(jsonPayload);

        logger.debug("payload = {}", payload);
        String response = invoke("POST", BASE_URI + siteID + "/storm_mode", accessToken, payload, "application/json");
        logger.trace("response to Storm Mode change = {}", response);
        return response;
    }

    private String invoke(String httpMethod, String uri, String accessToken)
            throws TeslaPowerwallCloudCommunicationException {
        return invoke(httpMethod, uri, accessToken, null, null);
    }

    private String invoke(String httpMethod, String uri, String accessToken, @Nullable String payload,
            @Nullable String contentType) throws TeslaPowerwallCloudCommunicationException {
        logger.debug("Calling url: {}", uri);

        try {
            Request request = httpClient.newRequest(uri).method(httpMethod).timeout(TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // Only add Auth header if it's provided (helps when generating the access token)
            if (!accessToken.isEmpty()) {
                request.header(HttpHeader.AUTHORIZATION, "Bearer " + accessToken);
            }

            // Use StringContentProvider and .content() for the payload
            if (payload != null && contentType != null) {
                request.content(new StringContentProvider(payload));
                request.header(HttpHeader.CONTENT_TYPE, contentType);
            } else {
                request.header(HttpHeader.CONTENT_TYPE, "application/json");
            }

            ContentResponse response = request.send();

            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                String contentAsString = response.getContentAsString();
                if (contentAsString == null || contentAsString.isEmpty()) {
                    throw new TeslaPowerwallCloudCommunicationException(
                            "Received empty successful response from " + uri);
                }
                return contentAsString;
            } else {
                throw new TeslaPowerwallCloudCommunicationException(String.format("HTTP Error %d: %s when invoking %s",
                        response.getStatus(), response.getContentAsString(), uri));
            }

        } catch (InterruptedException | TimeoutException | ExecutionException ex) {
            logger.debug("{}", ex.getLocalizedMessage(), ex);
            throw new TeslaPowerwallCloudCommunicationException("Communication error: " + ex.getMessage(), ex);
        }
    }
}
