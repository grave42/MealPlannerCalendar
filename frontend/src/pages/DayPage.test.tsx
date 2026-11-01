import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import type { Dish, MealPlanEntry } from '../api/types';
import { makeDish, makeEntry } from '../test/fixtures';
import { renderApp } from '../test/render';
import { server } from '../test/server';

const DATE = '2026-10-15';

interface Mocks {
  entries: MealPlanEntry[];
  dishes: Dish[];
  requestedMealTypes: (string | null)[];
  posted: unknown[];
}

function setup(entries: MealPlanEntry[] = [], dishes: Dish[] = []): Mocks {
  const state: Mocks = { entries: [...entries], dishes, requestedMealTypes: [], posted: [] };
  server.use(
    http.get('*/api/v1/meal-plan', () => HttpResponse.json(state.entries)),
    http.get('*/api/v1/dish', ({ request }) => {
      state.requestedMealTypes.push(new URL(request.url).searchParams.get('mealType'));
      return HttpResponse.json(state.dishes);
    }),
    http.post('*/api/v1/meal-plan', async ({ request }) => {
      const body = (await request.json()) as {
        date: string;
        mealType: MealPlanEntry['mealType'];
        dishId: string;
      };
      state.posted.push(body);
      const dish = state.dishes.find((d) => d.id === body.dishId)!;
      const entry = makeEntry({
        id: `server-${state.entries.length}`,
        date: body.date,
        mealType: body.mealType,
        dishId: dish.id,
        dishName: dish.name,
      });
      state.entries.push(entry);
      return HttpResponse.json(entry, { status: 201 });
    }),
    http.delete('*/api/v1/meal-plan/:id', ({ params }) => {
      state.entries = state.entries.filter((e) => e.id !== params.id);
      return new HttpResponse(null, { status: 204 });
    }),
  );
  return state;
}

const section = (name: string) => screen.findByRole('region', { name });

describe('DayPage', () => {
  it('показывает заголовок, четыре секции и блюда дня', async () => {
    setup([
      makeEntry({ date: DATE, mealType: 'LUNCH', dishId: 'd1', dishName: 'Борщ', hasImage: true }),
    ]);
    renderApp(`/day/${DATE}`);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Четверг, 15 октября 2026' }),
    ).toBeInTheDocument();
    for (const name of ['Завтрак', 'Обед', 'Ужин', 'Перекус']) {
      const s = await section(name);
      expect(within(s).getByRole('button', { name: '+ Добавить блюдо' })).toBeInTheDocument();
    }
    const lunch = await section('Обед');
    expect(await within(lunch).findByRole('link', { name: 'Борщ' })).toHaveAttribute(
      'href',
      '/dishes/d1/edit',
    );
    expect(within(lunch).getByAltText('Борщ')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'К календарю' })).toHaveAttribute(
      'href',
      '/?month=2026-10',
    );
    expect(screen.getByRole('link', { name: 'Создать блюдо' })).toHaveAttribute(
      'href',
      '/dishes/new?returnTo=%2Fday%2F2026-10-15',
    );
  });

  it('переходит на предыдущий и следующий день', async () => {
    setup();
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    await user.click(await screen.findByRole('link', { name: '← Предыдущий день' }));
    expect(
      await screen.findByRole('heading', { level: 1, name: /14 октября 2026/ }),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Следующий день →' }));
    await user.click(screen.getByRole('link', { name: 'Следующий день →' }));
    expect(
      await screen.findByRole('heading', { level: 1, name: /16 октября 2026/ }),
    ).toBeInTheDocument();
  });

  it('показывает сообщение о некорректной дате', async () => {
    renderApp('/day/2026-13-45');
    expect(await screen.findByText('Некорректная дата')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'К календарю' })).toHaveAttribute('href', '/');
  });

  it('показывает ошибку загрузки и повторяет запрос', async () => {
    server.use(
      http.get('*/api/v1/meal-plan', () =>
        HttpResponse.json({ userMessage: 'x' }, { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    expect(await screen.findByText('Не удалось загрузить день')).toBeInTheDocument();
    setup();
    await user.click(screen.getByRole('button', { name: 'Повторить' }));
    expect(await section('Завтрак')).toBeInTheDocument();
  });

  it('добавляет блюдо в слот через окно выбора', async () => {
    const a = makeDish({ id: 'a', name: 'Каша', mealTypes: ['BREAKFAST'] });
    const b = makeDish({ id: 'b', name: 'Омлет' });
    const mocks = setup([], [a, b]);
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);

    const breakfast = await section('Завтрак');
    await user.click(within(breakfast).getByRole('button', { name: '+ Добавить блюдо' }));

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(await within(dialog).findByRole('button', { name: /Каша/ })).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: /Омлет/ })).toBeInTheDocument();
    expect(mocks.requestedMealTypes).toContain('BREAKFAST');

    await user.click(within(dialog).getByRole('button', { name: /Каша/ }));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(await within(breakfast).findByRole('link', { name: 'Каша' })).toBeInTheDocument();
    expect(mocks.posted).toEqual([{ date: DATE, mealType: 'BREAKFAST', dishId: 'a' }]);
  });

  it('фильтрует список в окне выбора по названию', async () => {
    setup([], [makeDish({ id: 'a', name: 'Каша' }), makeDish({ id: 'b', name: 'Омлет' })]);
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    await user.click(
      within(await section('Обед')).getByRole('button', { name: '+ Добавить блюдо' }),
    );
    const dialog = await screen.findByRole('dialog');
    await within(dialog).findByRole('button', { name: /Каша/ });
    await user.type(within(dialog).getByRole('searchbox'), 'омл');
    expect(within(dialog).queryByRole('button', { name: /Каша/ })).toBeNull();
    expect(within(dialog).getByRole('button', { name: /Омлет/ })).toBeInTheDocument();
  });

  it('делает неактивными блюда, уже находящиеся в слоте', async () => {
    setup(
      [makeEntry({ date: DATE, mealType: 'BREAKFAST', dishId: 'a', dishName: 'Каша' })],
      [makeDish({ id: 'a', name: 'Каша' }), makeDish({ id: 'b', name: 'Омлет' })],
    );
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    await user.click(
      within(await section('Завтрак')).getByRole('button', { name: '+ Добавить блюдо' }),
    );
    const dialog = await screen.findByRole('dialog');
    expect(await within(dialog).findByRole('button', { name: /Каша/ })).toBeDisabled();
    expect(within(dialog).getByRole('button', { name: /Омлет/ })).toBeEnabled();
  });

  it('показывает пустое состояние окна выбора', async () => {
    setup();
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    await user.click(
      within(await section('Ужин')).getByRole('button', { name: '+ Добавить блюдо' }),
    );
    expect(await screen.findByText('Нет подходящих блюд. Создайте новое')).toBeInTheDocument();
  });

  it('закрывает окно по Esc и возвращает фокус на кнопку', async () => {
    setup();
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    const button = within(await section('Перекус')).getByRole('button', {
      name: '+ Добавить блюдо',
    });
    await user.click(button);
    await screen.findByRole('dialog');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(button).toHaveFocus();
  });

  it('закрывает окно крестиком и кликом по фону', async () => {
    setup();
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    const button = within(await section('Обед')).getByRole('button', { name: '+ Добавить блюдо' });
    await user.click(button);
    await user.click(await screen.findByRole('button', { name: 'Закрыть' }));
    expect(screen.queryByRole('dialog')).toBeNull();

    await user.click(button);
    const dialog = await screen.findByRole('dialog');
    await user.click(dialog.parentElement!);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('при ошибке добавления показывает уведомление и откатывает изменение', async () => {
    setup([], [makeDish({ id: 'a', name: 'Каша' })]);
    server.use(
      http.post('*/api/v1/meal-plan', () =>
        HttpResponse.json({ userMessage: 'Блюдо уже есть в этом приёме пищи' }, { status: 422 }),
      ),
    );
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    const breakfast = await section('Завтрак');
    await user.click(within(breakfast).getByRole('button', { name: '+ Добавить блюдо' }));
    await user.click(await screen.findByRole('button', { name: /Каша/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Блюдо уже есть в этом приёме пищи');
    await waitFor(() => expect(within(breakfast).queryByRole('link', { name: 'Каша' })).toBeNull());
  });

  it('убирает блюдо из слота по «×»', async () => {
    setup([makeEntry({ id: 'e1', date: DATE, mealType: 'DINNER', dishId: 'a', dishName: 'Суп' })]);
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    const dinner = await section('Ужин');
    await user.click(await within(dinner).findByRole('button', { name: 'Убрать блюдо Суп' }));
    await waitFor(() => expect(within(dinner).queryByRole('link', { name: 'Суп' })).toBeNull());
  });

  it('при ошибке удаления показывает уведомление и возвращает блюдо', async () => {
    setup([makeEntry({ id: 'e1', date: DATE, mealType: 'DINNER', dishId: 'a', dishName: 'Суп' })]);
    server.use(
      http.delete('*/api/v1/meal-plan/:id', () =>
        HttpResponse.json({ userMessage: 'Не удалось убрать блюдо' }, { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderApp(`/day/${DATE}`);
    const dinner = await section('Ужин');
    await user.click(await within(dinner).findByRole('button', { name: 'Убрать блюдо Суп' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Не удалось убрать блюдо');
    expect(await within(dinner).findByRole('link', { name: 'Суп' })).toBeInTheDocument();
  });
});
