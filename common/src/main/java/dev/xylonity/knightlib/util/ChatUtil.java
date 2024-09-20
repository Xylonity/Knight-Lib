package dev.xylonity.knightlib.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

/**
 * Utility class for handling various chat-related tasks, including
 * sending messages to players, translating messages, and formatting
 * position data.
 *
 * @author Xylonity
 */
public class ChatUtil {

    /**
     * Creates a formatted message that allows for customization.
     * This method is highly flexible, allowing the user to specify a custom prefix,
     * message content, and formatting options.
     *
     * @param prefix The prefix for the message (can be empty or null).
     * @param message The actual content of the message.
     * @param format The desired formatting options (e.g., bold, italics, color).
     * @return A formatted MutableComponent ready to be sent as a message.
     */
    public static MutableComponent createFormattedMessage(String prefix, String message, ChatFormatting... format) {
        String finalMessage = (prefix != null ? prefix : "") + message;
        return Component.literal(finalMessage).withStyle(format);
    }

    /**
     * Creates a formatted translatable message that allows for customization.
     * This method is highly flexible, allowing the user to specify a custom prefix,
     * a translation key, optional arguments for translation, and formatting options.
     *
     * @param prefix The prefix for the message (can be empty or null).
     * @param translationKey The translation key for the message (defined in the language files).
     * @param args Optional arguments for the translation (e.g., item names, numbers, etc.).
     * @param format The desired formatting options (e.g., bold, italics, color).
     * @return A formatted translatable MutableComponent ready to be sent as a message.
     */
    public static MutableComponent createFormattedTranslatableMessage(String prefix, String translationKey, Object[] args, ChatFormatting... format) {
        String finalPrefix = (prefix != null ? prefix : "");
        MutableComponent translatableMessage = Component.translatable(translationKey, args);
        return Component.literal(finalPrefix).append(translatableMessage).withStyle(format);
    }

    /**
     * Sends a chat message to the specified player. This method only sends
     * the message if the player is on the client side.
     *
     * @param player The player to send the message to.
     * @param message The message as a MutableComponent.
     */
    public static void sendChatMessage(Player player, MutableComponent message) {
        if (player.level().isClientSide) {
            player.sendSystemMessage(message);
        }
    }

    /**
     * Sends a customizable chat message to the specified player.
     * This version uses the createFormattedMessage method to allow for complete customization.
     *
     * @param player The player to send the message to.
     * @param prefix The prefix for the message (can be empty or null).
     * @param message The actual content of the message.
     * @param format The desired formatting options (e.g., bold, italics, color).
     */
    public static void sendChatMessage(Player player, String prefix, String message, ChatFormatting... format) {
        sendChatMessage(player, createFormattedMessage(prefix, message, format));
    }

    /**
     * Sends a chat message to the player on the server side only.
     *
     * @param player The player to send the message to.
     * @param message The message as a Component.
     */
    public static void addServerChatMessage(Player player, Component message) {
        if (!player.level().isClientSide) {
            player.sendSystemMessage(message);
        }
    }

    /**
     * Sends a customizable server-side chat message to the player.
     * This version uses the createFormattedMessage method to allow for complete customization.
     *
     * @param player The player to send the message to.
     * @param prefix The prefix for the message (can be empty or null).
     * @param message The actual content of the message.
     * @param format The desired formatting options (e.g., bold, italics, color).
     */
    public static void addServerChatMessage(Player player, String prefix, String message, ChatFormatting... format) {
        addServerChatMessage(player, createFormattedMessage(prefix, message, format));
    }

    /**
     * Converts a BlockPos object into a readable string format.
     * Useful for debugging or displaying coordinates in chat.
     *
     * @param pos The BlockPos to convert.
     * @return A string in the format "x, y, z".
     */
    public static String blockPosToString(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    /**
     * Sends a status message to the player. This message appears as a
     * brief notification above the player's hotbar.
     *
     * @param player The player to send the message to.
     * @param prefix The prefix for the message (can be empty or null).
     * @param message The actual content of the message.
     * @param format The desired formatting options (e.g., bold, italics, color).
     */
    public static void sendStatusMessage(Player player, String prefix, String message, ChatFormatting... format) {
        player.displayClientMessage(createFormattedMessage(prefix, message, format), true);
    }

    /**
     * Sends feedback to the command source during command execution with optional arguments.
     *
     * @param context The command context.
     * @param prefix The prefix for the message (can be empty or null).
     * @param message The actual content of the message.
     * @param format The desired formatting options (e.g., bold, italics, color).
     */
    public static void sendFeedback(CommandContext<CommandSourceStack> context, String prefix, String message, ChatFormatting... format) {
        context.getSource().sendSuccess(() -> createFormattedMessage(prefix, message, format), false);
    }

    /**
     * Sends a formatted chat message with specific color formatting.
     *
     * @param player The player to send the message to.
     * @param message The message content.
     * @param color The desired chat color.
     */
    public static void sendColoredMessage(Player player, String message, ChatFormatting color) {
        MutableComponent coloredMessage = createFormattedMessage("", message).withStyle(color);
        player.sendSystemMessage(coloredMessage);
    }

    /**
     * Sends a localized error message to the player in red.
     *
     * @param player The player to send the message to.
     * @param message The error message translation key.
     */
    public static void sendErrorMessage(Player player, String message) {
        sendColoredMessage(player, message, ChatFormatting.RED);
    }

    /**
     * Sends a localized success message to the player in green.
     *
     * @param player The player to send the message to.
     * @param message The success message translation key.
     */
    public static void sendSuccessMessage(Player player, String message) {
        sendColoredMessage(player, message, ChatFormatting.GREEN);
    }

    /**
     * Sends a warning message to the player in yellow.
     *
     * @param player The player to send the message to.
     * @param message The warning message translation key.
     */
    public static void sendWarningMessage(Player player, String message) {
        sendColoredMessage(player, message, ChatFormatting.YELLOW);
    }

    /**
     * Sends a message to multiple players in a list.
     *
     * @param players A list of players to send the message to.
     * @param message The message to send.
     */
    public static void broadcastMessage(Iterable<Player> players, String message) {
        for (Player player : players) {
            sendChatMessage(player, "", message);
        }
    }

    /**
     * Sends a message to multiple players on the server side only.
     *
     * @param players A list of players to send the message to.
     * @param message The message to send.
     */
    public static void broadcastServerMessage(Iterable<Player> players, String message) {
        for (Player player : players) {
            addServerChatMessage(player, "", message);
        }
    }

    /**
     * Broadcasts a message to all players in the server with a specific formatting.
     *
     * @param players A list of players to send the message to.
     * @param prefix The prefix for the message.
     * @param message The actual content of the message.
     * @param format The desired formatting (bold, italics, etc).
     */
    public static void broadcastFormattedMessage(Iterable<Player> players, String prefix, String message, ChatFormatting... format) {
        for (Player player : players) {
            addServerChatMessage(player, prefix, message, format);
        }
    }

    /**
     * Sends a notification to players that includes positional data.
     *
     * @param player The player to send the message to.
     * @param prefix The prefix for the message.
     * @param pos The BlockPos data to include.
     */
    public static void sendPositionalMessage(Player player, String prefix, BlockPos pos) {
        String posString = blockPosToString(pos);
        sendChatMessage(player, prefix, "Position: " + posString, ChatFormatting.GRAY);
    }

    /**
     * Logs a formatted message to the server console.
     *
     * @param prefix The prefix of the log message.
     * @param message The message content to log.
     * @param formatting The desired formatting.
     */
    public static void logMessageToConsole(String prefix, String message, ChatFormatting formatting) {
        MutableComponent consoleMessage = createFormattedMessage(prefix, message, formatting);
        System.out.println(consoleMessage.getString());
    }

}
