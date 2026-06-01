package dev.xylonity.knightlib.api.event.impl.server;

import com.mojang.brigadier.CommandDispatcher;
import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Fired when commands are being registered (provides the dispatcher for direct command registration)
 */
public final class CommandRegistrationEvent extends KnightLibEvent {

    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final CommandBuildContext buildContext;
    private final Commands.CommandSelection commandSelection;

    public CommandRegistrationEvent(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        this.dispatcher = dispatcher;
        this.buildContext = buildContext;
        this.commandSelection = commandSelection;
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return dispatcher;
    }

    public CommandBuildContext getBuildContext() {
        return buildContext;
    }

    public Commands.CommandSelection getCommandSelection() {
        return commandSelection;
    }

}