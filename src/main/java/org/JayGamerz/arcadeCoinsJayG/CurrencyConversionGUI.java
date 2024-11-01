package org.JayGamerz.arcadeCoinsJayG;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

public class CurrencyConversionGUI implements Listener {

    private final ArcadeCoinsJayG econ; // Economy for Arcade Coins


    public CurrencyConversionGUI(JavaPlugin plugin, ArcadeCoinsJayG economy) {
        this.econ = economy;
    }

    private ItemStack getCustomHead(String playerName,String HeadName, int num) {
        // Create a new item stack of type player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);

        if (head.getItemMeta() instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            skullMeta.setDisplayName(HeadName);

            // Set the GameProfile for the head to apply the skin
            GameProfile profile = new GameProfile(UUID.randomUUID(), playerName);
            if (num==1) {
                profile.getProperties().put("textures", fetchSkinTexture(playerName, "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV", "0L3RleHR1cmUvODMzNzA3ZDk1NjYxYzAyYWQzYWY4ZjU2NjRlZjY0YzU3OTk4MzA4YzQ0YmY2NmZjNWZjY2NmYjhmYjA1MTI0OCJ9fX0="));
            }if (num==2) {
                profile.getProperties().put("textures", fetchSkinTexture(playerName, "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGZiYmEzZTljYzdh", "OWUxODg5N2Y5Yzk4ZmEzODBkMGE4YzJjNzliMWViYjcxMWJlMjJiNDEzNDk5M2ZjZWQzMSJ9fX0="));
            }
            try {
                Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(skullMeta, profile);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }

            head.setItemMeta(skullMeta);
        }
        return head;
    }
//
    private Property fetchSkinTexture(String playerName,String t1,String t2) {
        // Fetch the player's GameProfile from Mojang's servers
        GameProfile profile = new GameProfile(UUID.randomUUID(), playerName);
        // For simplicity, this example assumes a default texture
        // You would need an actual way to fetch player skin textures for this to work dynamically.

        // Dummy texture example, replace with real fetching in production
        String dummyTexture = t1+t2;
        return new Property("textures", dummyTexture);
    }
    public void openConversionMenu(Player player) {
        Inventory conversionMenu = Bukkit.createInventory(null, 27, "Arcade Currency Conversion");

        // Add conversion button
        ItemStack convertButton = getCustomHead(player.getName(), "§6Convert AC to BedWars Economy", 2);
        ItemMeta convertMeta = convertButton.getItemMeta();
        String cr = "§6Currently Conversion rate: " + econ.getConversionRate();
        convertMeta.setLore(Collections.singletonList(cr));
        convertButton.setItemMeta(convertMeta);
        conversionMenu.setItem(0, convertButton);

        // Display player's Arcade Coins
        double arcadeCoins = econ.checkCoins(player);
        ItemStack coinsDisplay = getCustomHead(player.getDisplayName(), "§aYour Arcade Coins " + arcadeCoins, 1);
        ItemMeta coinsMeta = coinsDisplay.getItemMeta();
        coinsDisplay.setItemMeta(coinsMeta);
        conversionMenu.setItem(13, coinsDisplay);

        // Display Exit Button
        ItemStack Exit = new ItemStack(Material.BARRIER);
        ItemMeta EXITMeta = Exit.getItemMeta();
        EXITMeta.setDisplayName(ChatColor.RED+"Exit");
        Exit.setItemMeta(EXITMeta);
        conversionMenu.setItem(26, Exit);

        // Fill empty slots with a gray stained glass pane as a filler item
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < conversionMenu.getSize(); i++) {
            if (conversionMenu.getItem(i) == null || conversionMenu.getItem(i).getType() == Material.AIR) {
                conversionMenu.setItem(i, filler);
            }
        }

        // Open the inventory for the player
        player.openInventory(conversionMenu);
    }


    // Method to open the percentage selection menu
    public void openPercentageSelectionMenu(Player player) {
        Inventory percentageMenu = Bukkit.createInventory(null, 27, "Select Conversion Percentage");

        double arcadeCoins = econ.checkCoins(player);

        // Array of percentages to display
        int[] percentages = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

        // Add percentage options with calculated conversion amounts
        for (int i = 0; i < percentages.length; i++) {
            int percentage = percentages[i];

            // Calculate the amount of Arcade Coins being converted based on the percentage
            double arcadeCoinsToConvert = arcadeCoins * (percentage / 100.0);
            double convertedAmount = arcadeCoinsToConvert / econ.getConversionRate();

            // Create item to represent this percentage option
            ItemStack percentageItem = new ItemStack(Material.PAPER);
            ItemMeta meta = percentageItem.getItemMeta();

            meta.setDisplayName(ChatColor.GREEN + "" + percentage + "% Conversion");
            meta.setLore(Collections.singletonList(
                    ChatColor.GRAY + "Receive " + ChatColor.GOLD + convertedAmount + " BedWars Currency"
            ));
            percentageItem.setItemMeta(meta);

            percentageMenu.setItem(i, percentageItem);
        }

        // Add an exit button
        ItemStack exitButton = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exitButton.getItemMeta();
        exitMeta.setDisplayName(ChatColor.RED + "Exit");
        exitButton.setItemMeta(exitMeta);
        percentageMenu.setItem(25, exitButton);

        // Add a back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Back");
        backButton.setItemMeta(backMeta);
        percentageMenu.setItem(26, backButton);

        // Fill remaining slots with a filler item
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < percentageMenu.getSize(); i++) {
            if (percentageMenu.getItem(i) == null) {
                percentageMenu.setItem(i, filler);
            }
        }

        // Open the percentage selection menu for the player
        player.openInventory(percentageMenu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().equals("Arcade Currency Conversion")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            if (event.getCurrentItem().getType() == Material.BARRIER) player.closeInventory();

            // Check for conversion button click
            if (event.getCurrentItem().getType() == Material.PLAYER_HEAD && Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getDisplayName().equals("§6Convert AC to BedWars Economy")) {
                openPercentageSelectionMenu(player); // Open percentage selection when convert is clicked
            }
        } else if (event.getView().getTitle().equals("Select Conversion Percentage")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            if (event.getCurrentItem().getType() == Material.BARRIER) player.closeInventory();

            // Handle percentage selection
            if (event.getCurrentItem().getType() == Material.PAPER) {
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                int percentage = Integer.parseInt(itemName.replaceAll("[^0-9]", ""));
                double arcadeCoins = econ.checkCoins(player);
                double amountToConvert = arcadeCoins * (percentage / 100.0);

                if (amountToConvert >= econ.getConversionRate()) {
                    convertCurrency(player, amountToConvert);
                } else {
                    player.sendMessage("§cYou need at least " + econ.getConversionRate() + " Arcade Coins to convert this amount!");
                }
                player.closeInventory();
            }
        }
    }

    private void convertCurrency(Player player, double arcadeCoins) {
        double convertedAmount = arcadeCoins / econ.getConversionRate();
        econ.removeCoins(player, arcadeCoins);
        econ.getCustomCurrencyManager().addCurrency(player, convertedAmount);
        player.sendMessage("§aConverted " + arcadeCoins + " Arcade Coins into " + convertedAmount + " BedWars Currency!");
    }
}
