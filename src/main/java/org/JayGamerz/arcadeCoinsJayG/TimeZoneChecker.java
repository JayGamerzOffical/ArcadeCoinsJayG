package org.JayGamerz.arcadeCoinsJayG;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.*;

public class TimeZoneChecker {
    private final ZoneId timeZone;

    public TimeZoneChecker(FileConfiguration config) {
        String timeZoneString = config.getString("timezone", "UTC");  // Default to UTC if not set
        timeZone = ZoneId.of(timeZoneString);
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public boolean canClaimDailyReward(LocalDateTime lastClaimTime) {
        LocalDate currentDate = LocalDate.now(timeZone);
        LocalDate lastClaimDate = lastClaimTime.toLocalDate();
        return !lastClaimDate.equals(currentDate);
    }

    public LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now(timeZone);
    }
}
