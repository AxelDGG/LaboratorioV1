package com.oraclebot.agentphase2.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oraclebot.agentphase2.config.AiProps;
import com.oraclebot.agentphase2.model.SprintInfo;
import com.oraclebot.agentphase2.model.TaskItem;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LlmIntentParser implements IntentParser {

    private static final Logger logger = LoggerFactory.getLogger(LlmIntentParser.class);

    private final AiProps aiProps;
    private final ObjectMapper objectMapper;
    private final RuleBasedIntentParser fallbackParser;

    public LlmIntentParser(AiProps aiProps, ObjectMapper objectMapper, RuleBasedIntentParser fallbackParser) {
        this.aiProps = aiProps;
        this.objectMapper = objectMapper;
        this.fallbackParser = fallbackParser;
    }

    @Override
    public ParsedIntent parse(String messageText) {
        logger.debug("LlmIntentParser.parse() - Mensaje: {}", messageText);
        logger.debug(
            "LlmIntentParser - AI Enabled: {}, API Key presente: {}",
            aiProps.isEnabled(),
            aiProps.getApiKey() != null && !aiProps.getApiKey().isBlank()
        );

        if (!aiProps.isEnabled() || aiProps.getApiKey() == null || aiProps.getApiKey().isBlank()) {
            logger.warn("LlmIntentParser - LLM deshabilitado o API key vacia. Usando fallback local.");
            return fallbackParser.parse(messageText);
        }

        try {
            String systemPrompt = """
                Eres un router de mensajes para un bot de Telegram.
                Debes responder solo JSON valido.

                Intenciones permitidas:
                HELP
                LIST_TASKS
                LIST_TASKS_BY_ASSIGNEE
                LIST_TASKS_BY_STATUS
                CREATE_TASK
                CURRENT_SPRINT_SUMMARY
                TEAM_LOAD_SUMMARY
                GENERAL_RESPONSE
                UNKNOWN

                Reglas:
                - Usa GENERAL_RESPONSE cuando el usuario haga una pregunta general o conversacional.
                - Usa GENERAL_RESPONSE para temas fuera del proyecto, por ejemplo recetas, cultura general, consejos o explicaciones.
                - Usa las intenciones estructuradas solo cuando la solicitud sea claramente una consulta o accion del sistema agile.
                - Si falta informacion para una accion estructurada, usa clarificationNeeded=true y haz una pregunta clara.
                - No inventes datos del proyecto.

                Devuelve JSON exacto con esta forma:
                {
                  "intent": "GENERAL_RESPONSE",
                  "assignee": null,
                  "status": null,
                  "title": null,
                  "storyPoints": null,
                  "sprintName": null,
                  "clarificationNeeded": false,
                  "clarificationQuestion": "",
                  "responseText": "respuesta final para el usuario"
                }

                Para GENERAL_RESPONSE:
                - escribe la respuesta final en responseText
                - deja clarificationNeeded en false

                Para las intenciones estructuradas:
                - deja responseText en null

                Responde solo JSON, sin markdown ni explicaciones extra.
                """;

            String content = chatCompletion(systemPrompt, messageText, 0);
            if (content == null || content.isBlank()) {
                logger.warn("LlmIntentParser - Respuesta vacia del LLM. Usando fallback.");
                return fallbackParser.parse(messageText);
            }

            String jsonResponse = extractContent(content);
            logger.info("LlmIntentParser - JSON del LLM: {}", jsonResponse);
            ParsedIntent intent = objectMapper.readValue(jsonResponse, ParsedIntent.class);
            logger.info("LlmIntentParser - Intencion clasificada: {}", intent.getIntent());
            return intent;
        } catch (Exception ex) {
            logger.error("LlmIntentParser - ERROR CRITICO en parse()", ex);
            logger.warn("Fallo el parser LLM. Uso fallback local.");
            return fallbackParser.parse(messageText);
        }
    }

    public String generateConversationalResponse(String messageText) {
        logger.debug("LlmIntentParser.generateConversationalResponse() - Generando respuesta conversacional para: {}", messageText);

        if (!aiProps.isEnabled() || aiProps.getApiKey() == null || aiProps.getApiKey().isBlank()) {
            logger.warn("LlmIntentParser - LLM deshabilitado. No hay respuesta conversacional.");
            return null;
        }

        try {
            String systemPrompt = """
                Eres un asistente amable y util.
                Responde preguntas generales de forma clara y concisa.
                Se conversacional, natural y breve.
                No clasifiques ni estructures. Solo responde.
                """;

            String content = chatCompletion(systemPrompt, messageText, 0.4);
            if (content == null || content.isBlank()) {
                logger.warn("LlmIntentParser - Respuesta conversacional vacia del LLM");
                return null;
            }

            String response = extractContent(content);
            logger.info("LlmIntentParser - Respuesta conversacional generada: {}", response);
            return response;
        } catch (Exception ex) {
            logger.error("LlmIntentParser - Error generando respuesta conversacional", ex);
            return null;
        }
    }

    public String answerGeneralQuestion(
        String messageText,
        SprintInfo currentSprint,
        List<TaskItem> tasks,
        Map<String, Integer> teamLoad
    ) {
        String response = generateConversationalResponse(messageText);
        return response == null || response.isBlank()
            ? "No pude generar una respuesta en este momento."
            : response;
    }

    private String chatCompletion(String systemPrompt, String userPrompt, double temperature) throws Exception {
        Map<String, Object> payload = Map.of(
            "model", aiProps.getModel(),
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", temperature
        );

        logger.info("LlmIntentParser - Enviando solicitud a Groq:");
        logger.info("  - URL: {}chat/completions", normalizeBaseUrl(aiProps.getBaseUrl()));
        logger.info("  - Modelo: {}", aiProps.getModel());

        String responseBody = buildClient().post()
            .uri("chat/completions")
            .body(payload)
            .retrieve()
            .body(String.class);

        logger.debug("LlmIntentParser - Response body: {}", responseBody);

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        return content.isMissingNode() ? null : content.asText();
    }

    private RestClient buildClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(Math.max(aiProps.getTimeoutSeconds(), 1));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
            .baseUrl(normalizeBaseUrl(aiProps.getBaseUrl()))
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProps.getApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .requestFactory(requestFactory)
            .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private String extractContent(String rawContent) {
        String trimmed = rawContent == null ? "" : rawContent.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstNewLine = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNewLine < 0 || lastFence <= firstNewLine) {
            return trimmed;
        }

        return trimmed.substring(firstNewLine + 1, lastFence).trim();
    }
}
