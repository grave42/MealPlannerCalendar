import { formatFullDate, formatMonthTitle, parseIsoDate, toIsoDate } from './dates';

describe('dates', () => {
  it('форматирует заголовок месяца', () => {
    expect(formatMonthTitle(2026, 10)).toBe('Октябрь 2026');
  });

  it('форматирует полную дату', () => {
    expect(formatFullDate(new Date(2026, 9, 15))).toBe('Четверг, 15 октября 2026');
  });

  it('разбирает корректную дату и отклоняет некорректную', () => {
    expect(toIsoDate(parseIsoDate('2026-10-15')!)).toBe('2026-10-15');
    expect(parseIsoDate('2026-02-30')).toBeNull();
    expect(parseIsoDate('abc')).toBeNull();
    expect(parseIsoDate(undefined)).toBeNull();
  });
});
