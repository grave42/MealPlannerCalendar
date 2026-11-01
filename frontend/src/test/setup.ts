import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { server } from './server';

// Node fetch не принимает относительные адреса — разрешаем их относительно localhost.
const originalFetch = globalThis.fetch;
globalThis.fetch = ((input: RequestInfo | URL, init?: RequestInit) =>
  typeof input === 'string' && input.startsWith('/')
    ? originalFetch(new URL(input, 'http://localhost').href, init)
    : originalFetch(input, init)) as typeof fetch;

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
});
afterAll(() => server.close());
