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
package org.openhab.binding.emeraldhws.internal.discovery;

import static org.openhab.binding.emeraldhws.internal.EmeraldHWSBindingConstants.*;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.emeraldhws.internal.EmeraldHWSAccountHandler;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The EmeraldHWSDiscoveryService is responsible for auto detecting an Emerald
 * HWS device in the local network.
 *
 * @author paul@smedley.id.au - Initial contribution
 */

@Component(scope = ServiceScope.PROTOTYPE, service = EmeraldHWSDiscoveryService.class)
@NonNullByDefault
public class EmeraldHWSDiscoveryService extends AbstractThingHandlerDiscoveryService<EmeraldHWSAccountHandler> {

    private Logger logger = LoggerFactory.getLogger(EmeraldHWSDiscoveryService.class);
    private @NonNullByDefault({}) ThingUID bridgeUid;

    public EmeraldHWSDiscoveryService() {
        super(EmeraldHWSAccountHandler.class, SUPPORTED_THING_TYPES_UIDS, 5, false);
    }

    @Override
    public void initialize() {
        bridgeUid = thingHandler.getThing().getUID();
        super.initialize();
    }

    private void discover() {
        /* getapi() and parse the list..... */
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("uuid", "9b0f0970-ba6d-454d-befd-3688528a6636");
        ThingUID uid = new ThingUID(THING_TYPE_HWS, bridgeUid, "9b0f0970-ba6d-454d-befd-3688528a6636");
        thingDiscovered(DiscoveryResultBuilder.create(uid).withBridge(bridgeUid).withProperties(properties)
                .withRepresentationProperty("uuid").withLabel("Emerald HWS").build());
    }

    @Override
    protected void startScan() {
        logger.debug("Starting device discovery");
        discover();
    }
}
