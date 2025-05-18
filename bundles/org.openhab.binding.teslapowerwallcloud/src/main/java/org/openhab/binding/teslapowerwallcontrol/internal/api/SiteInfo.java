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
public class SiteInfo {
    @SerializedName("response")
    public @NonNullByDefault({}) SiteInfoResponse siteInfoResponse;

    public class SiteInfoResponse {
        @SerializedName("backup_reserve_percent")
        public int reserve;
        @SerializedName("default_real_mode")
        public String defaultRealMode = "";

        @SerializedName("site_name")
        public String siteName = "";

        @SerializedName("storm_mode_enabled")
        public boolean stormModeEnabled;

        public String version = "";
    }

    private SiteInfo() {
    }
}
