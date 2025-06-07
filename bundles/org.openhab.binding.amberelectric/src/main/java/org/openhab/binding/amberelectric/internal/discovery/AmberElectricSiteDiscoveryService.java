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
package org.openhab.binding.amberelectric.internal.discovery;

import static org.openhab.binding.amberelectric.internal.AmberElectricBindingConstants.*;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.amberelectric.internal.AmberElectricAccountHandler;
import org.openhab.binding.amberelectric.internal.api.Sites;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

/**
 * The AmberElectricSiteDiscoveryService is responsible for auto detecting sites that are on the
 * Amber Electric service.
 *
 * @author paul@smedley.id.au - Initial contribution
 */

@Component(scope = ServiceScope.PROTOTYPE, service = AmberElectricSiteDiscoveryService.class)
@NonNullByDefault
public class AmberElectricSiteDiscoveryService
        extends AbstractThingHandlerDiscoveryService<AmberElectricAccountHandler> {

    private Logger logger = LoggerFactory.getLogger(AmberElectricSiteDiscoveryService.class);
    private @NonNullByDefault({}) ThingUID bridgeUid;

    private final Gson gson = new Gson();

    public AmberElectricSiteDiscoveryService() {
        super(AmberElectricAccountHandler.class, SUPPORTED_THING_TYPES_UIDS, 5, false);
    }

    protected String getSiteList() {
        return thingHandler.getSiteList();
    }

    @Override
    public void initialize() {
        bridgeUid = thingHandler.getThing().getUID();
        super.initialize();
    }

    private void discover() {
        String responseSiteList = getSiteList();
        logger.trace("Site List = {}", responseSiteList);
        HashMap<String, Object> properties = new HashMap<>();
        JsonArray jsonArraySiteList = JsonParser.parseString(responseSiteList).getAsJsonArray();
        Sites siteList = new Sites();
        for (int i = 0; i < jsonArraySiteList.size(); i++) {
            siteList = gson.fromJson(jsonArraySiteList.get(i), Sites.class);
            if (siteList == null) {
                return;
            }
            properties.put("id", siteList.id);
            ThingUID uid = new ThingUID(AMBERELECTRIC_SITE, bridgeUid, siteList.nmi);
            thingDiscovered(DiscoveryResultBuilder.create(uid).withBridge(bridgeUid).withProperties(properties)
                    .withRepresentationProperty("id").withLabel("Amber Electric - NMI " + siteList.nmi).build());
        }
    }

    @Override
    protected void startScan() {
        logger.debug("Starting device discovery");
        discover();
    }
}
