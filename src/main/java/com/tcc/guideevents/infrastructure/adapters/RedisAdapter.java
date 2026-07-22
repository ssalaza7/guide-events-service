package com.tcc.guideevents.infrastructure.adapters;

import com.tcc.guideevents.domain.eventos.ProcessedEventStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisAdapter implements ProcessedEventStore {

    private static final String KEY_PREFIX = "guide-events:processed:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Duration retention;

    public RedisAdapter(
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${guide-events.idempotency.retention-minutes:60}") long retentionMinutes) {
        this.redisTemplate = redisTemplate;
        this.retention = Duration.ofMinutes(retentionMinutes);
    }

    @Override
    public Mono<Boolean> reserve(String eventId) {
        return redisTemplate.opsForValue().setIfAbsent(key(eventId), "1", retention);
    }

    @Override
    public Mono<Void> release(String eventId) {
        return redisTemplate.opsForValue().delete(key(eventId)).then();
    }

    private String key(String eventId) {
        return KEY_PREFIX + eventId;
    }
}
