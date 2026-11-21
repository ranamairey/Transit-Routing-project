package com.example.demo.transit.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A browser's Web Push subscription, owned by a signed-in rider. */
@Document("push_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class PushSubscriptionRecord {
    @Id
    private String id;
    @Indexed(unique = true)
    private String endpoint;
    private String p256dh;
    private String auth;
    @Indexed
    private String userEmail;
    private Instant createdAt;
}
