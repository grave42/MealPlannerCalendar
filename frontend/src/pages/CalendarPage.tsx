import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { listMealPlan } from '../api/mealPlan';
import { CalendarGrid } from '../components/CalendarGrid';
import { LoadError } from '../components/LoadError';
import { Skeleton } from '../components/Skeleton';
import { buildMonthGrid } from '../utils/monthGrid';
import { formatMonthTitle } from '../utils/dates';
import styles from './CalendarPage.module.css';

function parseMonth(value: string | null): { year: number; month: number } {
  const m = value?.match(/^(\d{4})-(\d{2})$/);
  if (m && Number(m[2]) >= 1 && Number(m[2]) <= 12) {
    return { year: Number(m[1]), month: Number(m[2]) };
  }
  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() + 1 };
}

function monthParam(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`;
}

export function CalendarPage() {
  const [params, setParams] = useSearchParams();
  const { year, month } = parseMonth(params.get('month'));

  const cells = buildMonthGrid(year, month);
  const from = cells[0].date;
  const to = cells[cells.length - 1].date;

  const query = useQuery({
    queryKey: ['meal-plan', from, to],
    queryFn: () => listMealPlan(from, to),
  });

  const goTo = (y: number, m: number) => {
    const d = new Date(y, m - 1, 1);
    setParams({ month: monthParam(d.getFullYear(), d.getMonth() + 1) });
  };

  const goToday = () => {
    setParams({});
  };

  return (
    <div className="page">
      <div className="page-head">
        <h1>{formatMonthTitle(year, month)}</h1>
        <button type="button" className="btn" onClick={() => goTo(year, month - 1)}>
          ←
        </button>
        <button type="button" className="btn" onClick={() => goTo(year, month + 1)}>
          →
        </button>
        <button type="button" className="btn" onClick={goToday}>
          Сегодня
        </button>
      </div>
      {query.isPending && (
        <Skeleton label="Загрузка календаря" count={5} className={styles.skeletonRow} />
      )}
      {query.isError && (
        <LoadError message="Не удалось загрузить календарь" onRetry={() => void query.refetch()} />
      )}
      {query.isSuccess && <CalendarGrid year={year} month={month} entries={query.data} />}
    </div>
  );
}
