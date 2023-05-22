package com.yang.reggie.config;


import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig extends CachingConfigurerSupport {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

    // 设置键的序列化器
    redisTemplate.setKeySerializer(new StringRedisSerializer());

    redisTemplate.setHashKeySerializer(new StringRedisSerializer());

    redisTemplate.setConnectionFactory(connectionFactory);

    // 设置值的序列化器（可根据需要选择合适的序列化器）
    // redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    // redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

    // redisTemplate.afterPropertiesSet();

    return redisTemplate;
  }
}
