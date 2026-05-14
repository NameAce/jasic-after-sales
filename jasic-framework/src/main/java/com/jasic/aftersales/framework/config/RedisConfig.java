package com.jasic.aftersales.framework.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate，使用 Jackson 序列化
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 调用setConnectionFactory方法，复用统一能力并保证业务规则一致。
        template.setConnectionFactory(connectionFactory);

        // 调用ObjectMapper方法，复用统一能力并保证业务规则一致。
        ObjectMapper objectMapper = new ObjectMapper();
        // 调用setVisibility方法，复用统一能力并保证业务规则一致。
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 调用activateDefaultTyping方法，复用统一能力并保证业务规则一致。
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        // 调用JavaTimeModule方法，复用统一能力并保证业务规则一致。
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        // 调用setObjectMapper方法，复用统一能力并保证业务规则一致。
        jacksonSerializer.setObjectMapper(objectMapper);

        // 调用StringRedisSerializer方法，复用统一能力并保证业务规则一致。
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // 调用setKeySerializer方法，复用统一能力并保证业务规则一致。
        template.setKeySerializer(stringSerializer);
        // 调用setHashKeySerializer方法，复用统一能力并保证业务规则一致。
        template.setHashKeySerializer(stringSerializer);
        // 调用setValueSerializer方法，复用统一能力并保证业务规则一致。
        template.setValueSerializer(jacksonSerializer);
        // 调用setHashValueSerializer方法，复用统一能力并保证业务规则一致。
        template.setHashValueSerializer(jacksonSerializer);
        // 调用afterPropertiesSet方法，复用统一能力并保证业务规则一致。
        template.afterPropertiesSet();

        return template;
    }
}
