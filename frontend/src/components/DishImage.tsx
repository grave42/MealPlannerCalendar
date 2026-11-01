import { dishImageUrl } from '../api/dishes';
import styles from './DishImage.module.css';

interface DishImageProps {
  dishId: string;
  name: string;
  hasImage: boolean;
  className?: string;
}

/** Картинка блюда или нейтральная серая заглушка. */
export function DishImage({ dishId, name, hasImage, className = '' }: DishImageProps) {
  if (!hasImage) {
    return <div className={`${styles.placeholder} ${className}`} aria-hidden="true" />;
  }
  return (
    <img
      className={`${styles.image} ${className}`}
      src={dishImageUrl(dishId)}
      alt={name}
      loading="lazy"
    />
  );
}
