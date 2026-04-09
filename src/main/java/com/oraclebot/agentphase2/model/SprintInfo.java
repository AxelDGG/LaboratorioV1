package com.oraclebot.agentphase2.model;

import java.time.LocalDate;

public class SprintInfo {

    private final String name;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public SprintInfo(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}

