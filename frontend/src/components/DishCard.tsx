import { Link } from 'react-router-dom';
import type { Dish } from '../api/types';
import { MEAL_LABELS } from '../utils/dates';
import { DishImage } from './DishImage';
import styles from './DishCard.module.css';

interface DishCardProps {
  dish: Dish;
  onDelete: (dish: Dish) => void;
}

export function DishCard({ dish, onDelete }: DishCardProps) {
  return (
    <article className={styles.card}>
      <DishImage
        dishId={dish.id}
        name={dish.name}
        hasImage={dish.hasImage}
        className={styles.image}
      />
      <div className={styles.body}>
        <h2 className={styles.name}>{dish.name}</h2>
        {dish.mealTypes.length > 0 && (
          <div className={styles.chips}>
            {dish.mealTypes.map((t) => (
              <span key={t} className="chip">
                {MEAL_LABELS[t]}
              </span>
            ))}
          </div>
        )}
        <div className={styles.actions}>
          <Link
            to={`/dishes/${dish.id}/edit`}
            className="btn"
            aria-label={`Редактировать блюдо «${dish.name}»`}
          >
            Редактировать
          </Link>
          <button
            type="button"
            className="btn btn-danger"
            aria-label={`Удалить блюдо «${dish.name}»`}
            onClick={() => onDelete(dish)}
          >
            Удалить
          </button>
        </div>
      </div>
    </article>
  );
}
