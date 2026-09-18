package com.rustyrelic.hytale.hoardkeeper;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3dc;

/**
 * Everything StackEngine needs to run: whose inventory to pull from, and where/how far to look for
 * chests. Doesn't know about commands or players — sourceRef is just an entity with an inventory.
 */
public record StackRequest(Store<EntityStore> store, Ref<EntityStore> sourceRef, World world, Vector3dc position, double radius) {
}
