import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface InventoryBalance {
  id: string;
  warehouseCode: string;
  productSku: string;
  availableUnits: number;
  reservedUnits: number;
  incomingUnits: number;
  damagedUnits: number;
  reorderPoint: number | null;
  avgDailyDemand: number | null;
  leadTimeDays: number | null;
  safetyStock: number | null;
  updatedAt: string;
}

export interface DemandProfileResponse {
  warehouseCode: string;
  productSku: string;
  avgDailyDemand: number;
  leadTimeDays: number;
  safetyStock: number;
  reorderPoint: number;
}

export interface StockoutRisk {
  warehouseCode: string;
  productSku: string;
  availableUnits: number;
  avgDailyDemand: number;
  daysOfCover: number;
  leadTimeDays: number | null;
  reorderPoint: number | null;
  belowReorderPoint: boolean;
  coverShorterThanLeadTime: boolean;
}

export interface PurchaseOrderItem {
  id: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
}

export interface PurchaseOrder {
  id: string;
  supplierId: string;
  warehouseCode: string;
  status: string;
  totalValue: number | null;
  items: PurchaseOrderItem[];
  sourceType: string | null;
  sourceId: string | null;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class OperationsApiService {
  private readonly baseUrl = `${environment.gatewayUrl}/operations-service/api/v1`;

  constructor(private readonly http: HttpClient) {}

  listInventory(): Observable<InventoryBalance[]> {
    return this.http.get<InventoryBalance[]>(`${this.baseUrl}/inventory`);
  }

  listLowStock(): Observable<InventoryBalance[]> {
    return this.http.get<InventoryBalance[]>(`${this.baseUrl}/inventory/low-stock`);
  }

  listWarehousesForProduct(productSku: string): Observable<InventoryBalance[]> {
    return this.http.get<InventoryBalance[]>(`${this.baseUrl}/inventory/${productSku}/warehouses`);
  }

  adjustInventory(body: {
    warehouseCode: string;
    productSku: string;
    quantityDelta: number;
    reason: string;
  }): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/inventory/adjustments`, body);
  }

  transferInventory(body: {
    sourceWarehouseCode: string;
    destWarehouseCode: string;
    productSku: string;
    quantity: number;
    referenceType?: string;
    referenceId?: string;
  }): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/inventory/transfers`, body);
  }

  listPurchaseOrders(): Observable<PurchaseOrder[]> {
    return this.http.get<PurchaseOrder[]>(`${this.baseUrl}/purchase-orders`);
  }

  getPurchaseOrder(id: string): Observable<PurchaseOrder> {
    return this.http.get<PurchaseOrder>(`${this.baseUrl}/purchase-orders/${id}`);
  }

  createPurchaseOrder(body: {
    supplierId: string;
    warehouseCode: string;
    items: { productSku: string; quantity: number; unitPrice: number }[];
    sourceType?: string;
    sourceId?: string;
  }): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.baseUrl}/purchase-orders`, body);
  }

  submitPurchaseOrder(id: string): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.baseUrl}/purchase-orders/${id}/submit`, {});
  }

  receivePurchaseOrder(id: string): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.baseUrl}/purchase-orders/${id}/receive`, {});
  }

  setDemandProfile(body: {
    warehouseCode: string;
    productSku: string;
    avgDailyDemand: number;
    leadTimeDays: number;
    safetyStockOverride?: number;
    demandStdDev?: number;
    serviceLevelZ?: number;
  }): Observable<DemandProfileResponse> {
    return this.http.post<DemandProfileResponse>(`${this.baseUrl}/planning/demand-profile`, body);
  }

  getStockoutRisk(): Observable<StockoutRisk[]> {
    return this.http.get<StockoutRisk[]>(`${this.baseUrl}/planning/stockout-risk`);
  }
}
