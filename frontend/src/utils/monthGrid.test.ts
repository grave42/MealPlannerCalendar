import { buildMonthGrid } from './monthGrid';

describe('buildMonthGrid', () => {
  it('строит обычный месяц с выравниванием по понедельнику', () => {
    // Октябрь 2026: 1 октября — четверг
    const grid = buildMonthGrid(2026, 10);
    expect(grid).toHaveLength(35);
    expect(grid[0]).toEqual({ date: '2026-09-28', day: 28, inCurrentMonth: false });
    expect(grid[3]).toEqual({ date: '2026-10-01', day: 1, inCurrentMonth: true });
    expect(grid[33]).toEqual({ date: '2026-10-31', day: 31, inCurrentMonth: true });
    expect(grid[34]).toEqual({ date: '2026-11-01', day: 1, inCurrentMonth: false });
    expect(grid.filter((c) => c.inCurrentMonth)).toHaveLength(31);
  });

  it('обрабатывает февраль високосного года', () => {
    // Февраль 2024: 29 дней, 1 февраля — четверг
    const grid = buildMonthGrid(2024, 2);
    expect(grid.filter((c) => c.inCurrentMonth)).toHaveLength(29);
    expect(grid[0].date).toBe('2024-01-29');
    expect(grid[grid.length - 1].date).toBe('2024-03-03');
    expect(grid.some((c) => c.date === '2024-02-29' && c.inCurrentMonth)).toBe(true);
  });

  it('обрабатывает февраль невисокосного года, занимающий ровно четыре недели', () => {
    // Февраль 2027: 1 февраля — понедельник, 28 дней
    const grid = buildMonthGrid(2027, 2);
    expect(grid).toHaveLength(28);
    expect(grid.every((c) => c.inCurrentMonth)).toBe(true);
  });

  it('обрабатывает месяц, начинающийся с воскресенья', () => {
    // Февраль 2026: 1 февраля — воскресенье
    const grid = buildMonthGrid(2026, 2);
    expect(grid[5]).toEqual({ date: '2026-01-31', day: 31, inCurrentMonth: false });
    expect(grid[6]).toEqual({ date: '2026-02-01', day: 1, inCurrentMonth: true });
    expect(grid[0].date).toBe('2026-01-26');
  });

  it('строит шестинедельный месяц', () => {
    // Март 2026: 1 марта — воскресенье, 31 день → 6 недель
    const grid = buildMonthGrid(2026, 3);
    expect(grid).toHaveLength(42);
    expect(grid[41].date).toBe('2026-04-05');
  });

  it('переходит через границу года', () => {
    const grid = buildMonthGrid(2026, 12);
    expect(grid[grid.length - 1].date.startsWith('2027-01')).toBe(true);
  });
});
