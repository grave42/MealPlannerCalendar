import { Link } from 'react-router-dom';
import type { MealPlanEntry } from '../api/types';
import type { GridCell } from '../utils/monthGrid';
import { MEAL_TYPES, formatDayMonth, parseIsoDate } from '../utils/dates';
import { MealSlot } from './MealSlot';
import styles from './DayCell.module.css';

interface DayCellProps {
  cell: GridCell;
  isToday: boolean;
  entries: MealPlanEntry[];
}

export function DayCell({ cell, isToday, entries }: DayCellProps) {
  const date = parseIsoDate(cell.date)!;
  const weekday = new Intl.DateTimeFormat('ru-RU', { weekday: 'short' }).format(date);

  const head = (
    <span className={styles.head}>
      <span className={styles.number}>{cell.day}</span>
      <span className={styles.weekday} aria-hidden="true">
        {weekday}
      </span>
    </span>
  );

  if (!cell.inCurrentMonth) {
    return <div className={`${styles.cell} ${styles.outside}`}>{head}</div>;
  }

  return (
    <Link
      to={`/day/${cell.date}`}
      className={`${styles.cell} ${isToday ? styles.today : ''}`}
      aria-label={`${formatDayMonth(date)}, открыть день`}
      aria-current={isToday ? 'date' : undefined}
    >
      {head}
      {MEAL_TYPES.map((type) => (
        <MealSlot key={type} mealType={type} entries={entries.filter((e) => e.mealType === type)} />
      ))}
    </Link>
  );
}
