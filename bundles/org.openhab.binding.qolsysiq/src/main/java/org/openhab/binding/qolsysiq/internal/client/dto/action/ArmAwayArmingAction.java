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
package org.openhab.binding.qolsysiq.internal.client.dto.action;

/**
 * An {@link ArmingActionType#ARM_AWAY} type of {@link ArmingAction} message sent to the panel
 *
 * @author Dan Cunningham - Initial contribution
 */
public class ArmAwayArmingAction extends ArmingAction {
    public Integer delay;

    public ArmAwayArmingAction(String token, Integer partitionId, Integer delay) {
        super(ArmingActionType.ARM_AWAY, token, partitionId);
        this.delay = delay;
    }

    public ArmAwayArmingAction(String token, Integer partitionId) {
        this(token, partitionId, null);
    }

    public ArmAwayArmingAction(Integer partitionId) {
        this("", partitionId, null);
    }

    public ArmAwayArmingAction(Integer partitionId, Integer delay) {
        this("", partitionId, delay);
    }
}
