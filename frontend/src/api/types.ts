export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK';

export interface Dish {
  id: string;
  name: string;
  description: string | null;
  ingredients: string | null;
  instructions: string | null;
  mealTypes: MealType[];
  hasImage: boolean;
}

export interface DishInput {
  name: string;
  description: string;
  ingredients: string;
  instructions: string;
  mealTypes: MealType[];
  sourceImageUrl?: string;
}

export interface MealPlanEntry {
  id: string;
  date: string;
  mealType: MealType;
  position: number;
  dishId: string;
  dishName: string;
  hasImage: boolean;
}

export interface MealPlanEntryInput {
  date: string;
  mealType: MealType;
  dishId: string;
}

export interface ExternalRecipeSearchItem {
  id: string;
  title: string;
  imageUrl: string | null;
}

export interface ExternalRecipe {
  id: string;
  title: string;
  description: string | null;
  ingredients: string | null;
  instructions: string | null;
  imageUrl: string | null;
}
