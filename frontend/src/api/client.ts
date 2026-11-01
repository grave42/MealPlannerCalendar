export class ApiError extends Error {
  readonly status: number;
  readonly userMessage: string;

  constructor(status: number, userMessage: string) {
    super(userMessage);
    this.name = 'ApiError';
    this.status = status;
    this.userMessage = userMessage;
  }
}

const DEFAULT_ERROR = 'Что-то пошло не так. Попробуйте ещё раз';

/** Возвращает понятное пользователю сообщение для любой ошибки. */
export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.userMessage : DEFAULT_ERROR;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, init);
  } catch {
    throw new ApiError(0, 'Нет связи с сервером. Проверьте подключение');
  }

  if (!response.ok) {
    let message = DEFAULT_ERROR;
    try {
      const body = (await response.json()) as { userMessage?: unknown };
      if (typeof body.userMessage === 'string' && body.userMessage) message = body.userMessage;
    } catch {
      // тело ответа не JSON — используем сообщение по умолчанию
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export function jsonBody(method: string, body: unknown): RequestInit {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}
