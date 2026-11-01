import { request } from './client';
import type { ExternalRecipe, ExternalRecipeSearchItem } from './types';

const BASE = '/api/v1/external-recipe';

export function searchExternalRecipes(query: string): Promise<ExternalRecipeSearchItem[]> {
  return request<ExternalRecipeSearchItem[]>(`${BASE}/search?query=${encodeURIComponent(query)}`);
}

export function getExternalRecipe(id: string): Promise<ExternalRecipe> {
  return request<ExternalRecipe>(`${BASE}?id=${encodeURIComponent(id)}`);
}
