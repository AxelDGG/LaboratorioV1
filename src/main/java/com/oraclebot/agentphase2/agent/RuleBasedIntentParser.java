package com.oraclebot.agentphase2.agent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedIntentParser implements IntentParser {

    private static final Pattern CREATE_TASK_PATTERN =
        Pattern.compile("crea(?:r)? una tarea para (.+?)(?: y asigna(?:la)? a ([a-zA-ZáéíóúÁÉÍÓÚñÑ ]+))?(?: con (\\d+) puntos?)?$", Pattern.CASE_INSENSITIVE);

    @Override
    public ParsedIntent parse(String messageText) {
        String text = messageText == null ? "" : messageText.trim();
        String normalized = text.toLowerCase(Locale.ROOT);

        ParsedIntent intent = new ParsedIntent();

        if (normalized.equals("/start") || normalized.equals("ayuda") || normalized.equals("/help")) {
            intent.setIntent(IntentType.HELP);
            return intent;
        }

        if (normalized.contains("sprint actual") || normalized.contains("como va el sprint")) {
            intent.setIntent(IntentType.CURRENT_SPRINT_SUMMARY);
            return intent;
        }

        if (normalized.contains("quien tiene mas carga") || normalized.contains("carga del equipo")) {
            intent.setIntent(IntentType.TEAM_LOAD_SUMMARY);
            return intent;
        }

        if (normalized.contains("tareas tiene ")) {
            intent.setIntent(IntentType.LIST_TASKS_BY_ASSIGNEE);
            String assignee = normalized.substring(normalized.indexOf("tareas tiene ") + "tareas tiene ".length()).trim();
            intent.setAssignee(capitalize(assignee));
            return intent;
        }

        if (normalized.contains("tareas siguen ") || normalized.contains("tareas pendientes") || normalized.contains("tareas done")) {
            intent.setIntent(IntentType.LIST_TASKS_BY_STATUS);
            if (normalized.contains("pendientes")) {
                intent.setStatus("PENDING");
            } else if (normalized.contains("done")) {
                intent.setStatus("DONE");
            } else if (normalized.contains("progreso")) {
                intent.setStatus("IN_PROGRESS");
            } else {
                intent.setStatus("PENDING");
            }
            return intent;
        }

        if (normalized.equals("/todolist") || normalized.equals("lista de tareas")) {
            intent.setIntent(IntentType.LIST_TASKS);
            return intent;
        }

        Matcher createMatcher = CREATE_TASK_PATTERN.matcher(text);
        if (createMatcher.find()) {
            intent.setIntent(IntentType.CREATE_TASK);
            intent.setTitle(createMatcher.group(1) == null ? null : createMatcher.group(1).trim());
            intent.setAssignee(createMatcher.group(2) == null ? null : capitalize(createMatcher.group(2).trim()));
            intent.setStoryPoints(createMatcher.group(3) == null ? null : Integer.parseInt(createMatcher.group(3)));
            if (intent.getTitle() == null || intent.getTitle().isBlank()) {
                intent.setClarificationNeeded(true);
                intent.setClarificationQuestion("Necesito el titulo de la tarea para poder crearla.");
            }
            return intent;
        }

        intent.setIntent(IntentType.UNKNOWN);
        intent.setClarificationNeeded(true);
        intent.setClarificationQuestion("No entendi la solicitud. Puedes pedir ayuda, consultar tareas, crear tareas o pedir resumen del sprint.");
        return intent;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }
}

