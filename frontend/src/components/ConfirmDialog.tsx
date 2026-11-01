import { Modal } from './Modal';
import styles from './ConfirmDialog.module.css';

interface ConfirmDialogProps {
  title: string;
  message: string;
  confirmLabel: string;
  danger?: boolean;
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  title,
  message,
  confirmLabel,
  danger = false,
  busy = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p className={styles.message}>{message}</p>
      <div className={styles.actions}>
        <button
          type="button"
          className={`btn ${danger ? 'btn-danger-solid' : 'btn-primary'}`}
          onClick={onConfirm}
          disabled={busy}
        >
          {confirmLabel}
        </button>
        <button type="button" className="btn btn-secondary" onClick={onCancel}>
          Отмена
        </button>
      </div>
    </Modal>
  );
}
