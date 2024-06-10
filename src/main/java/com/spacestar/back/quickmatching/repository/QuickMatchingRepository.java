package com.spacestar.back.quickmatching.repository;

import com.spacestar.back.quickmatching.domain.QuickMatching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuickMatchingRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY = "QuickMatching";

    public QuickMatchingRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(QuickMatching quickMatching) {
        System.out.println("quickMatching.getId() = " + quickMatching.getId());
        redisTemplate.opsForZSet().add(KEY,  quickMatching,System.currentTimeMillis());
    }

    public QuickMatching findById(String id) {
        return (QuickMatching) redisTemplate.opsForHash().get(KEY, id);
    }

    public void delete(String id) {
        redisTemplate.opsForHash().delete(KEY, id);
    }
}
