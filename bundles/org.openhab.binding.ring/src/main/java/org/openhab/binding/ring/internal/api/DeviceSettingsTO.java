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
package org.openhab.binding.ring.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link DeviceSettingsTO} class is part of the Ring Devices Structure
 *
 * @author Paul Smedley - Initial contribution
 */
@NonNullByDefault
public class DeviceSettingsTO {

    @SerializedName("motion_detection_enabled")
    public boolean motionDetectionEnabled;

    @SerializedName("lite_24x7")
    public @Nullable Lite24x7TO lite24x7;

    public static class Lite24x7TO {
        @SerializedName("enabled")
        public boolean enabled = false;

        @SerializedName("frequency")
        public int frequency = 0;
    }
}
