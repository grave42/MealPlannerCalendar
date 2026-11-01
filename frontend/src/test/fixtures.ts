import type { Dish, MealPlanEntry, MealType } from '../api/types';

export function makeDish(overrides: Partial<Dish> = {}): Dish {
  return {
    id: 'dish-1',
    name: 'Омлет',
    description: null,
    ingredients: null,
    instructions: null,
    mealTypes: [],
    hasImage: false,
    ...overrides,
  };
}

export function makeEntry(
  overrides: Partial<MealPlanEntry> & { date: string; mealType: MealType },
): MealPlanEntry {
  return {
    id: 'entry-1',
    position: 1,
    dishId: 'dish-1',
    dishName: 'Омлет',
    hasImage: false,
    ...overrides,
  };
}
