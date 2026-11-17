package com.supplymind.aiops.agent;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_agent_runs")
public class AiAgentRun {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "investigation_id", nullable = false)
    private UUID investigationId;

    @Column(name = "agent_name", nullable = false)
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentRunStatus status;

    @Enumerated(EnumType.STRING)
    private AiMode mode;

    private String provider;

    private String model;

    @Column(columnDefinition = "text")
    private String output;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getInvestigationId() {
        return investigationId;
    }

    public void setInvestigationId(UUID investigationId) {
        this.investigationId = investigationId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public AgentRunStatus getStatus() {
        return status;
    }

    public void setStatus(AgentRunStatus status) {
        this.status = status;
    }

    public AiMode getMode() {
        return mode;
    }

    public void setMode(AiMode mode) {
        this.mode = mode;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
