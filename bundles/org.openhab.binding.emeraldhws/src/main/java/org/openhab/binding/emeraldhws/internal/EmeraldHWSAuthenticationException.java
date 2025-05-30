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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Exception for when an unexpected response is received from the Emerald Servers.
 *
 * @author Paul Smedley - Initial contribution
 *
 */
@NonNullByDefault
public class EmeraldHWSAuthenticationException extends Exception {
    private static final long serialVersionUID = 529232811860854017L;

    public EmeraldHWSAuthenticationException(String message) {
        super(message);
    }

    public EmeraldHWSAuthenticationException(Throwable ex) {
        super(ex);
    }

    public EmeraldHWSAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
