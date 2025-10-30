package com.example.chatserver.common.configs;

import com.example.chatserver.chat.service.RedisPubSubService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis의 Pub/Sub (발행/구독) 기능을 사용하기 위한 설정 클래스.
 *
 * Pub/Sub은 데이터를 저장하지 않고,
 * 채널(channel)을 통해 메시지를 발행(Publish)하면
 * 그 채널을 구독(Subscribe) 중인 다른 서버나 세션이 메시지를 실시간으로 받는 구조이다.
 *
 * 이 설정에서는 다음을 수행한다.
 *  - LettuceConnectionFactory로 Redis 연결 생성
 *  - StringRedisTemplate으로 문자열 기반 Publish 지원
 *  - RedisMessageListenerContainer로 Subscribe 기능 구성
 *  - MessageListenerAdapter로 수신 메시지 처리 메서드 지정
 */
public class RedisConfig {

    // application.yml에서 Redis 접속정보 주입
    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    /**
     * Redis 연결용 기본 Bean
     *
     * LettuceConnectionFactory를 통해 Redis 서버와 연결을 생성한다.
     * RedisConnectionFactory는 Spring이 Redis를 다룰 때 사용하는 인터페이스이다.
     *
     * @Qualifier("chatPubSub"): 여러 Redis 연결을 사용할 경우 이름으로 구분하기 위한 용도.
     */
    @Bean
    @Qualifier("chatPubSub")
    public RedisConnection chatPubSubFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);

        // Pub/Sub은 특정 DB index에 종속되지 않으므로 database 설정은 생략한다.
        // configuration.setDatabase(0);

        // Lettuce는 비동기 I/O 기반의 Redis 클라이언트로, Spring Boot의 기본 Redis 클라이언트이다.
        return new LettuceConnectionFactory(configuration).getConnection();
    }

    /**
     * Redis Publish용 Template Bean
     *
     * RedisTemplate<keyType, valueType>을 기반으로 동작하며,
     * 여기서는 문자열 메시지 전송만 필요하므로 StringRedisTemplate을 사용한다.
     *
     * publish 시 "template.convertAndSend(channel, message)" 형태로 메시지를 전송한다.
     */
    @Bean
    @Qualifier("chatPubSub")
    public StringRedisTemplate stringRedisTemplate(
            @Qualifier("chatPubSub") RedisConnectionFactory redisConnectionFactory) {

        return new StringRedisTemplate(redisConnectionFactory);
    }

    /**
     * Redis Subscribe용 Listener Container
     *
     * RedisMessageListenerContainer는 Redis의 채널을 구독하는 역할을 한다.
     * 지정한 채널("chat")에서 메시지가 들어오면,
     * 연결된 MessageListenerAdapter를 통해 비즈니스 로직이 호출된다.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            @Qualifier("chatPubSub") RedisConnectionFactory redisConnectionFactory,
            MessageListenerAdapter messageListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, new PatternTopic("chat"));
        return container;
    }

    /**
     * 메시지 수신 처리 어댑터
     *
     * Redis에서 "chat" 채널에 메시지가 도착하면,
     * 지정된 Bean(RedisPubSubService)의 특정 메서드("onMessage")를 자동으로 호출한다.
     *
     * Redis → Spring으로 전달되는 메시지를 비즈니스 로직과 연결해주는 어댑터 역할을 한다.
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisPubSubService redisPubSubService) {
        // RedisPubSubService 클래스의 onMessage(String message) 메서드가 자동 호출된다.
        return new MessageListenerAdapter(redisPubSubService, "onMessage");
    }
}
