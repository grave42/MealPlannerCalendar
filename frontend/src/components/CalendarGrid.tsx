import { useMemo } from 'react';
import type { MealPlanEntry } from '../api/types';
import { buildMonthGrid } from '../utils/monthGrid';
import { todayIso } from '../utils/dates';
import { DayCell } from './DayCell';
import styles from './CalendarGrid.module.css';

const WEEKDAYS = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];

interface CalendarGridProps {
  year: number;
  month: number;
  entries: MealPlanEntry[];
}

export function CalendarGrid({ year, month, entries }: CalendarGridProps) {
  const cells = useMemo(() => buildMonthGrid(year, month), [year, month]);
  const byDate = useMemo(() => {
    const map = new Map<string, MealPlanEntry[]>();
    for (const e of [...entries].sort((a, b) => a.position - b.position)) {
      map.set(e.date, [...(map.get(e.date) ?? []), e]);
    }
    return map;
  }, [entries]);
  const today = todayIso();

  return (
    <div className={styles.grid}>
      {WEEKDAYS.map((d) => (
        <div key={d} className={styles.weekday}>
          {d}
        </div>
      ))}
      {cells.map((cell) => (
        <DayCell
          key={cell.date}
          cell={cell}
          isToday={cell.date === today}
          entries={cell.inCurrentMonth ? (byDate.get(cell.date) ?? []) : []}
        />
      ))}
    </div>
  );
}
