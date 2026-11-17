import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Product {
  id: string;
  sku: string;
  name: string;
  unitCost: number | null;
  reorderPoint: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface Warehouse {
  id: string;
  code: string;
  name: string;
  city: string | null;
  country: string | null;
  capacityUnits: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface Supplier {
  id: string;
  name: string;
  category: string | null;
  onTimeRate: number | null;
  avgLeadTimeDays: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierPerformance {
  supplierId: string;
  name: string;
  onTimeRate: number | null;
  avgLeadTimeDays: number | null;
  riskLevel: string;
}

@Injectable({ providedIn: 'root' })
export class CoreApiService {
  private readonly baseUrl = `${environment.gatewayUrl}/core-service/api/v1`;

  constructor(private readonly http: HttpClient) {}

  listProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.baseUrl}/products`);
  }

  createProduct(body: { sku: string; name: string; unitCost?: number; reorderPoint?: number }): Observable<Product> {
    return this.http.post<Product>(`${this.baseUrl}/products`, body);
  }

  listWarehouses(): Observable<Warehouse[]> {
    return this.http.get<Warehouse[]>(`${this.baseUrl}/warehouses`);
  }

  createWarehouse(body: {
    code: string;
    name: string;
    city?: string;
    country?: string;
    capacityUnits?: number;
  }): Observable<Warehouse> {
    return this.http.post<Warehouse>(`${this.baseUrl}/warehouses`, body);
  }

  listSuppliers(): Observable<Supplier[]> {
    return this.http.get<Supplier[]>(`${this.baseUrl}/suppliers`);
  }

  getSupplierPerformance(id: string): Observable<SupplierPerformance> {
    return this.http.get<SupplierPerformance>(`${this.baseUrl}/suppliers/${id}/performance`);
  }

  createSupplier(body: {
    name: string;
    category?: string;
    onTimeRate?: number;
    avgLeadTimeDays?: number;
  }): Observable<Supplier> {
    return this.http.post<Supplier>(`${this.baseUrl}/suppliers`, body);
  }
}
