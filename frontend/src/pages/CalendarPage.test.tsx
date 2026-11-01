import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { renderApp } from '../test/render';
import { makeEntry } from '../test/fixtures';
import { server } from '../test/server';

function mockPlan(entries: unknown[] = []) {
  server.use(http.get('*/api/v1/meal-plan', () => HttpResponse.json(entries)));
}

describe('CalendarPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 9, 15, 12));
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('показывает скелетон, затем сетку текущего месяца с выделенным сегодняшним днём', async () => {
    mockPlan();
    renderApp('/');
    expect(screen.getByRole('status', { name: 'Загрузка календаря' })).toBeInTheDocument();

    expect(await screen.findByRole('heading', { name: 'Октябрь 2026' })).toBeInTheDocument();
    const today = await screen.findByRole('link', { name: '15 октября, открыть день' });
    expect(today).toHaveAttribute('href', '/day/2026-10-15');
    expect(today).toHaveAttribute('aria-current', 'date');
    for (const day of ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']) {
      expect(screen.getByText(day)).toBeInTheDocument();
    }
    expect(within(today).getByText('Завтрак')).toBeInTheDocument();
    expect(within(today).getByText('Обед')).toBeInTheDocument();
    expect(within(today).getByText('Ужин')).toBeInTheDocument();
    expect(within(today).getByText('Перекус')).toBeInTheDocument();
  });

  it('показывает блюда в слотах, с картинкой и без неё', async () => {
    mockPlan([
      makeEntry({
        id: 'e1',
        date: '2026-10-15',
        mealType: 'LUNCH',
        dishId: 'a',
        dishName: 'Борщ',
        hasImage: true,
      }),
      makeEntry({
        id: 'e2',
        date: '2026-10-15',
        mealType: 'LUNCH',
        dishId: 'b',
        dishName: 'Плов',
        position: 2,
      }),
    ]);
    renderApp('/');
    const cell = await screen.findByRole('link', { name: '15 октября, открыть день' });
    await within(cell).findByText('Борщ');
    expect(within(cell).getByText('Плов')).toBeInTheDocument();
    expect(within(cell).getByAltText('Борщ')).toHaveAttribute('src', '/api/v1/dish/a/image');
  });

  it('сворачивает лишние блюда в «+N»', async () => {
    mockPlan(
      ['a', 'b', 'c', 'd'].map((id, i) =>
        makeEntry({
          id,
          date: '2026-10-15',
          mealType: 'DINNER',
          dishId: id,
          dishName: `Блюдо ${id}`,
          position: i,
        }),
      ),
    );
    renderApp('/');
    expect(await screen.findByText('+2')).toBeInTheDocument();
  });

  it('переключает месяцы и возвращается кнопкой «Сегодня»', async () => {
    mockPlan();
    const user = userEvent.setup();
    renderApp('/');
    await screen.findByRole('heading', { name: 'Октябрь 2026' });

    await user.click(screen.getByRole('button', { name: '→' }));
    expect(await screen.findByRole('heading', { name: 'Ноябрь 2026' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '←' }));
    await user.click(screen.getByRole('button', { name: '←' }));
    expect(await screen.findByRole('heading', { name: 'Сентябрь 2026' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Сегодня' }));
    expect(await screen.findByRole('heading', { name: 'Октябрь 2026' })).toBeInTheDocument();
  });

  it('не делает дни другого месяца кликабельными', async () => {
    mockPlan();
    renderApp('/');
    await screen.findByRole('link', { name: '15 октября, открыть день' });
    expect(screen.queryByRole('link', { name: '28 сентября, открыть день' })).toBeNull();
  });

  it('открывает день по клику на ячейку', async () => {
    mockPlan();
    const user = userEvent.setup();
    renderApp('/');
    await user.click(await screen.findByRole('link', { name: '20 октября, открыть день' }));
    await waitFor(() =>
      expect(screen.getByTestId('location')).toHaveTextContent('/day/2026-10-20'),
    );
    expect(await screen.findByRole('heading', { name: /20 октября 2026/ })).toBeInTheDocument();
  });

  it('показывает ошибку и перезагружает по «Повторить»', async () => {
    server.use(
      http.get('*/api/v1/meal-plan', () =>
        HttpResponse.json({ userMessage: 'Сбой' }, { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderApp('/');
    expect(await screen.findByText('Не удалось загрузить календарь')).toBeInTheDocument();

    mockPlan();
    await user.click(screen.getByRole('button', { name: 'Повторить' }));
    expect(
      await screen.findByRole('link', { name: '15 октября, открыть день' }),
    ).toBeInTheDocument();
    expect(screen.queryByText('Не удалось загрузить календарь')).toBeNull();
  });
});
