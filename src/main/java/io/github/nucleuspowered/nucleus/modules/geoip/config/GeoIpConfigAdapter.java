/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.geoip.config;

import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;

public class GeoIpConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<GeoIpConfig> {

    public GeoIpConfigAdapter() {
        super(GeoIpConfig.class);
    }
}
