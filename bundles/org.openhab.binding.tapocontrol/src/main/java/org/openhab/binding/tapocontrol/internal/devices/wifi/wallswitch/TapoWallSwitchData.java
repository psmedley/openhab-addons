/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.tapocontrol.internal.devices.wifi.wallswitch;

import static org.openhab.binding.tapocontrol.internal.TapoControlHandlerFactory.GSON;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.tapocontrol.internal.devices.dto.TapoBaseDeviceData;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Tapo-Device Information class
 *
 * @author Paul Smedley - Initial contribution
 */
@NonNullByDefault
public class TapoWallSwitchData extends TapoBaseDeviceData {
    @SerializedName("device_on")
    @Expose(serialize = true, deserialize = true)
    private boolean deviceOn = false;

    @SerializedName("on_time")
    @Expose(serialize = false, deserialize = true)
    private long onTime = 0;

    /***********************************
     *
     * SET VALUES
     *
     ************************************/

    public void switchOn() {
        deviceOn = true;
    }

    public void switchOff() {
        deviceOn = false;
    }

    public void switchOnOff(boolean on) {
        deviceOn = on;
    }

    /***********************************
     *
     * GET VALUES
     *
     ************************************/

    public boolean isOff() {
        return !deviceOn;
    }

    public boolean isOn() {
        return deviceOn;
    }

    public long getOnTime() {
        return onTime;
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        return GSON.toJson(this);
    }
}
