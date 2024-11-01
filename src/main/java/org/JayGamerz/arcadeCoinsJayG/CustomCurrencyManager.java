package org.JayGamerz.arcadeCoinsJayG;

import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class CustomCurrencyManager {
    private final Connection connection;
    private ArcadeCoinsJayG arcadeCoinsJayG;
    private YamlConfiguration messagesConfig;

    public CustomCurrencyManager(Connection connection, ArcadeCoinsJayG arcadeCoinsJayG) {
        this.connection = connection;
        this.arcadeCoinsJayG = arcadeCoinsJayG;
        createTableIfNotExists(); // Call the method to create the table if it doesn't exist
        loadMessages(); // Load messages from the messages.yml file
    }

    // Load messages from messages.yml
    private void loadMessages() {
        File messagesFile = new File(arcadeCoinsJayG.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            arcadeCoinsJayG.saveResource("messages.yml", false); // Copy from jar to folder
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Retrieve a message by key and replace placeholders
    public String getMessage(String key, String... placeholders) {
        String message = messagesConfig.getString(key);
        if (message != null && placeholders.length > 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message != null ? message : "Message not found.";
    }

    // Create the bedwars_currency table if it doesn't exist
    private void createTableIfNotExists() {
        String query = "CREATE TABLE IF NOT EXISTS bedwars_currency (" +
                "uuid VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "currency DOUBLE NOT NULL DEFAULT 0.0)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating bedwars_currency table.");
            e.printStackTrace();
        }
    }

    // Get the currency balance for a player
    public double getCurrency(UUID uuid) {
        double currency = 0.0;
        String query = "SELECT currency FROM bedwars_currency WHERE uuid = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currency = rs.getDouble("currency");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving currency for UUID: " + uuid);
            e.printStackTrace();
        }
        return currency;
    }

    // Set the currency balance for a player
    public void setCurrency(UUID uuid, double amount) {
        String query = "REPLACE INTO bedwars_currency (uuid, currency) VALUES (?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error setting currency for UUID: " + uuid + " to amount: " + amount);
            e.printStackTrace();
        }
    }

    // Add currency to a player's balance
    public void addCurrency(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double currentCurrency = getCurrency(uuid);
        setCurrency(uuid, currentCurrency + amount);
        player.sendMessage(getMessage("currency.added", "amount", String.valueOf(amount), "newBalance", String.valueOf(currentCurrency + amount)));
    }

    // Remove currency from a player's balance
    public void removeCurrency(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double currentCurrency = getCurrency(uuid);

        if (currentCurrency >= amount) {
            setCurrency(uuid, currentCurrency - amount);
            player.sendMessage(getMessage("currency.removed", "amount", String.valueOf(amount), "newBalance", String.valueOf(currentCurrency - amount)));
        } else {
            player.sendMessage(getMessage("currency.insufficient", "amount", String.valueOf(amount), "currentBalance", String.valueOf(currentCurrency)));
        }
    }

    // Display the currency balance to the player
    public void showBalance(Player player) {
        UUID uuid = player.getUniqueId();
        double currency = getCurrency(uuid);
        player.sendMessage(getMessage("currency.balance", "balance", String.valueOf(currency)));
    }
}
