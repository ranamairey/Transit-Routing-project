package com.example.demo.transit.service;

import com.example.demo.transit.domain.RouteStatus;
import com.example.demo.transit.dto.PushSubscribeRequest;
import com.example.demo.transit.entity.PushSubscriptionRecord;
import com.example.demo.transit.entity.User;
import com.example.demo.transit.repository.PushSubscriptionRepository;
import com.example.demo.transit.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Web Push (VAPID) alerts for riders who follow a line. Disabled unless VAPID_PUBLIC_KEY and
 * VAPID_PRIVATE_KEY are set. Sending happens off the request thread; dead subscriptions are removed.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionRepository subscriptions;
    private final UserRepository users;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String publicKey;
    private final String publicUrl;
    private final PushService push;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "web-push");
        t.setDaemon(true);
        return t;
    });

    public PushNotificationService(PushSubscriptionRepository subscriptions, UserRepository users, ObjectMapper objectMapper,
                                   Clock clock,
                                   @Value("${app.push.public-key:}") String publicKey,
                                   @Value("${app.push.private-key:}") String privateKey,
                                   @Value("${app.push.subject:mailto:admin@example.com}") String subject,
                                   @Value("${app.public-url:}") String publicUrl) {
        this.subscriptions = subscriptions;
        this.users = users;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.publicKey = publicKey == null ? "" : publicKey.trim();
        this.publicUrl = publicUrl;
        PushService service = null;
        if (!this.publicKey.isEmpty() && privateKey != null && !privateKey.isBlank()) {
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
                service = new PushService(this.publicKey, privateKey.trim(), subject);
                log.info("Web Push alerts enabled");
            } catch (Exception e) {
                log.warn("Web Push disabled: invalid VAPID keys ({})", e.getMessage());
            }
        }
        this.push = service;
    }

    public boolean enabled() {
        return push != null;
    }

    public String publicKey() {
        return enabled() ? publicKey : null;
    }

    public void subscribe(String email, PushSubscribeRequest request) {
        PushSubscriptionRecord record = subscriptions.findByEndpoint(request.endpoint()).orElseGet(PushSubscriptionRecord::new);
        record.setEndpoint(request.endpoint());
        record.setP256dh(request.p256dh());
        record.setAuth(request.auth());
        record.setUserEmail(email);
        record.setCreatedAt(clock.instant());
        subscriptions.save(record);
    }

    public void unsubscribe(String endpoint) {
        subscriptions.deleteByEndpoint(endpoint);
    }

    @EventListener
    public void onRouteStatusChanged(RouteStatusChangedEvent event) {
        if (!enabled() || event.status() == null || event.status() == RouteStatus.NORMAL) {
            return;
        }
        executor.submit(() -> notifyFollowers(event));
    }

    /** Sends one alert to every subscription of every rider following the line; returns how many were delivered. */
    int notifyFollowers(RouteStatusChangedEvent event) {
        List<String> emails = users.findByFollowedRouteIdsContaining(event.routeId()).stream().map(User::getEmail).toList();
        if (emails.isEmpty()) {
            return 0;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("routeId", event.routeId());
        payload.put("routeName", event.routeName());
        payload.put("status", event.status().name());
        payload.put("fare", event.fare());
        payload.put("url", (publicUrl == null ? "" : publicUrl.replaceAll("/$", "")) + "/lines/" + event.routeId());
        int sent = 0;
        try {
            byte[] body = objectMapper.writeValueAsBytes(payload);
            for (PushSubscriptionRecord s : subscriptions.findByUserEmailIn(emails)) {
                if (send(s, body)) {
                    sent++;
                }
            }
        } catch (Exception e) {
            log.warn("Push alert for line {} failed: {}", event.routeId(), e.getMessage());
        }
        return sent;
    }

    private boolean send(PushSubscriptionRecord s, byte[] body) {
        try {
            HttpResponse response = push.send(new Notification(s.getEndpoint(), s.getP256dh(), s.getAuth(), body));
            int code = response.getStatusLine().getStatusCode();
            if (code == 404 || code == 410) {
                subscriptions.deleteByEndpoint(s.getEndpoint());
                return false;
            }
            return code >= 200 && code < 300;
        } catch (Exception e) {
            log.debug("Push to {} failed: {}", s.getEndpoint(), e.getMessage());
            return false;
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
