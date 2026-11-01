interface LoadErrorProps {
  message: string;
  onRetry: () => void;
}

export function LoadError({ message, onRetry }: LoadErrorProps) {
  return (
    <div role="alert">
      <p className="error-text">{message}</p>
      <button type="button" className="btn" onClick={onRetry}>
        Повторить
      </button>
    </div>
  );
}
