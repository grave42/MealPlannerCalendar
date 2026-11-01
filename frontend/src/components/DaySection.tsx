import { useId, useState } from 'react';
import { Link } from 'react-router-dom';
import type { Dish, MealPlanEntry, MealType } from '../api/types';
import { MEAL_LABELS } from '../utils/dates';
import { DishImage } from './DishImage';
import { DishPickerModal } from './DishPickerModal';
import styles from './DaySection.module.css';

interface DaySectionProps {
  mealType: MealType;
  entries: MealPlanEntry[];
  onAdd: (mealType: MealType, dish: Dish) => void;
  onRemove: (entry: MealPlanEntry) => void;
}

/** Секция одного приёма пищи на странице дня. */
export function DaySection({ mealType, entries, onAdd, onRemove }: DaySectionProps) {
  const headingId = useId();
  const [pickerOpen, setPickerOpen] = useState(false);

  return (
    <section aria-labelledby={headingId} className={styles.section}>
      <h2 id={headingId} className={styles.heading}>
        {MEAL_LABELS[mealType]}
      </h2>
      <div className={styles.body}>
        {entries.length > 0 && (
          <ul className={styles.list}>
            {entries.map((entry) => (
              <li key={entry.id} className={styles.item}>
                <DishImage
                  dishId={entry.dishId}
                  name={entry.dishName}
                  hasImage={entry.hasImage}
                  className={styles.image}
                />
                <Link to={`/dishes/${entry.dishId}/edit`} className={styles.name}>
                  {entry.dishName}
                </Link>
                <button
                  type="button"
                  className={`btn ${styles.remove}`}
                  aria-label={`Убрать блюдо ${entry.dishName}`}
                  disabled={entry.id.startsWith('tmp-')}
                  onClick={() => onRemove(entry)}
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        )}
        <button type="button" className="btn" onClick={() => setPickerOpen(true)}>
          + Добавить блюдо
        </button>
      </div>
      {pickerOpen && (
        <DishPickerModal
          mealType={mealType}
          takenDishIds={entries.map((e) => e.dishId)}
          onSelect={(dish) => {
            setPickerOpen(false);
            onAdd(mealType, dish);
          }}
          onClose={() => setPickerOpen(false)}
        />
      )}
    </section>
  );
}
