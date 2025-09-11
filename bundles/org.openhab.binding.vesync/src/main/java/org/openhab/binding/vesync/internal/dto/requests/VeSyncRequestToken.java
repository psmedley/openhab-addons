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

import com.google.gson.annotations.SerializedName;

/**
 * The {@link VeSyncRequestToken} is the Java class as a DTO to exchange an
 * authorizeCode for a token for the Vesync API.
 *
 * @author Paul Smedley - Initial contribution
 */
public class VeSyncRequestToken extends VeSyncRequest {

    @SerializedName("authorizeCode")
    public String authorizeCode;
    @SerializedName("emailSubscriptions")
    boolean emailSubscriptions = false;
    // @SerializedName("bizToken")
    // public String bizToken;
    // @SerializedName("regionChange")
    // public String regionChange;

    public VeSyncRequestToken() {
        super();
        method = "loginByAuthorizeCode4Vesync";
    }

    public VeSyncRequestToken(String authorizeCode) {
        this();
        this.authorizeCode = authorizeCode;
        // this.bizToken = "";
        // this.regionChange = "";
    }
}
