import { jsonBody, request } from './client';
import type { Dish, DishInput, MealType } from './types';

const BASE = '/api/v1/dish';

const imageVersions = new Map<string, number>();

/** Адрес картинки блюда; версия сбрасывает кэш браузера после замены картинки. */
export function dishImageUrl(id: string): string {
  const v = imageVersions.get(id);
  return v ? `${BASE}/${id}/image?v=${v}` : `${BASE}/${id}/image`;
}

export function listDishes(params: { mealType?: MealType; search?: string } = {}): Promise<Dish[]> {
  const query = new URLSearchParams();
  if (params.mealType) query.set('mealType', params.mealType);
  if (params.search?.trim()) query.set('search', params.search.trim());
  const qs = query.toString();
  return request<Dish[]>(qs ? `${BASE}?${qs}` : BASE);
}

export function getDish(id: string): Promise<Dish> {
  return request<Dish>(`${BASE}/${id}`);
}

export function createDish(input: DishInput): Promise<Dish> {
  return request<Dish>(BASE, jsonBody('POST', input));
}

export function updateDish(id: string, input: DishInput): Promise<Dish> {
  return request<Dish>(`${BASE}/${id}`, jsonBody('PUT', input));
}

export function deleteDish(id: string): Promise<void> {
  return request<void>(`${BASE}/${id}`, { method: 'DELETE' });
}

export async function uploadDishImage(id: string, file: File): Promise<void> {
  const form = new FormData();
  form.append('file', file);
  await request<void>(`${BASE}/${id}/image`, { method: 'PUT', body: form });
  imageVersions.set(id, Date.now());
}

export async function deleteDishImage(id: string): Promise<void> {
  await request<void>(`${BASE}/${id}/image`, { method: 'DELETE' });
  imageVersions.set(id, Date.now());
}
