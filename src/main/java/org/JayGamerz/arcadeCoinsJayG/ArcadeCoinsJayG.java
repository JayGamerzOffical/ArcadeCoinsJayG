package org.JayGamerz.arcadeCoinsJayG;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.UUID;

public class ArcadeCoinsJayG extends JavaPlugin implements CommandExecutor, Listener {

    ArcadeCoinsJayG arcadeCoinsJayG;
    private Map<UUID, Double> coinMultipliers = new HashMap<>();
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private CurrencyConversionGUI conversionGUI;
    private TimeZoneChecker timeZoneChecker;
    private static double CONVERSION_RATE;
    private Connection connection;
    private String host, database, username, password;
    private int port;
    CustomCurrencyManager customCurrencyManager;
    private CoinGiftManager coinGiftManager;
    private File dailyRewardsFile;
    private FileConfiguration dailyRewardsConfig;


    @Override
    public void onDisable() {
        getLogger().info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
        closeDatabaseConnection();
    }

    @Override
    public void onEnable() {

        config = this.getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        loadMessages();
        setupDatabase();
        setupDailyRewardsFile();
        coinGiftManager = new CoinGiftManager(this);
        timeZoneChecker = new TimeZoneChecker(config);
        getServer().getPluginManager().registerEvents(this, this);
        conversionGUI = new CurrencyConversionGUI(this, this);
        Bukkit.getServer().getPluginManager().registerEvents(conversionGUI, this);
        customCurrencyManager = new CustomCurrencyManager(connection, this);
    }

    private void setupDatabase() {
        host = config.getString("mysql.host");
        port = config.getInt("mysql.port");
        database = config.getString("mysql.database");
        username = config.getString("mysql.username");
        password = config.getString("mysql.password");

        try {
            openDatabaseConnection();
            createTableIfNotExists();
        } catch (SQLException e) {
            getLogger().severe("Please correctly setup your MySQL DataBase in plugins/ArcadeCoinsJayG/config.yml!");
            getLogger().severe("Could not connect to MySQL database. Disabling plugin.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false); // Copy from jar to folder
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String key, String... placeholders) {
        String message = messagesConfig.getString(key);
        if (message != null && placeholders.length > 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return message != null ? message : "Message not found.";
    }

    public double getConversionRate() {
        return CONVERSION_RATE = config.getDouble("ConversionRate.BedWars", 10);
    }

    private void openDatabaseConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password);
        getLogger().info("Connected to MySQL database.");
    }

    private void createTableIfNotExists() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS arcade_coins (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "coins DOUBLE NOT NULL, " +
                "last_claim TIMESTAMP)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableSQL);
        }
    }

    private void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                getLogger().info("Closed Arcade Coins MySQL connection.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Retrieves the coin balance for a specific player UUID
    public double getCoins(UUID uuid) {
        double coins = 0.0;
        String query = "SELECT coins FROM arcade_coins WHERE uuid = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                coins = rs.getDouble("coins");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving coins for UUID: " + uuid);
            e.printStackTrace();
        }
        return coins;
    }

    // Sets the coin balance for a specific player UUID
    private void setCoins(UUID uuid, double amount) {
        String query = "REPLACE INTO arcade_coins (uuid, coins) VALUES (?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error setting coins for UUID: " + uuid + " to amount: " + amount);
            e.printStackTrace();
        }
    }

    // Removes a specified amount of coins from a player's balance
    public void removeCoins(Player player, double amount) {
        UUID uuid = player.getUniqueId();
        double currentCoins = getCoins(uuid);

        // Check if the player has enough coins
        if (currentCoins >= amount) {
            double newBalance = currentCoins - amount;
            setCoins(uuid, newBalance);
            player.sendMessage("You have successfully removed " + amount + " coins. Your new balance is: " + newBalance + " coins.");
        } else {
            player.sendMessage("§cYou do not have enough coins to remove " + amount + " coins. Your current balance is: " + currentCoins + " coins.");
        }
    }

    public void showBalance(Player player) {
        UUID uuid = player.getUniqueId();
        double balance = getCoins(uuid);
        player.sendMessage("Your current balance is: " + balance + " coins.");
    }

    public void showLeaderboard(Player player) {
        String query = "SELECT uuid, coins FROM arcade_coins ORDER BY coins DESC LIMIT 10"; // SQL query to get top 10 players
        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            // Send the leaderboard title with color
            player.sendMessage(ChatColor.AQUA + "=== " + ChatColor.GOLD + config.getString("YourServerName", "YourNetwork") + ChatColor.AQUA + " AC Leaderboard ===");

            int rank = 1; // Initialize rank

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                double coins = rs.getDouble("coins");
                Player p = Bukkit.getPlayer(UUID.fromString(uuid));
                String playerName = (p != null) ? p.getName() : "Unknown Player"; // Get player name or use 'Unknown Player'

                // Convert coins to string and send rank and coins with colors
                player.sendMessage(ChatColor.YELLOW + String.valueOf(rank) + ". " + ChatColor.GREEN + playerName + ChatColor.YELLOW + ": " + ChatColor.WHITE + String.valueOf(coins) + ChatColor.YELLOW + " coins"); // Send rank and coins
                rank++; // Increment rank
            }

            // If no players found, notify the player
            if (rank == 1) {
                player.sendMessage(ChatColor.RED + "No players found on the leaderboard.");
            }

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while fetching the leaderboard. Please try again later."); // Send error message to player
            e.printStackTrace(); // Print stack trace for debugging
        }
    }


    public double checkCoins(Player player) {
        UUID uuid = player.getUniqueId();
        return getCoins(uuid);
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

    private void saveDailyRewardsFile() {
        try {
            dailyRewardsConfig.save(dailyRewardsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupDailyRewardsFile() {
        dailyRewardsFile = new File(getDataFolder(), "dailyrewards.yml");
        if (!dailyRewardsFile.exists()) {
            dailyRewardsFile.getParentFile().mkdirs();
            saveResource("dailyrewards.yml", false);
        }
        dailyRewardsConfig = YamlConfiguration.loadConfiguration(dailyRewardsFile);
    }

    public void addCoins(UUID uuid, double amount) {
        double currentCoins = getCoins(uuid);
        setCoins(uuid, currentCoins + amount);
    }

    private void claimDailyReward(Player player) {
        UUID uuid = player.getUniqueId();
        LocalDateTime lastClaimTime = getLastClaimTime(uuid);

        // Check if the player can claim the daily reward
        if (lastClaimTime != null && !timeZoneChecker.canClaimDailyReward(lastClaimTime)) {
            player.sendMessage(ChatColor.RED + "You have already claimed your daily reward today!");
            return;
        }

        // Get the reward amount from the configuration or default to 100.0
        double reward = getConfig().getDouble("dailyRewardAmount", 100.0);
        addCoins(uuid, reward);
        updateLastClaimTime(uuid);

        player.sendMessage(ChatColor.GREEN + "You have claimed your daily reward of " + ChatColor.GOLD + reward + ChatColor.GREEN + " coins!");
    }

    private LocalDateTime getLastClaimTime(UUID uuid) {
        String lastClaimString = dailyRewardsConfig.getString("players." + uuid + ".lastClaim");

        if (lastClaimString != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(lastClaimString, formatter);
        }
        return null;
    }

    private void updateLastClaimTime(UUID uuid) {
        LocalDateTime now = timeZoneChecker.getCurrentDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        dailyRewardsConfig.set("players." + uuid + ".lastClaim", now.format(formatter));
        saveDailyRewardsFile();
    }
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;
            // Handle player commands
            return handleCoinsCommand(player, args);
        } else if (sender instanceof ConsoleCommandSender) {
            // Handle console commands
            return handleCoinsCommandConsole(sender, args);
        }
        sender.sendMessage(ChatColor.RED + "This command can only be executed by a player or the console.");
        return true;
    }

    // Handle coins command for players
    private boolean handleCoinsCommand(Player player, String[] args) {
        // If no arguments, show the player's current balance
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Your current balance is: " + ChatColor.GOLD + getCoins(player.getUniqueId()));
            return true;
        }
        // Handle subcommands
        return handleCoinsSubCommands(player, args);
    }

    // Handle coins command for console
    private boolean handleCoinsCommandConsole(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "You must specify a subcommand. Usage: /coins <subcommand>");
            return true;
        }
        // Check the first argument
        return handleCoinsSubCommands(sender, args);
    }

    // Handle the subcommands for both players and console
    private boolean handleCoinsSubCommands(CommandSender sender, String[] args) {
        switch (args[0].toLowerCase()) {
            case "add":
                if (sender.hasPermission("arcadecoins.coins.add")) {
                    if (args.length == 3) {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "Player not found.");
                            return true;
                        }
                        int amount = Integer.parseInt(args[2]);
                        addCoins(target.getUniqueId(), amount);
                        sender.sendMessage(ChatColor.GREEN + "Added " + ChatColor.GOLD + amount + ChatColor.GREEN + " coins to " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + ".");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /coins add <player> <amount>");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                }
                break;

            case "remove":
                if (sender.hasPermission("arcadecoins.coins.remove")) {
                    if (args.length == 3) {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "Player not found.");
                            return true;
                        }
                        int amount = Integer.parseInt(args[2]);
                        removeCoins(target, amount);
                        sender.sendMessage(ChatColor.YELLOW + "Removed " + ChatColor.GOLD + amount + ChatColor.YELLOW + " coins from " + ChatColor.AQUA + target.getName() + ChatColor.YELLOW + ".");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /coins remove <player> <amount>");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                }
                break;

            case "set":
                if (sender.hasPermission("arcadecoins.coins.set")) {
                    if (args.length == 3) {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "Player not found.");
                            return true;
                        }
                        int amount = Integer.parseInt(args[2]);
                        setCoins(target.getUniqueId(), amount);
                        sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + "'s coin balance to " + ChatColor.GOLD + amount + ChatColor.GREEN + ".");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /coins set <player> <amount>");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                }
                break;

            case "check":
                if (sender.hasPermission("arcadecoins.coins.check")) {
                    if (args.length == 2) {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "Player not found.");
                            return true;
                        }
                        sender.sendMessage(ChatColor.AQUA + target.getName() + ChatColor.YELLOW + "'s balance is: " + ChatColor.GOLD + getCoins(target.getUniqueId()));
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /coins check <player>");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                }
                break;

            case "convert":
                if (sender instanceof Player) {
                    if (sender.hasPermission("arcadecoins.coins.convert")) {
                        conversionGUI.openConversionMenu((Player) sender);
                        sender.sendMessage(ChatColor.BLUE + "Opening conversion menu...");
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                }
                break;

            case "bwcoins":
                if (sender instanceof Player) {
                    if (sender.hasPermission("arcadecoins.coins.bwcoins")) {
                        double bwcoins = getCustomCurrencyManager().getCurrency(((Player) sender).getUniqueId());
                        sender.sendMessage(ChatColor.YELLOW + "Your BedWars Coins are: " + ChatColor.GOLD + bwcoins);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                }
                break;

            case "shop":
                if (sender instanceof Player) {
                    if (sender.hasPermission("arcadecoins.coins.shop")) {
                        sender.sendMessage(ChatColor.YELLOW + "Currently Shop is Not Available and Coming very soon.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                }
                break;

            case "daily":
                if (sender instanceof Player) {
                    if (sender.hasPermission("arcadecoins.coins.dailyreward")) {
                        claimDailyReward((Player) sender);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                }
                break;

            case "gift":
                if (sender instanceof Player) {
                    if (sender.hasPermission("arcadecoins.coins.gift")) {
                        if (args.length == 3) {
                            Player player = (Player) sender;
                            String recipientName = args[1];
                            double amount;

                            try {
                                amount = Double.parseDouble(args[2]);
                                coinGiftManager.giftCoins(player, recipientName, amount);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(ChatColor.RED + "Invalid amount specified. Please enter a number.");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Usage: /coins gift <player> <amount>");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                }
                break;

            case "top":
                if (sender instanceof Player) {
                    if (sender.hasPermission("arcadecoins.coins.leaderboard")) {
                        sender.sendMessage(ChatColor.DARK_GREEN + "Displaying leaderboard...");
                        showLeaderboard((Player) sender);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                }
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Available commands: " + ChatColor.YELLOW + "/coins, /coins add,/coins bwcoins ,/coins shop, /coins remove, /coins set, /coins check, /coins convert, /coins daily, /coins top");
                break;
        }
        return true;
    }

    public ArcadeCoinsJayG getArcadeCoinsJayG() {
        return arcadeCoinsJayG;
    }

    public CustomCurrencyManager getCustomCurrencyManager() {
        return customCurrencyManager;
    }
}
