package org.JayGamerz.arcadeCoinsJayG;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CoinGiftManager {

    private final ArcadeCoinsJayG plugin;

    public CoinGiftManager(ArcadeCoinsJayG plugin) {
        this.plugin = plugin;
    }

    public void giftCoins(Player sender, String recipientName, double amount) {
        // Check if the amount is negative
        if (amount < 0) {
            sender.sendMessage(ChatColor.RED + "You cannot gift a negative amount of coins.");
            return;
        }

        UUID senderUUID = sender.getUniqueId();
        UUID recipientUUID = getPlayerUUIDByName(recipientName);

        // Check if recipient exists
        if (recipientUUID == null) {
            sender.sendMessage(ChatColor.RED + "Player " + recipientName + " not found.");
            return;
        }

        // Check if the sender has enough coins
        double senderCoins = plugin.getCoins(senderUUID);
        if (senderCoins < amount) {
            sender.sendMessage(ChatColor.RED + "You do not have enough coins to gift " + amount + " coins.");
            return;
        }

        // Deduct coins from sender and add to recipient
        plugin.removeCoins(sender, amount);
        plugin.addCoins(recipientUUID, amount);
        sender.sendMessage(ChatColor.GREEN + "You have gifted " + amount + " coins to " + recipientName + ".");

        Player recipient = Bukkit.getPlayer(recipientUUID);
        if (recipient != null) {
            recipient.sendMessage(ChatColor.GREEN + sender.getName() + " has gifted you " + amount + " coins!");
        }
    }

    private UUID getPlayerUUIDByName(String playerName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return player.getUniqueId();
            }
        }
        // If player is not online, check in database or other methods if needed
        return null; // Return null if the player is not found
    }
}
