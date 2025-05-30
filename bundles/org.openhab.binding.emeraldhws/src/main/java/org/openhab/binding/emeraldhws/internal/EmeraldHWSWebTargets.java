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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Handles performing the actual HTTP requests for communicating with the Emerald Servers.
 *
 * @author Paul Smedley - Initial Contribution
 *
 */
@NonNullByDefault
public class EmeraldHWSWebTargets {
    private static final int TIMEOUT_MS = 30000;

    private String getTokenUri = "https://api.emerald-ems.com.au/api/v1/customer/sign-in";
    private String token = "";
    private final Logger logger = LoggerFactory.getLogger(EmeraldHWSWebTargets.class);
    private HttpClient httpClient;

    private final Gson gson = new Gson();

    public EmeraldHWSWebTargets(String hostname, HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getToken(String email, String password)
            throws EmeraldHWSCommunicationException, EmeraldHWSAuthenticationException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", "customer");
        jsonObject.addProperty("password", password);
        jsonObject.addProperty("email", email);
        jsonObject.addProperty("force_sm_off", false);
        logger.debug("logonjson = {}", jsonObject.toString());
        String response = invoke(getTokenUri, HttpMethod.POST, "Content-Type", "application/json",
                jsonObject.toString());
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        String token = jsonResponse.get("token").getAsString();
        logger.debug("Token: {}", token);
        return token;
    }

    private String invoke(String uri, String email, String password)
            throws EmeraldHWSCommunicationException, EmeraldHWSAuthenticationException {
        if (token.isEmpty()) {
            token = getToken(email, password);
        }
        return invoke(uri, HttpMethod.GET, "Authorization", "Bearer " + token, "");
    }

    private String invoke(String uri, HttpMethod method, String headerKey, String headerValue, String params)
            throws EmeraldHWSCommunicationException, EmeraldHWSAuthenticationException {
        logger.debug("Calling url: {}", uri);
        int status = 0;
        String jsonResponse = "";
        synchronized (this) {
            try {
                Request request = httpClient.newRequest(uri).method(method).header(headerKey, headerValue)
                        .timeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        .content(new StringContentProvider(params), "application/json");
                if (logger.isTraceEnabled()) {
                    logger.trace("{} request for {}", method, uri);
                }
                ContentResponse response = request.send();
                status = response.getStatus();
                jsonResponse = response.getContentAsString();
                if (!jsonResponse.isEmpty()) {
                    logger.trace("JSON response: '{}'", jsonResponse);
                }
                if (status == HttpStatus.UNAUTHORIZED_401) {
                    throw new EmeraldHWSAuthenticationException("Unauthorized");
                }
                if (!HttpStatus.isSuccess(status)) {
                    throw new EmeraldHWSCommunicationException(
                            String.format("Tesla Powerwall returned error <%d> while invoking %s", status, uri));
                }
            } catch (TimeoutException | ExecutionException | InterruptedException ex) {
                throw new EmeraldHWSCommunicationException(String.format("{}", ex.getLocalizedMessage(), ex));
            }
        }

        return jsonResponse;
    }
}
