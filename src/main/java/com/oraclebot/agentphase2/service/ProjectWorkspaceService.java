package com.oraclebot.agentphase2.service;

import com.oraclebot.agentphase2.model.SprintInfo;
import com.oraclebot.agentphase2.model.TaskItem;
import java.util.List;
import java.util.Map;

public interface ProjectWorkspaceService {

    List<TaskItem> findAllTasks();

    List<TaskItem> findTasksByAssignee(String assignee);

    List<TaskItem> findTasksByStatus(String status);

    TaskItem createTask(String title, String assignee, int storyPoints, String sprintName);

    SprintInfo getCurrentSprint();

    Map<String, Integer> storyPointsByAssignee();
}

