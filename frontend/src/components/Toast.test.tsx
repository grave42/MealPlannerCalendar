import { act, render, screen } from '@testing-library/react';
import { TOAST_DURATION_MS, ToastProvider, useToast } from './Toast';

function Trigger() {
  const { showError } = useToast();
  return <button onClick={() => showError('Не удалось добавить блюдо')}>Показать</button>;
}

describe('Toast', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('показывает уведомление об ошибке ровно 5 секунд', () => {
    render(
      <ToastProvider>
        <Trigger />
      </ToastProvider>,
    );
    act(() => screen.getByRole('button', { name: 'Показать' }).click());
    expect(screen.getByRole('alert')).toHaveTextContent('Не удалось добавить блюдо');

    act(() => {
      vi.advanceTimersByTime(TOAST_DURATION_MS - 1);
    });
    expect(screen.getByRole('alert')).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(screen.queryByRole('alert')).toBeNull();
    expect(TOAST_DURATION_MS).toBe(5000);
  });
});
