/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.api.data;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.text.MessageFormat;
import java.util.Optional;

public class LocationData {

    private final String warpName;
    private final Location<World> location;
    private final Vector3d rotation;

    public LocationData(String name, Location<World> location, Vector3d rotation) {
        this.rotation = rotation;
        this.location = location;
        this.warpName = name;
    }

    public String getName() {
        return warpName;
    }

    public Vector3d getRotation() {
        return rotation;
    }

    public Optional<Location<World>> getLocation() {
        return Optional.ofNullable(location);
    }

    public Optional<Transform<World>> getTransform() {
        if (location == null) {
            return Optional.empty();
        }

        return Optional.of(new Transform<>(location.getExtent(), location.getPosition(), rotation));
    }

    public String toLocationString() {
        if (location == null) {
            return MessageFormat.format("name: {0}, no location", warpName);
        }

        return MessageFormat.format("name: {0}, world: {1}, x: {2}, y: {3}, z: {4}", warpName, location.getExtent().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
