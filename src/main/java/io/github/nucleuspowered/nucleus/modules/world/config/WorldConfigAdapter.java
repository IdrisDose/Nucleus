/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.modules.world.config;

import io.github.nucleuspowered.nucleus.internal.qsml.NucleusConfigAdapter;

public class WorldConfigAdapter extends NucleusConfigAdapter.StandardWithSimpleDefault<WorldConfig> {

    public WorldConfigAdapter() {
        super(WorldConfig.class);
    }
}
