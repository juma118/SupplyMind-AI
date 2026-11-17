package com.supplymind.aiops.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentRunRepository extends JpaRepository<AiAgentRun, UUID> {

    List<AiAgentRun> findAllByInvestigationIdOrderByStartedAtAsc(UUID investigationId);
}
