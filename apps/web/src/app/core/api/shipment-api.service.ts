import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Shipment {
  id: string;
  shipmentCode: string;
  carrier: string | null;
  origin: string | null;
  destinationWarehouse: string;
  productSku: string;
  quantity: number;
  avgDailyDemand: number | null;
  status: string;
  eta: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ShipmentEvent {
  id: string;
  eventType: string;
  description: string | null;
  delayHours: number | null;
  occurredAt: string;
}

export interface OnTimeRate {
  totalDelivered: number;
  onTimeDelivered: number;
  onTimeRate: number | null;
}

@Injectable({ providedIn: 'root' })
export class ShipmentApiService {
  private readonly baseUrl = `${environment.gatewayUrl}/shipment-service/api/v1`;

  constructor(private readonly http: HttpClient) {}

  listShipments(): Observable<Shipment[]> {
    return this.http.get<Shipment[]>(`${this.baseUrl}/shipments`);
  }

  listAtRisk(): Observable<Shipment[]> {
    return this.http.get<Shipment[]>(`${this.baseUrl}/shipments/at-risk`);
  }

  getTimeline(shipmentId: string): Observable<ShipmentEvent[]> {
    return this.http.get<ShipmentEvent[]>(`${this.baseUrl}/shipments/${shipmentId}/timeline`);
  }

  getOnTimeRate(): Observable<OnTimeRate> {
    return this.http.get<OnTimeRate>(`${this.baseUrl}/shipments/analytics/on-time-rate`);
  }

  createShipment(body: {
    shipmentCode: string;
    carrier?: string;
    origin?: string;
    destinationWarehouse: string;
    productSku: string;
    quantity: number;
    avgDailyDemand?: number;
  }): Observable<Shipment> {
    return this.http.post<Shipment>(`${this.baseUrl}/shipments`, body);
  }

  addEvent(
    shipmentId: string,
    body: { eventType: 'BOOKED' | 'DEPARTED' | 'DELAYED' | 'ARRIVED'; description?: string; delayHours?: number },
  ): Observable<ShipmentEvent> {
    return this.http.post<ShipmentEvent>(`${this.baseUrl}/shipments/${shipmentId}/events`, body);
  }
}
