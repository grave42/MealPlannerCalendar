import { jsonBody, request } from './client';
import type { MealPlanEntry, MealPlanEntryInput } from './types';

const BASE = '/api/v1/meal-plan';

export function listMealPlan(from: string, to: string): Promise<MealPlanEntry[]> {
  return request<MealPlanEntry[]>(`${BASE}?from=${from}&to=${to}`);
}

export function addMealPlanEntry(input: MealPlanEntryInput): Promise<MealPlanEntry> {
  return request<MealPlanEntry>(BASE, jsonBody('POST', input));
}

export function removeMealPlanEntry(id: string): Promise<void> {
  return request<void>(`${BASE}/${id}`, { method: 'DELETE' });
}
