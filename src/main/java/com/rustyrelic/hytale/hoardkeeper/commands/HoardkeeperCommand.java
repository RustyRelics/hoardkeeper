package com.rustyrelic.hytale.hoardkeeper.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import javax.annotation.Nonnull;

/**
 * Parent for the /hoardkeeper command family (alias /hk). Executes nothing itself -- see
 * AbstractCommandCollection, which prints usage when run bare. Subcommands are added with
 * addSubCommand(...) in HoardkeeperPlugin.setup().
 */
public class HoardkeeperCommand extends AbstractCommandCollection {

    public HoardkeeperCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
        addAliases("hk"); // AbstractCommand.addAliases(String...), AbstractCommand.java:640

        // Open to everyone: each subcommand sets its own permission. Without this, a command with no
        // permission call gets one auto-generated from its name (AbstractCommand.setOwner/
        // generatePermission), and AbstractCommand.hasPermission checks a subcommand's PARENT permission
        // too -- so leaving this unset would silently require an extra "hoardkeeper" node on top of
        // stack/exclude's own nodes.
        requireNoPermission();
    }

}
