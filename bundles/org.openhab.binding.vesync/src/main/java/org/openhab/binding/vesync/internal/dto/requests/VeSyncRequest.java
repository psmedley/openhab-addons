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
package org.openhab.binding.vesync.internal.dto.requests;

import org.eclipse.jetty.http.HttpMethod;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link VeSyncRequest} is a Java class used as a DTO to hold the Vesync's API's common request data.
 *
 * @author David Goodyear - Initial contribution
 */
public class VeSyncRequest {

    public transient HttpMethod httpMethod;

    @SerializedName("timeZone")
    public String timeZone = "America/New_York";

    @SerializedName("acceptLanguage")
    public String acceptLanguage = "en";

    // @SerializedName("accountID")
    // public String accountID = "";

    @SerializedName("authProtocolType")
    public String authProtocolType = "generic";

    @SerializedName("clientInfo")
    public String clientInfo = "SM N9005";

    @SerializedName("clientType")
    public String clientType = "vesyncApp";

    @SerializedName("clientVersion")
    public String clientVersion = "VeSync 5.6.60";

    @SerializedName("debugMode")
    public boolean debugMode = false;

    @SerializedName("osInfo")
    public String osInfo = "Android";

    @SerializedName("terminalId")
    public String terminalId = "cc15570b8b97e8d8";

    @SerializedName("token")
    public String token = "";

    @SerializedName("userCountryCode")
    public String userCountryCode = "DE";

    @SerializedName("appID")
    public String appID = "4c93ee2e";

    @SerializedName("sourceAppID")
    public String sourceAppID = "4c93ee2e";

    @SerializedName("traceId")
    public String traceId = "";

    @SerializedName("method")
    public String method;

    public VeSyncRequest() {
        traceId = String.valueOf(System.currentTimeMillis());
        httpMethod = HttpMethod.POST;
    }
}
