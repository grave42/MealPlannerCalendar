import type { MealPlanEntry, MealType } from '../api/types';
import { MEAL_LABELS } from '../utils/dates';
import { DishImage } from './DishImage';
import styles from './MealSlot.module.css';

const MAX_VISIBLE = 2;

interface MealSlotProps {
  mealType: MealType;
  entries: MealPlanEntry[];
}

/** Строка приёма пищи внутри ячейки календаря: миниатюры и названия блюд. */
export function MealSlot({ mealType, entries }: MealSlotProps) {
  const visible = entries.slice(0, MAX_VISIBLE);
  const hidden = entries.length - visible.length;
  return (
    <div className={styles.slot}>
      <span className={styles.label}>{MEAL_LABELS[mealType]}</span>
      {visible.map((e) => (
        <span key={e.id} className={styles.dish}>
          <DishImage
            dishId={e.dishId}
            name={e.dishName}
            hasImage={e.hasImage}
            className={styles.thumb}
          />
          <span className={styles.name}>{e.dishName}</span>
        </span>
      ))}
      {hidden > 0 && <span className={styles.more}>+{hidden}</span>}
    </div>
  );
}
