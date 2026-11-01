import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { addMealPlanEntry, listMealPlan, removeMealPlanEntry } from '../api/mealPlan';
import type { Dish, MealPlanEntry, MealType } from '../api/types';
import { DaySection } from '../components/DaySection';
import { LoadError } from '../components/LoadError';
import { Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import { MEAL_TYPES, addDays, formatFullDate, parseIsoDate, toIsoDate } from '../utils/dates';

export function DayPage() {
  const { date } = useParams<{ date: string }>();
  const parsed = parseIsoDate(date);
  if (!parsed || !date) {
    return (
      <div className="page">
        <p className="error-text">Некорректная дата</p>
        <Link to="/" className="btn">
          К календарю
        </Link>
      </div>
    );
  }
  return <DayContent date={date} day={parsed} />;
}

interface AddVariables {
  mealType: MealType;
  dish: Dish;
}

function DayContent({ date, day }: { date: string; day: Date }) {
  const queryClient = useQueryClient();
  const { showError } = useToast();
  const key = ['meal-plan', date, date];

  const query = useQuery({ queryKey: key, queryFn: () => listMealPlan(date, date) });

  const rollback = (previous: MealPlanEntry[] | undefined, error: unknown) => {
    queryClient.setQueryData(key, previous);
    showError(errorMessage(error));
  };

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['meal-plan'] });

  const addMutation = useMutation({
    mutationFn: ({ mealType, dish }: AddVariables) =>
      addMealPlanEntry({ date, mealType, dishId: dish.id }),
    onMutate: async ({ mealType, dish }) => {
      await queryClient.cancelQueries({ queryKey: ['meal-plan'] });
      const previous = queryClient.getQueryData<MealPlanEntry[]>(key);
      const temp: MealPlanEntry = {
        id: `tmp-${dish.id}-${mealType}`,
        date,
        mealType,
        position: Number.MAX_SAFE_INTEGER,
        dishId: dish.id,
        dishName: dish.name,
        hasImage: dish.hasImage,
      };
      queryClient.setQueryData<MealPlanEntry[]>(key, [...(previous ?? []), temp]);
      return { previous };
    },
    onError: (error, _vars, ctx) => rollback(ctx?.previous, error),
    onSettled: refresh,
  });

  const removeMutation = useMutation({
    mutationFn: (entry: MealPlanEntry) => removeMealPlanEntry(entry.id),
    onMutate: async (entry) => {
      await queryClient.cancelQueries({ queryKey: ['meal-plan'] });
      const previous = queryClient.getQueryData<MealPlanEntry[]>(key);
      queryClient.setQueryData<MealPlanEntry[]>(
        key,
        (previous ?? []).filter((e) => e.id !== entry.id),
      );
      return { previous };
    },
    onError: (error, _entry, ctx) => rollback(ctx?.previous, error),
    onSettled: refresh,
  });

  const prev = toIsoDate(addDays(day, -1));
  const next = toIsoDate(addDays(day, 1));
  const monthParam = date.slice(0, 7);
  const entries = [...(query.data ?? [])].sort((a, b) => a.position - b.position);

  return (
    <div className="page">
      <div className="page-head">
        <h1>{formatFullDate(day)}</h1>
        <Link to={`/day/${prev}`} className="btn">
          ← Предыдущий день
        </Link>
        <Link to={`/day/${next}`} className="btn">
          Следующий день →
        </Link>
        <Link to={`/?month=${monthParam}`} className="btn">
          К календарю
        </Link>
        <Link
          to={`/dishes/new?returnTo=${encodeURIComponent(`/day/${date}`)}`}
          className="btn btn-primary"
        >
          Создать блюдо
        </Link>
      </div>
      {query.isPending && <Skeleton label="Загрузка дня" count={4} />}
      {query.isError && (
        <LoadError message="Не удалось загрузить день" onRetry={() => void query.refetch()} />
      )}
      {query.isSuccess &&
        MEAL_TYPES.map((type) => (
          <DaySection
            key={type}
            mealType={type}
            entries={entries.filter((e) => e.mealType === type)}
            onAdd={(mealType, dish) => addMutation.mutate({ mealType, dish })}
            onRemove={(entry) => removeMutation.mutate(entry)}
          />
        ))}
    </div>
  );
}
