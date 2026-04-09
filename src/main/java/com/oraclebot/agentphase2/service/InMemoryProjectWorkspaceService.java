package com.oraclebot.agentphase2.service;

import com.oraclebot.agentphase2.model.SprintInfo;
import com.oraclebot.agentphase2.model.TaskItem;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class InMemoryProjectWorkspaceService implements ProjectWorkspaceService {

    private final AtomicLong sequence = new AtomicLong(100);
    private final List<TaskItem> tasks = new CopyOnWriteArrayList<>();
    private SprintInfo currentSprint;

    @PostConstruct
    void seedData() {
        currentSprint = new SprintInfo("Sprint 2", LocalDate.now().minusDays(3), LocalDate.now().plusDays(11));
        tasks.add(new TaskItem(sequence.incrementAndGet(), "Definir backlog priorizado", "Ana", "DONE", 3, currentSprint.getName(), LocalDate.now().plusDays(1)));
        tasks.add(new TaskItem(sequence.incrementAndGet(), "Implementar servicio de tareas", "Luis", "IN_PROGRESS", 8, currentSprint.getName(), LocalDate.now().plusDays(2)));
        tasks.add(new TaskItem(sequence.incrementAndGet(), "Crear reporte de sprint", "Maria", "PENDING", 5, currentSprint.getName(), LocalDate.now().plusDays(4)));
        tasks.add(new TaskItem(sequence.incrementAndGet(), "Configurar bot de Telegram", "Luis", "PENDING", 3, currentSprint.getName(), LocalDate.now().plusDays(3)));
    }

    @Override
    public List<TaskItem> findAllTasks() {
        return tasks.stream()
            .sorted(Comparator.comparing(TaskItem::getId))
            .toList();
    }

    @Override
    public List<TaskItem> findTasksByAssignee(String assignee) {
        String normalized = normalize(assignee);
        return tasks.stream()
            .filter(task -> normalize(task.getAssignee()).equals(normalized))
            .sorted(Comparator.comparing(TaskItem::getId))
            .toList();
    }

    @Override
    public List<TaskItem> findTasksByStatus(String status) {
        String normalized = normalizeStatus(status);
        return tasks.stream()
            .filter(task -> normalizeStatus(task.getStatus()).equals(normalized))
            .sorted(Comparator.comparing(TaskItem::getId))
            .toList();
    }

    @Override
    public TaskItem createTask(String title, String assignee, int storyPoints, String sprintName) {
        String safeAssignee = (assignee == null || assignee.isBlank()) ? "Sin asignar" : capitalize(assignee);
        String safeSprint = (sprintName == null || sprintName.isBlank()) ? currentSprint.getName() : sprintName;
        int safePoints = storyPoints <= 0 ? 3 : storyPoints;
        TaskItem task = new TaskItem(
            sequence.incrementAndGet(),
            title,
            safeAssignee,
            "PENDING",
            safePoints,
            safeSprint,
            LocalDate.now().plusDays(5)
        );
        tasks.add(task);
        return task;
    }

    @Override
    public SprintInfo getCurrentSprint() {
        return currentSprint;
    }

    @Override
    public Map<String, Integer> storyPointsByAssignee() {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (TaskItem task : tasks) {
            totals.merge(task.getAssignee(), task.getStoryPoints(), Integer::sum);
        }
        return totals;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeStatus(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "pendiente", "pending" -> "PENDING";
            case "en progreso", "in progress", "in_progress" -> "IN_PROGRESS";
            case "done", "hecha", "terminada" -> "DONE";
            default -> normalized.toUpperCase(Locale.ROOT);
        };
    }

    private String capitalize(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }
}

