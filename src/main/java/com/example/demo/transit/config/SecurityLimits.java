package com.example.demo.transit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.limits")
public class SecurityLimits {
    /** Failed/any login attempts allowed per email within the window. */
    private int loginAttempts = 10;
    private int loginWindowMinutes = 15;
    /** Minutes a user must wait before reporting the same route again. */
    private int reportCooldownMinutes = 10;
    /** Reports a user may submit per hour across all routes. */
    private int reportsPerHour = 20;
}
