package com.oraclebot.agentphase2.bot;

import com.oraclebot.agentphase2.agent.AgentOrchestrator;
import com.oraclebot.agentphase2.config.BotProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class TelegramAgentBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TelegramAgentBot.class);

    private final BotProps botProps;
    private final AgentOrchestrator orchestrator;
    private final TelegramClient telegramClient;

    public TelegramAgentBot(BotProps botProps, AgentOrchestrator orchestrator) {
        this.botProps = botProps;
        this.orchestrator = orchestrator;
        this.telegramClient = new OkHttpTelegramClient(botProps.getToken());
    }

    @Override
    public String getBotToken() {
        return botProps.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String requestText = update.getMessage().getText();
        String responseText = orchestrator.handleMessage(requestText);

        try {
            telegramClient.execute(
                SendMessage.builder()
                    .chatId(chatId)
                    .text(responseText)
                    .build()
            );
        } catch (Exception ex) {
            logger.error("No pude responder al chat {}", chatId, ex);
        }
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        logger.info("Agent bot registrado. running={}", botSession.isRunning());
    }
}
