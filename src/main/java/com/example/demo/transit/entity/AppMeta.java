package com.example.demo.transit.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Small key/value records about the deployment itself (for example which dataset version was synced). */
@Document("app_meta")
@Getter
@Setter
@NoArgsConstructor
public class AppMeta {
    @Id
    private String id;
    private String value;
    private Instant updatedAt;
}
