import styles from './Skeleton.module.css';

interface SkeletonProps {
  label: string;
  count?: number;
  className?: string;
}

/** Серые блоки-заглушки на время загрузки. */
export function Skeleton({ label, count = 1, className = '' }: SkeletonProps) {
  return (
    <div role="status" aria-label={label} aria-busy="true" className={styles.wrap}>
      {Array.from({ length: count }, (_, i) => (
        <div key={i} className={`${styles.block} ${className}`} />
      ))}
    </div>
  );
}
