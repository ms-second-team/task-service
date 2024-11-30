package ru.mssecondteam.taskservice.config;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mssecondteam.taskservice.client.EventClientErrorDecoder;

@Configuration
public class EventClientConfig {

    @Bean
    public ErrorDecoder eventClientErrorDecoder() {
        return new EventClientErrorDecoder();
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
