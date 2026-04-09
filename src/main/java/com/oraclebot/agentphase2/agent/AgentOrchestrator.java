package com.oraclebot.agentphase2.agent;

import com.oraclebot.agentphase2.model.SprintInfo;
import com.oraclebot.agentphase2.model.TaskItem;
import com.oraclebot.agentphase2.service.ProjectWorkspaceService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

@Component
public class AgentOrchestrator {

    private final LlmIntentParser llmIntentParser;
    private final ProjectWorkspaceService workspaceService;

    public AgentOrchestrator(LlmIntentParser llmIntentParser, ProjectWorkspaceService workspaceService) {
        this.llmIntentParser = llmIntentParser;
        this.workspaceService = workspaceService;
    }

    public String handleMessage(String messageText) {
        ParsedIntent parsedIntent = llmIntentParser.parse(messageText);

        if (parsedIntent.isClarificationNeeded()
            && parsedIntent.getIntent() != IntentType.UNKNOWN
            && parsedIntent.getIntent() != IntentType.GENERAL_RESPONSE) {
            return parsedIntent.getClarificationQuestion();
        }

        return switch (parsedIntent.getIntent()) {
            case GENERAL_RESPONSE -> parsedIntent.getResponseText() == null || parsedIntent.getResponseText().isBlank()
                ? llmIntentParser.answerGeneralQuestion(
                    messageText,
                    workspaceService.getCurrentSprint(),
                    workspaceService.findAllTasks(),
                    workspaceService.storyPointsByAssignee()
                )
                : parsedIntent.getResponseText();
            case HELP -> helpText();
            case LIST_TASKS -> formatTasks("Estas son las tareas registradas:", workspaceService.findAllTasks());
            case LIST_TASKS_BY_ASSIGNEE -> formatTasks(
                "Estas son las tareas de " + safe(parsedIntent.getAssignee()) + ":",
                workspaceService.findTasksByAssignee(parsedIntent.getAssignee())
            );
            case LIST_TASKS_BY_STATUS -> formatTasks(
                "Estas son las tareas con estado " + safe(parsedIntent.getStatus()) + ":",
                workspaceService.findTasksByStatus(parsedIntent.getStatus())
            );
            case CREATE_TASK -> createTask(parsedIntent);
            case CURRENT_SPRINT_SUMMARY -> sprintSummary();
            case TEAM_LOAD_SUMMARY -> teamLoadSummary();
            case UNKNOWN -> llmIntentParser.answerGeneralQuestion(
                messageText,
                workspaceService.getCurrentSprint(),
                workspaceService.findAllTasks(),
                workspaceService.storyPointsByAssignee()
            );
        };
    }

    private String createTask(ParsedIntent parsedIntent) {
        if (parsedIntent.getTitle() == null || parsedIntent.getTitle().isBlank()) {
            return "Necesito el titulo de la tarea para poder crearla.";
        }

        TaskItem task = workspaceService.createTask(
            parsedIntent.getTitle(),
            parsedIntent.getAssignee(),
            parsedIntent.getStoryPoints() == null ? 3 : parsedIntent.getStoryPoints(),
            parsedIntent.getSprintName()
        );

        return """
            Tarea creada correctamente.
            Id: %d
            Titulo: %s
            Responsable: %s
            Estado: %s
            Story points: %d
            Sprint: %s
            """.formatted(
            task.getId(),
            task.getTitle(),
            task.getAssignee(),
            task.getStatus(),
            task.getStoryPoints(),
            task.getSprintName()
        ).trim();
    }

    private String sprintSummary() {
        SprintInfo sprint = workspaceService.getCurrentSprint();
        List<TaskItem> sprintTasks = workspaceService.findAllTasks().stream()
            .filter(task -> sprint.getName().equals(task.getSprintName()))
            .toList();

        long done = sprintTasks.stream().filter(task -> "DONE".equals(task.getStatus())).count();
        long inProgress = sprintTasks.stream().filter(task -> "IN_PROGRESS".equals(task.getStatus())).count();
        long pending = sprintTasks.stream().filter(task -> "PENDING".equals(task.getStatus())).count();
        int totalPoints = sprintTasks.stream().mapToInt(TaskItem::getStoryPoints).sum();

        return """
            Resumen del sprint actual
            Sprint: %s
            Inicio: %s
            Fin: %s
            Tareas: %d
            DONE: %d
            IN_PROGRESS: %d
            PENDING: %d
            Story points totales: %d
            """.formatted(
            sprint.getName(),
            sprint.getStartDate(),
            sprint.getEndDate(),
            sprintTasks.size(),
            done,
            inProgress,
            pending,
            totalPoints
        ).trim();
    }

    private String teamLoadSummary() {
        Map<String, Integer> totals = workspaceService.storyPointsByAssignee();
        if (totals.isEmpty()) {
            return "No hay datos de carga del equipo.";
        }

        StringJoiner joiner = new StringJoiner("\n", "Carga actual del equipo\n", "");
        totals.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
            .forEach(entry -> joiner.add("- " + entry.getKey() + ": " + entry.getValue() + " pts"));

        return joiner.toString().trim();
    }

    private String formatTasks(String title, List<TaskItem> tasks) {
        if (tasks.isEmpty()) {
            return title + "\nNo encontre tareas para ese criterio.";
        }

        StringJoiner joiner = new StringJoiner("\n", title + "\n", "");
        for (TaskItem task : tasks) {
            joiner.add("- %d [%s] %s | %s | %d pts | %s".formatted(
                task.getId(),
                task.getStatus(),
                task.getTitle(),
                task.getAssignee(),
                task.getStoryPoints(),
                task.getSprintName()
            ));
        }
        return joiner.toString().trim();
    }

    private String helpText() {
        return """
            Puedo ayudarte con consultas y acciones del proyecto, y tambien con preguntas generales.

            Ejemplos:
            - que tareas tiene ana
            - que tareas siguen pendientes
            - crea una tarea para revisar la api y asignala a luis con 5 puntos
            - como va el sprint actual
            - quien tiene mas carga
            - como hacer guacamole
            """;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "sin filtro" : value;
    }
}
