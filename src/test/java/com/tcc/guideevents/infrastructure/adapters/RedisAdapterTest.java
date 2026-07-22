package com.tcc.guideevents.infrastructure.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAdapterTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;
    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private RedisAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisAdapter(redisTemplate, 60);
    }

    @Test
    void reserveReturnsTrueWhenKeyWasAbsent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("guide-events:processed:evt-1"), eq("1"), any())).thenReturn(Mono.just(true));

        StepVerifier.create(adapter.reserve("evt-1")).expectNext(true).verifyComplete();
    }

    @Test
    void reserveReturnsFalseWhenKeyAlreadyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("guide-events:processed:evt-1"), eq("1"), any())).thenReturn(Mono.just(false));

        StepVerifier.create(adapter.reserve("evt-1")).expectNext(false).verifyComplete();
    }

    @Test
    void releaseDeletesTheKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.delete(eq("guide-events:processed:evt-1"))).thenReturn(Mono.just(true));

        StepVerifier.create(adapter.release("evt-1")).verifyComplete();
    }
}
