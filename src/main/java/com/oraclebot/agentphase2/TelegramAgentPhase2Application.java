package com.oraclebot.agentphase2;

import com.oraclebot.agentphase2.config.AiProps;
import com.oraclebot.agentphase2.config.BotProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({BotProps.class, AiProps.class})
public class TelegramAgentPhase2Application {

    public static void main(String[] args) {
        SpringApplication.run(TelegramAgentPhase2Application.class, args);
    }
}

