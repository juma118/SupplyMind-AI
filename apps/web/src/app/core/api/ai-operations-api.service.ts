import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Investigation {
  id: string;
  incidentType: string;
  status: string;
  severity: string | null;
  sourceReference: string;
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
}

export interface Recommendation {
  id: string;
  investigationId: string;
  summary: string;
  estimatedCost: number | null;
  status: string;
  actionType: string;
  actionProductSku: string | null;
  actionQuantity: number | null;
  actionSourceWarehouse: string | null;
  actionDestWarehouse: string | null;
  actionSupplierId: string | null;
  createdAt: string;
}

export interface AuditLogEntry {
  id: string;
  actor: string;
  action: string;
  entityType: string;
  entityId: string;
  details: string | null;
  occurredAt: string;
}

export interface AgentRun {
  id: string;
  agentName: string;
  status: string;
  mode: string | null;
  provider: string | null;
  model: string | null;
  output: string | null;
  errorMessage: string | null;
  startedAt: string;
  finishedAt: string | null;
}

export interface Evidence {
  id: string;
  sourceType: string;
  sourceId: string;
  description: string;
  createdAt: string;
}

export interface InvestigationDetail {
  investigation: Investigation;
  agentRuns: AgentRun[];
  evidence: Evidence[];
  recommendations: Recommendation[];
}

@Injectable({ providedIn: 'root' })
export class AiOperationsApiService {
  private readonly baseUrl = `${environment.gatewayUrl}/ai-operations-service/api/v1`;

  constructor(private readonly http: HttpClient) {}

  listInvestigations(): Observable<Investigation[]> {
    return this.http.get<Investigation[]>(`${this.baseUrl}/investigations`);
  }

  getInvestigationDetail(investigationId: string): Observable<InvestigationDetail> {
    return this.http.get<InvestigationDetail>(`${this.baseUrl}/investigations/${investigationId}/timeline`);
  }

  getRecommendations(investigationId: string): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.baseUrl}/investigations/${investigationId}/recommendations`);
  }

  listPendingRecommendations(): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.baseUrl}/recommendations`);
  }

  listAuditLogs(): Observable<AuditLogEntry[]> {
    return this.http.get<AuditLogEntry[]>(`${this.baseUrl}/audit-logs`);
  }

  // actorId is not sent - the Gateway-forwarded JWT identifies the caller server-side
  // (see ApprovalController); a client-claimed actor would be as untrustworthy as the
  // old client-asserted tenant header.
  approve(recommendationId: string, comment: string): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.baseUrl}/recommendations/${recommendationId}/approve`, { comment });
  }

  reject(recommendationId: string, comment: string): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.baseUrl}/recommendations/${recommendationId}/reject`, { comment });
  }
}
