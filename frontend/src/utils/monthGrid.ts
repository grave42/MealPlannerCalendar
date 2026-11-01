import { addDays, toIsoDate } from './dates';

export interface GridCell {
  date: string;
  day: number;
  inCurrentMonth: boolean;
}

/**
 * Строит сетку месяца: полные недели, начиная с понедельника.
 * @param year год
 * @param month номер месяца от 1 до 12
 */
export function buildMonthGrid(year: number, month: number): GridCell[] {
  const first = new Date(year, month - 1, 1);
  const daysInMonth = new Date(year, month, 0).getDate();
  const offset = (first.getDay() + 6) % 7;
  const total = Math.ceil((offset + daysInMonth) / 7) * 7;
  const start = addDays(first, -offset);

  const cells: GridCell[] = [];
  for (let i = 0; i < total; i += 1) {
    const d = addDays(start, i);
    cells.push({
      date: toIsoDate(d),
      day: d.getDate(),
      inCurrentMonth: d.getMonth() === month - 1 && d.getFullYear() === year,
    });
  }
  return cells;
}
