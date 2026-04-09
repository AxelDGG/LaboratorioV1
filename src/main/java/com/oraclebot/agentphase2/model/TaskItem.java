package com.oraclebot.agentphase2.model;

import java.time.LocalDate;

public class TaskItem {

    private final long id;
    private String title;
    private String assignee;
    private String status;
    private int storyPoints;
    private String sprintName;
    private LocalDate dueDate;

    public TaskItem(long id, String title, String assignee, String status, int storyPoints, String sprintName, LocalDate dueDate) {
        this.id = id;
        this.title = title;
        this.assignee = assignee;
        this.status = status;
        this.storyPoints = storyPoints;
        this.sprintName = sprintName;
        this.dueDate = dueDate;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(int storyPoints) {
        this.storyPoints = storyPoints;
    }

    public String getSprintName() {
        return sprintName;
    }

    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}

