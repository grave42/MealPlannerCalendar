import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import type { Dish } from '../api/types';
import { makeDish } from '../test/fixtures';
import { renderApp } from '../test/render';
import { server } from '../test/server';

interface State {
  dishes: Dish[];
  searches: (string | null)[];
  deleted: string[];
}

function setup(dishes: Dish[]): State {
  const state: State = { dishes, searches: [], deleted: [] };
  server.use(
    http.get('*/api/v1/dish', ({ request }) => {
      const search = new URL(request.url).searchParams.get('search');
      state.searches.push(search);
      const needle = (search ?? '').toLowerCase();
      return HttpResponse.json(state.dishes.filter((d) => d.name.toLowerCase().includes(needle)));
    }),
    http.delete('*/api/v1/dish/:id', ({ params }) => {
      state.deleted.push(String(params.id));
      state.dishes = state.dishes.filter((d) => d.id !== params.id);
      return new HttpResponse(null, { status: 204 });
    }),
  );
  return state;
}

const omelet = makeDish({
  id: 'o',
  name: 'Омлет',
  mealTypes: ['BREAKFAST', 'SNACK'],
  hasImage: true,
});
const soup = makeDish({ id: 's', name: 'Суп' });

describe('DishesPage', () => {
  it('показывает карточки блюд с метками, картинкой и кнопками', async () => {
    setup([omelet, soup]);
    renderApp('/dishes');
    expect(screen.getByRole('status', { name: 'Загрузка блюд' })).toBeInTheDocument();

    const card = (await screen.findByRole('heading', { name: 'Омлет' })).closest('article')!;
    expect(within(card).getByText('Завтрак')).toBeInTheDocument();
    expect(within(card).getByText('Перекус')).toBeInTheDocument();
    expect(within(card).getByAltText('Омлет')).toHaveAttribute('src', '/api/v1/dish/o/image');
    expect(within(card).getByRole('link', { name: /Редактировать/ })).toHaveAttribute(
      'href',
      '/dishes/o/edit',
    );
    expect(screen.getByRole('link', { name: 'Создать блюдо' })).toHaveAttribute(
      'href',
      '/dishes/new',
    );
    expect(screen.getByRole('heading', { name: 'Суп' })).toBeInTheDocument();
  });

  it('ищет блюда по названию', async () => {
    const state = setup([omelet, soup]);
    const user = userEvent.setup();
    renderApp('/dishes');
    await screen.findByRole('heading', { name: 'Омлет' });

    await user.type(screen.getByRole('searchbox', { name: 'Найти блюдо' }), 'суп');
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Омлет' })).toBeNull());
    expect(screen.getByRole('heading', { name: 'Суп' })).toBeInTheDocument();
    expect(state.searches).toContain('суп');
  });

  it('показывает пустое состояние картотеки', async () => {
    setup([]);
    renderApp('/dishes');
    expect(await screen.findByText('В картотеке пока нет блюд')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Создать блюдо' })).toHaveLength(2);
  });

  it('показывает «Ничего не найдено» для поиска без результатов', async () => {
    setup([omelet]);
    const user = userEvent.setup();
    renderApp('/dishes');
    await screen.findByRole('heading', { name: 'Омлет' });
    await user.type(screen.getByRole('searchbox', { name: 'Найти блюдо' }), 'xyz');
    expect(await screen.findByText('Ничего не найдено')).toBeInTheDocument();
  });

  it('удаляет блюдо после подтверждения', async () => {
    const state = setup([omelet, soup]);
    const user = userEvent.setup();
    renderApp('/dishes');
    await user.click(await screen.findByRole('button', { name: 'Удалить блюдо «Омлет»' }));

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Удалить блюдо «Омлет»? Оно также исчезнет из календаря.');
    await user.click(within(dialog).getByRole('button', { name: 'Удалить' }));

    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Омлет' })).toBeNull());
    expect(state.deleted).toEqual(['o']);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('не удаляет блюдо при отмене', async () => {
    const state = setup([omelet]);
    const user = userEvent.setup();
    renderApp('/dishes');
    await user.click(await screen.findByRole('button', { name: 'Удалить блюдо «Омлет»' }));
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Отмена' }),
    );
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Омлет' })).toBeInTheDocument();
    expect(state.deleted).toEqual([]);
  });

  it('показывает уведомление, если удалить не удалось', async () => {
    setup([omelet]);
    server.use(
      http.delete('*/api/v1/dish/:id', () =>
        HttpResponse.json({ userMessage: 'Блюдо не найдено' }, { status: 404 }),
      ),
    );
    const user = userEvent.setup();
    renderApp('/dishes');
    await user.click(await screen.findByRole('button', { name: 'Удалить блюдо «Омлет»' }));
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Удалить' }),
    );
    expect(await screen.findByRole('alert')).toHaveTextContent('Блюдо не найдено');
  });

  it('показывает ошибку загрузки с кнопкой «Повторить»', async () => {
    server.use(
      http.get('*/api/v1/dish', () => HttpResponse.json({ userMessage: 'x' }, { status: 500 })),
    );
    const user = userEvent.setup();
    renderApp('/dishes');
    expect(await screen.findByText('Не удалось загрузить блюда')).toBeInTheDocument();
    setup([soup]);
    await user.click(screen.getByRole('button', { name: 'Повторить' }));
    expect(await screen.findByRole('heading', { name: 'Суп' })).toBeInTheDocument();
  });
});
