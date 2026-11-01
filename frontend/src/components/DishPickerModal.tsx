import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { listDishes } from '../api/dishes';
import type { Dish, MealType } from '../api/types';
import { MEAL_LABELS } from '../utils/dates';
import { DishImage } from './DishImage';
import { LoadError } from './LoadError';
import { Modal } from './Modal';
import { Skeleton } from './Skeleton';
import styles from './DishPickerModal.module.css';

interface DishPickerModalProps {
  mealType: MealType;
  takenDishIds: string[];
  onSelect: (dish: Dish) => void;
  onClose: () => void;
}

export function DishPickerModal({
  mealType,
  takenDishIds,
  onSelect,
  onClose,
}: DishPickerModalProps) {
  const [search, setSearch] = useState('');
  const query = useQuery({
    queryKey: ['dishes', { mealType }],
    queryFn: () => listDishes({ mealType }),
  });

  const needle = search.trim().toLowerCase();
  const dishes = (query.data ?? []).filter((d) => d.name.toLowerCase().includes(needle));

  return (
    <Modal title={`Добавить: ${MEAL_LABELS[mealType]}`} onClose={onClose}>
      <input
        type="search"
        className="input"
        placeholder="Найти блюдо…"
        aria-label="Найти блюдо"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        data-autofocus
      />
      <div className={styles.list}>
        {query.isPending && <Skeleton label="Загрузка блюд" count={4} />}
        {query.isError && (
          <LoadError message="Не удалось загрузить блюда" onRetry={() => void query.refetch()} />
        )}
        {query.isSuccess && dishes.length === 0 && <p>Нет подходящих блюд. Создайте новое</p>}
        {query.isSuccess && dishes.length > 0 && (
          <ul className={styles.items}>
            {dishes.map((dish) => (
              <li key={dish.id}>
                <button
                  type="button"
                  className={styles.item}
                  disabled={takenDishIds.includes(dish.id)}
                  onClick={() => onSelect(dish)}
                >
                  <DishImage
                    dishId={dish.id}
                    name={dish.name}
                    hasImage={dish.hasImage}
                    className={styles.thumb}
                  />
                  <span className={styles.name}>{dish.name}</span>
                  {dish.mealTypes.map((t) => (
                    <span key={t} className="chip">
                      {MEAL_LABELS[t]}
                    </span>
                  ))}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Modal>
  );
}
