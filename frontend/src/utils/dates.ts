import type { MealType } from '../api/types';

export const MEAL_TYPES: MealType[] = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];

export const MEAL_LABELS: Record<MealType, string> = {
  BREAKFAST: 'Завтрак',
  LUNCH: 'Обед',
  DINNER: 'Ужин',
  SNACK: 'Перекус',
};

const pad = (n: number) => String(n).padStart(2, '0');

export function toIsoDate(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Разбирает строго формат YYYY-MM-DD; для несуществующих дат возвращает null. */
export function parseIsoDate(value: string | undefined): Date | null {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
  const [y, m, d] = value.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return toIsoDate(date) === value ? date : null;
}

export function addDays(d: Date, days: number): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + days);
}

export function todayIso(): string {
  return toIsoDate(new Date());
}

function capitalize(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

/** «Октябрь 2026» */
export function formatMonthTitle(year: number, month: number): string {
  const name = new Intl.DateTimeFormat('ru-RU', { month: 'long' }).format(
    new Date(year, month - 1, 1),
  );
  return `${capitalize(name)} ${year}`;
}

/** «15 октября» */
export function formatDayMonth(d: Date): string {
  return new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long' }).format(d);
}

/** «Пятница, 15 октября 2026» */
export function formatFullDate(d: Date): string {
  const weekday = new Intl.DateTimeFormat('ru-RU', { weekday: 'long' }).format(d);
  const rest = new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(d);
  return `${capitalize(weekday)}, ${rest.replace(/\s*г\.$/, '')}`;
}
