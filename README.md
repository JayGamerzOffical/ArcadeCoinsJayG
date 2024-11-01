# ArcadeCoins
![ArcadeCoins Logo](./images/b/Arcade%20Coins%20-%20Custom%20Currency%20&%20Conversion%20System.jpg)  <!-- Replace with your actual image link -->

ArcadeCoins by Jay is a powerful Minecraft plugin designed to manage an in-game currency system, allowing players to earn, spend, and track their coins efficiently. This plugin integrates with a MySQL database to store player coin balances and features a user-friendly command interface.

## Images -------------------
![Images](./images/Screenshot%202024-11-01%20064054.png)

  ![Screenshot 2024-11-01 064136](./images/Screenshot%202024-11-01%20064136.png)


  ![Screenshot 2024-11-01 064146](./images/Screenshot%202024-11-01%20064146.png)
  ![Screenshot 2024-11-01 064146](./images/Screenshot%202024-11-01%20064241.png)


  ![Screenshot 2024-11-01 064241](./images/Screenshot%202024-11-01%20064302.png)


  ![Screenshot 2024-11-01 064302](./images/Screenshot%202024-11-01%20064310.png)


  ![Screenshot 2024-11-01 064310](./images/Screenshot%202024-11-01%20064318.png)
  ![Screenshot 2024-11-01 064310](./images/Screenshot%202024-11-01%20064325.png)
  ![Screenshot 2024-11-01 064310](./images/Screenshot%202024-11-01%20064337.png)
  ![Screenshot 2024-11-01 064310](./images/Screenshot%202024-11-01%20064346.png)
  ![Screenshot 2024-11-01 064310](./images/Screenshot%202024-11-01%20064504.png)

### **So, Now Please Break the button of star...*..**

## Features
- **Coin Management:** Players can earn, spend, and check their coin balances.
- **MySQL Integration:** Store player data in a MySQL database for persistence.
- **Daily Rewards:** Players can claim daily rewards to enhance engagement.
- **Gift System:** Players can share their coins with other players.
- **Custom Currency:** Easily configure conversion rates and custom currency settings.
- **Leaderboard:** Display the top players based on their coin balances.
- **Dynamic Message System:** Supports customizable messages via a configuration file.
- **Timezone Support:** Claims and rewards adjust based on player timezone.

## Installation
1. Download the latest release of ArcadeCoinsJayG from the plugin store.
2. Place the downloaded `.jar` file into your server's plugins directory.

### Config.yml
```yaml
# MySQL Database Configuration
mysql:
  host: "your_mysql_host"         # Replace with your MySQL host address
  port: 3306                       # Default MySQL port
  database: "your_database_name"   # Name of your database
  username: "your_username"        # Your database username
  password: "your_password"        # Your database password

# Time Zone Configuration
# If you don't know your time zone then search on Google: <your country name or your state> time zone
timezone: "Asia/Jerusalem"      # Set your desired time zone (e.g., America/New_York)

# Daily Rewards Configuration
dailyRewardAmount: 100.0           # Amount awarded to players for daily login rewards

# This will show in Leaderboard
YourServerName: "YourNetwork"

# Currency Conversion Rates
ConversionRate:
  BedWars: 10                      # Conversion rate for BedWars to Arcade Coins
```
## Messages.yml
```yaml
currency:
  added: "You have received {amount} BedWars currency! Your new balance is: {newBalance} currency."
  removed: "You have successfully removed {amount} BedWars currency. Your new balance is: {newBalance} currency."
  insufficient: "§cYou do not have enough BedWars currency to remove {amount}. Your current balance is: {currentBalance} currency."
  balance: "Your current BedWars currency balance is: {balance} currency."
```
Commands
Main Command:
/coins - Main command for arcade coins.
Subcommands:
/coins balance - Show your coin balance.

/coins add <player> <amount> - Add coins to a player.
Example: /coins add PlayerName 50 (adds 50 coins to PlayerName's balance)

/coins remove <player> <amount> - Remove coins from a player.
Example: /coins remove PlayerName 20 (removes 20 coins from PlayerName's balance)

/coins set <player> <amount> - Set a player's coin balance.

/coins check <player> - Check a player's coin balance.

/coins convert - Open the currency conversion menu.

/coins leaderboard - Show the leaderboard for coin holders.

/coins dailyreward - Claim your daily reward of coins.
Note: Players can only claim their daily reward once per day.

/coins bwcoins - Access BedWars coins.

/coins shop - Open the coins shop.