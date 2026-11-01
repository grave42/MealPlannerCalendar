import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { deleteDish, listDishes } from '../api/dishes';
import type { Dish } from '../api/types';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DishCard } from '../components/DishCard';
import { LoadError } from '../components/LoadError';
import { Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import styles from './DishesPage.module.css';

export function DishesPage() {
  const queryClient = useQueryClient();
  const { showError } = useToast();
  const [search, setSearch] = useState('');
  const debounced = useDebouncedValue(search.trim(), 250);
  const [toDelete, setToDelete] = useState<Dish | null>(null);

  const query = useQuery({
    queryKey: ['dishes', { search: debounced }],
    queryFn: () => listDishes({ search: debounced }),
    placeholderData: keepPreviousData,
  });

  const deleteMutation = useMutation({
    mutationFn: (dish: Dish) => deleteDish(dish.id),
    onSuccess: () => {
      setToDelete(null);
      void queryClient.invalidateQueries({ queryKey: ['dishes'] });
      void queryClient.invalidateQueries({ queryKey: ['meal-plan'] });
    },
    onError: (error) => {
      setToDelete(null);
      showError(errorMessage(error));
    },
  });

  const dishes = query.data ?? [];

  return (
    <div className="page">
      <div className="page-head">
        <h1>Блюда</h1>
        <input
          type="search"
          className={`input ${styles.search}`}
          placeholder="Найти блюдо…"
          aria-label="Найти блюдо"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <Link to="/dishes/new" className="btn btn-primary">
          Создать блюдо
        </Link>
      </div>

      {query.isPending && <Skeleton label="Загрузка блюд" count={4} className={styles.skeleton} />}
      {query.isError && (
        <LoadError message="Не удалось загрузить блюда" onRetry={() => void query.refetch()} />
      )}
      {query.isSuccess && dishes.length === 0 && debounced === '' && (
        <div>
          <p>В картотеке пока нет блюд</p>
          <Link to="/dishes/new" className="btn btn-primary">
            Создать блюдо
          </Link>
        </div>
      )}
      {query.isSuccess && dishes.length === 0 && debounced !== '' && <p>Ничего не найдено</p>}
      {query.isSuccess && dishes.length > 0 && (
        <div className={styles.grid}>
          {dishes.map((dish) => (
            <DishCard key={dish.id} dish={dish} onDelete={setToDelete} />
          ))}
        </div>
      )}

      {toDelete && (
        <ConfirmDialog
          title="Удаление блюда"
          message={`Удалить блюдо «${toDelete.name}»? Оно также исчезнет из календаря.`}
          confirmLabel="Удалить"
          danger
          busy={deleteMutation.isPending}
          onConfirm={() => deleteMutation.mutate(toDelete)}
          onCancel={() => setToDelete(null)}
        />
      )}
    </div>
  );
}
