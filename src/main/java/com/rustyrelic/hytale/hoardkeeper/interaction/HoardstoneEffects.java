package com.rustyrelic.hytale.hoardkeeper.interaction;

/**
 * The three effect asset ids the Hoardstone plays on use, confirmed against the vanilla asset
 * tree. Kept in one place so there is exactly one spot that names them.
 */
final class HoardstoneEffects {

    private HoardstoneEffects() {
    }

    static final String SUCCESS_PARTICLE = "Potion_Morph_Burst";
    static final String SUCCESS_SOUND = "SFX_Deployable_Totem_Heal_Spawn";
    static final String NOPE_SOUND = "SFX_Sleep_Fail";

}
