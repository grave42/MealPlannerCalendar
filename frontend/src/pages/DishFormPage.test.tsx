import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import type { ExternalRecipe, ExternalRecipeSearchItem } from '../api/types';
import { makeDish } from '../test/fixtures';
import { renderApp } from '../test/render';
import { server } from '../test/server';

interface Calls {
  post: Record<string, unknown>[];
  put: Record<string, unknown>[];
  imagePut: number;
  imageDelete: number;
}

function setup(): Calls {
  const calls: Calls = { post: [], put: [], imagePut: 0, imageDelete: 0 };
  server.use(
    http.get('*/api/v1/dish', () => HttpResponse.json([])),
    http.get('*/api/v1/meal-plan', () => HttpResponse.json([])),
    http.post('*/api/v1/dish', async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      calls.post.push(body);
      return HttpResponse.json(makeDish({ id: 'new-1', name: String(body.name) }), { status: 201 });
    }),
    http.put('*/api/v1/dish/:id', async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>;
      calls.put.push(body);
      return HttpResponse.json(makeDish({ name: String(body.name) }));
    }),
    http.put('*/api/v1/dish/:id/image', () => {
      calls.imagePut += 1;
      return new HttpResponse(null, { status: 204 });
    }),
    http.delete('*/api/v1/dish/:id/image', () => {
      calls.imageDelete += 1;
      return new HttpResponse(null, { status: 204 });
    }),
  );
  return calls;
}

const pasta1: ExternalRecipeSearchItem = {
  id: '1',
  title: 'Паста аль-помодоро',
  imageUrl: 'https://img.iamcook.ru/1.jpg',
};
const pasta2: ExternalRecipeSearchItem = { id: '2', title: 'Паста карбонара', imageUrl: null };
const recipe: ExternalRecipe = {
  id: '1',
  title: 'Паста аль-помодоро',
  description: 'Классика',
  ingredients: 'Паста\nТоматы',
  instructions: 'Сварить\nСмешать',
  imageUrl: 'https://img.iamcook.ru/1.jpg',
};

function mockExternal(
  search: ExternalRecipeSearchItem[] | number,
  detail: ExternalRecipe = recipe,
) {
  server.use(
    http.get('*/api/v1/external-recipe/search', () =>
      typeof search === 'number'
        ? HttpResponse.json(
            {
              userMessage:
                'Не удалось получить данные с iamcook.ru. Попробуйте позже или заполните блюдо вручную',
            },
            { status: search },
          )
        : HttpResponse.json(search),
    ),
    http.get('*/api/v1/external-recipe', () => HttpResponse.json(detail)),
  );
}

const nameField = () => screen.getByRole('textbox', { name: 'Название' });
const userNoAccept = () => userEvent.setup({ applyAccept: false });

describe('DishFormPage', () => {
  it('не отправляет форму с пустым названием и показывает ошибку под полем', async () => {
    const calls = setup();
    const user = userEvent.setup();
    renderApp('/dishes/new');
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));
    expect(await screen.findByText('Введите название блюда')).toBeInTheDocument();
    expect(nameField()).toHaveAttribute('aria-invalid', 'true');
    expect(calls.post).toHaveLength(0);
  });

  it('отклоняет слишком длинное название', async () => {
    const calls = setup();
    const user = userEvent.setup();
    renderApp('/dishes/new');
    await user.click(nameField());
    await user.paste('а'.repeat(201));
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));
    expect(await screen.findByText('Название не длиннее 200 символов')).toBeInTheDocument();
    expect(calls.post).toHaveLength(0);
  });

  it('создаёт блюдо только с названием и возвращает к списку', async () => {
    const calls = setup();
    const user = userEvent.setup();
    renderApp('/dishes/new');
    await user.type(nameField(), 'Омлет');
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/dishes'));
    expect(calls.post).toEqual([
      { name: 'Омлет', description: '', ingredients: '', instructions: '', mealTypes: [] },
    ]);
  });

  it('сохраняет выбранные метки', async () => {
    const calls = setup();
    const user = userEvent.setup();
    renderApp('/dishes/new');
    await user.type(nameField(), 'Каша');
    await user.click(screen.getByRole('checkbox', { name: 'Перекус' }));
    await user.click(screen.getByRole('checkbox', { name: 'Завтрак' }));
    expect(
      screen.getByText('Без меток блюдо предлагается для любого приёма пищи'),
    ).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(calls.post).toHaveLength(1));
    expect(calls.post[0].mealTypes).toEqual(['BREAKFAST', 'SNACK']);
  });

  it('загружает блюдо для редактирования и сохраняет изменённые метки через PUT', async () => {
    const calls = setup();
    server.use(
      http.get('*/api/v1/dish/:id', () =>
        HttpResponse.json(
          makeDish({
            id: 'x',
            name: 'Омлет',
            description: 'Вкусно',
            mealTypes: ['BREAKFAST'],
            hasImage: true,
          }),
        ),
      ),
    );
    const user = userEvent.setup();
    renderApp('/dishes/x/edit');
    await waitFor(() => expect(nameField()).toHaveValue('Омлет'));
    expect(screen.getByRole('checkbox', { name: 'Завтрак' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Обед' })).not.toBeChecked();
    expect(screen.getByAltText('Омлет')).toHaveAttribute('src', '/api/v1/dish/x/image');
    expect(screen.queryByText('Найти на iamcook.ru')).toBeNull();

    await user.click(screen.getByRole('checkbox', { name: 'Перекус' }));
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(calls.put).toHaveLength(1));
    expect(calls.put[0]).toMatchObject({
      name: 'Омлет',
      description: 'Вкусно',
      mealTypes: ['BREAKFAST', 'SNACK'],
    });
    expect(calls.imageDelete).toBe(0);
  });

  it('удаляет картинку при редактировании после «Убрать»', async () => {
    const calls = setup();
    server.use(
      http.get('*/api/v1/dish/:id', () =>
        HttpResponse.json(makeDish({ id: 'x', name: 'Омлет', hasImage: true })),
      ),
    );
    const user = userEvent.setup();
    renderApp('/dishes/x/edit');
    await waitFor(() => expect(nameField()).toHaveValue('Омлет'));
    await user.click(screen.getByRole('button', { name: 'Убрать' }));
    expect(screen.queryByAltText('Омлет')).toBeNull();
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(calls.imageDelete).toBe(1));
  });

  it('не принимает файл неподходящего типа', async () => {
    setup();
    const user = userNoAccept();
    renderApp('/dishes/new');
    const input = screen.getByLabelText('Выбрать файл', { selector: 'input' });
    await user.upload(input, new File(['x'], 'doc.txt', { type: 'text/plain' }));
    expect(
      await screen.findByText('Допустимы изображения JPG, PNG, WEBP до 5 МБ'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('img')).toBeNull();
  });

  it('не принимает файл больше 5 МБ', async () => {
    setup();
    const user = userNoAccept();
    renderApp('/dishes/new');
    const big = new File(['x'], 'big.png', { type: 'image/png' });
    Object.defineProperty(big, 'size', { value: 5 * 1024 * 1024 + 1 });
    await user.upload(screen.getByLabelText('Выбрать файл', { selector: 'input' }), big);
    expect(
      await screen.findByText('Допустимы изображения JPG, PNG, WEBP до 5 МБ'),
    ).toBeInTheDocument();
  });

  it('загружает выбранную картинку после сохранения блюда', async () => {
    const calls = setup();
    const user = userNoAccept();
    renderApp('/dishes/new');
    await user.type(nameField(), 'Омлет');
    await user.upload(
      screen.getByLabelText('Выбрать файл', { selector: 'input' }),
      new File(['x'], 'a.png', { type: 'image/png' }),
    );
    expect(await screen.findByAltText('Омлет')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(calls.imagePut).toBe(1));
    expect(calls.post).toHaveLength(1);
  });

  it('показывает ошибку сервера над формой', async () => {
    setup();
    server.use(
      http.post('*/api/v1/dish', () =>
        HttpResponse.json({ userMessage: 'Сервер недоступен' }, { status: 500 }),
      ),
    );
    const user = userEvent.setup();
    renderApp('/dishes/new');
    await user.type(nameField(), 'Омлет');
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Сервер недоступен');
    expect(screen.getByRole('button', { name: 'Сохранить' })).toBeEnabled();
  });

  it('возвращает на день из returnTo после сохранения и по «Отмена»', async () => {
    setup();
    const user = userEvent.setup();
    renderApp('/dishes/new?returnTo=%2Fday%2F2026-10-15');
    await user.type(nameField(), 'Омлет');
    await user.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() =>
      expect(screen.getByTestId('location')).toHaveTextContent('/day/2026-10-15'),
    );
  });

  it('отмена возвращает на список блюд', async () => {
    setup();
    const user = userEvent.setup();
    renderApp('/dishes/new');
    await user.click(screen.getByRole('link', { name: 'Отмена' }));
    expect(screen.getByTestId('location')).toHaveTextContent('/dishes');
  });

  describe('поиск на iamcook.ru', () => {
    it('показывает несколько результатов, заполняет поля выбранным и отправляет sourceImageUrl', async () => {
      const calls = setup();
      mockExternal([pasta1, pasta2]);
      const user = userEvent.setup();
      renderApp('/dishes/new');

      await user.type(screen.getByRole('textbox', { name: /Название блюда, например/ }), 'паста');
      await user.click(screen.getByRole('button', { name: 'Найти' }));

      expect(await screen.findByText('Паста карбонара')).toBeInTheDocument();
      expect(screen.getByAltText('Паста аль-помодоро')).toBeInTheDocument();
      const row = screen.getByText('Паста аль-помодоро').closest('li')!;
      await user.click(within(row).getByRole('button', { name: 'Выбрать' }));

      await waitFor(() => expect(nameField()).toHaveValue('Паста аль-помодоро'));
      expect(screen.getByRole('textbox', { name: 'Описание' })).toHaveValue('Классика');
      expect(screen.getByRole('textbox', { name: 'Состав' })).toHaveValue('Паста\nТоматы');
      expect(screen.getByRole('textbox', { name: 'Приготовление' })).toHaveValue(
        'Сварить\nСмешать',
      );
      expect(screen.getByAltText('Паста аль-помодоро')).toHaveAttribute(
        'src',
        'https://img.iamcook.ru/1.jpg',
      );

      await user.click(screen.getByRole('button', { name: 'Сохранить' }));
      await waitFor(() => expect(calls.post).toHaveLength(1));
      expect(calls.post[0]).toMatchObject({
        name: 'Паста аль-помодоро',
        sourceImageUrl: 'https://img.iamcook.ru/1.jpg',
      });
    });

    it('показывает единственный результат и требует выбора', async () => {
      setup();
      mockExternal([pasta1]);
      const user = userEvent.setup();
      renderApp('/dishes/new');
      await user.type(
        screen.getByRole('textbox', { name: /Название блюда, например/ }),
        'паста аль помодоро',
      );
      await user.click(screen.getByRole('button', { name: 'Найти' }));
      expect(await screen.findByRole('button', { name: 'Выбрать' })).toBeInTheDocument();
      expect(nameField()).toHaveValue('');
    });

    it('показывает «Ищем…» во время запроса', async () => {
      setup();
      let release: () => void = () => undefined;
      const gate = new Promise<void>((resolve) => (release = resolve));
      server.use(
        http.get('*/api/v1/external-recipe/search', async () => {
          await gate;
          return HttpResponse.json([]);
        }),
      );
      const user = userEvent.setup();
      renderApp('/dishes/new');
      await user.type(screen.getByRole('textbox', { name: /Название блюда, например/ }), 'паста');
      await user.click(screen.getByRole('button', { name: 'Найти' }));
      expect(await screen.findByRole('button', { name: 'Ищем…' })).toBeDisabled();
      release();
      expect(await screen.findByRole('button', { name: 'Найти' })).toBeEnabled();
    });

    it('спрашивает подтверждение перед заменой заполненных полей', async () => {
      setup();
      mockExternal([pasta1]);
      const user = userEvent.setup();
      renderApp('/dishes/new');
      await user.type(nameField(), 'Моё блюдо');
      await user.type(screen.getByRole('textbox', { name: /Название блюда, например/ }), 'паста');
      await user.click(screen.getByRole('button', { name: 'Найти' }));
      await user.click(await screen.findByRole('button', { name: 'Выбрать' }));

      let dialog = await screen.findByRole('dialog');
      expect(dialog).toHaveTextContent('Заменить заполненные поля данными с iamcook.ru?');
      await user.click(within(dialog).getByRole('button', { name: 'Отмена' }));
      expect(nameField()).toHaveValue('Моё блюдо');

      await user.click(screen.getByRole('button', { name: 'Выбрать' }));
      dialog = await screen.findByRole('dialog');
      await user.click(within(dialog).getByRole('button', { name: 'Заменить' }));
      await waitFor(() => expect(nameField()).toHaveValue('Паста аль-помодоро'));
    });

    it('сообщает, что ничего не найдено', async () => {
      setup();
      mockExternal([]);
      const user = userEvent.setup();
      renderApp('/dishes/new');
      await user.type(
        screen.getByRole('textbox', { name: /Название блюда, например/ }),
        'абракадабра',
      );
      await user.click(screen.getByRole('button', { name: 'Найти' }));
      expect(
        await screen.findByText('Ничего не найдено. Заполните блюдо вручную'),
      ).toBeInTheDocument();
    });

    it('показывает userMessage при ошибке 502 и оставляет форму доступной', async () => {
      setup();
      mockExternal(502);
      const user = userEvent.setup();
      renderApp('/dishes/new');
      await user.type(screen.getByRole('textbox', { name: /Название блюда, например/ }), 'паста');
      await user.click(screen.getByRole('button', { name: 'Найти' }));
      expect(
        await screen.findByText(/Не удалось получить данные с iamcook.ru/),
      ).toBeInTheDocument();
      await user.type(nameField(), 'Вручную');
      expect(nameField()).toHaveValue('Вручную');
      expect(screen.getByRole('button', { name: 'Сохранить' })).toBeEnabled();
    });
  });
});
