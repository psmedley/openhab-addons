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
package org.openhab.binding.bluetooth.bluegiga.internal.command.attributeclient;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaDeviceCommand;

/**
 * Class to implement the BlueGiga command <b>writeCommand</b>.
 * <p>
 * Writes the value of a remote devices attribute. The handle and the new value of the attribute
 * are gives as parameters.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaWriteCommandCommand extends BlueGigaDeviceCommand {
    public static final int COMMAND_CLASS = 0x04;
    public static final int COMMAND_METHOD = 0x06;

    /**
     * Attribute handle to write
     * <p>
     * BlueGiga API type is <i>uint16</i> - Java type is {@link int}
     */
    private int attHandle;

    /**
     * Value for the attribute
     * <p>
     * BlueGiga API type is <i>uint8array</i> - Java type is {@link int[]}
     */
    private int[] data = new int[0];

    /**
     * Attribute handle to write
     *
     * @param attHandle the attHandle to set as {@link int}
     */
    public void setAttHandle(int attHandle) {
        this.attHandle = attHandle;
    }

    /**
     * Value for the attribute
     *
     * @param data the data to set as {@link int[]}
     */
    public void setData(int[] data) {
        this.data = data;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeUInt8(connection);
        serializeUInt16(attHandle);
        serializeUInt8Array(data);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaWriteCommandCommand [connection=");
        builder.append(connection);
        builder.append(", attHandle=");
        builder.append(attHandle);
        builder.append(", data=");
        for (int c = 0; c < data.length; c++) {
            if (c > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", data[c]));
        }
        builder.append(']');
        return builder.toString();
    }
}
