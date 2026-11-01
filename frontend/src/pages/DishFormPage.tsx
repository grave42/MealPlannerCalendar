import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import {
  createDish,
  deleteDishImage,
  dishImageUrl,
  getDish,
  updateDish,
  uploadDishImage,
} from '../api/dishes';
import type { Dish, ExternalRecipe, MealType } from '../api/types';
import { ExternalRecipeSearch } from '../components/ExternalRecipeSearch';
import { LoadError } from '../components/LoadError';
import { Skeleton } from '../components/Skeleton';
import { MEAL_LABELS, MEAL_TYPES } from '../utils/dates';
import styles from './DishFormPage.module.css';

const MAX_IMAGE_SIZE = 5 * 1024 * 1024;
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const IMAGE_ERROR = 'Допустимы изображения JPG, PNG, WEBP до 5 МБ';

function safeReturnTo(value: string | null): string | null {
  return value && value.startsWith('/') && !value.startsWith('//') ? value : null;
}

export function DishFormPage() {
  const { id } = useParams<{ id: string }>();
  const query = useQuery({
    queryKey: ['dish', id],
    queryFn: () => getDish(id!),
    enabled: Boolean(id),
  });

  if (!id) return <DishForm />;
  if (query.isPending) {
    return (
      <div className="page">
        <Skeleton label="Загрузка блюда" count={4} />
      </div>
    );
  }
  if (query.isError) {
    return (
      <div className="page">
        <LoadError message="Не удалось загрузить блюдо" onRetry={() => void query.refetch()} />
      </div>
    );
  }
  return <DishForm dish={query.data} />;
}

function DishForm({ dish }: { dish?: Dish }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const returnTo = safeReturnTo(params.get('returnTo')) ?? '/dishes';

  const [name, setName] = useState(dish?.name ?? '');
  const [description, setDescription] = useState(dish?.description ?? '');
  const [ingredients, setIngredients] = useState(dish?.ingredients ?? '');
  const [instructions, setInstructions] = useState(dish?.instructions ?? '');
  const [mealTypes, setMealTypes] = useState<MealType[]>(dish?.mealTypes ?? []);

  const [file, setFile] = useState<File | null>(null);
  const [filePreview, setFilePreview] = useState<string | null>(null);
  const [sourceImageUrl, setSourceImageUrl] = useState<string | null>(null);
  const [removeImage, setRemoveImage] = useState(false);

  const [nameError, setNameError] = useState<string | null>(null);
  const [imageError, setImageError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);

  const savedId = useRef<string | undefined>(dish?.id);
  const fileInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    return () => {
      if (filePreview) URL.revokeObjectURL(filePreview);
    };
  }, [filePreview]);

  const existingImage = Boolean(dish?.hasImage) && !removeImage;
  const imageSrc =
    filePreview ?? sourceImageUrl ?? (existingImage && dish ? dishImageUrl(dish.id) : null);

  const hasFilledData = [name, description, ingredients, instructions].some((v) => v.trim() !== '');

  const toggleType = (type: MealType) =>
    setMealTypes((prev) =>
      MEAL_TYPES.filter((t) => (t === type ? !prev.includes(t) : prev.includes(t))),
    );

  const handleFile = (e: ChangeEvent<HTMLInputElement>) => {
    const picked = e.target.files?.[0];
    e.target.value = '';
    if (!picked) return;
    if (!ALLOWED_IMAGE_TYPES.includes(picked.type) || picked.size > MAX_IMAGE_SIZE) {
      setImageError(IMAGE_ERROR);
      return;
    }
    setImageError(null);
    setFile(picked);
    setFilePreview(URL.createObjectURL(picked));
    setSourceImageUrl(null);
  };

  const clearImage = () => {
    setFile(null);
    setFilePreview(null);
    setSourceImageUrl(null);
    setImageError(null);
    if (dish?.hasImage) setRemoveImage(true);
  };

  const applyRecipe = (recipe: ExternalRecipe) => {
    setName(recipe.title ?? '');
    setDescription(recipe.description ?? '');
    setIngredients(recipe.ingredients ?? '');
    setInstructions(recipe.instructions ?? '');
    setFile(null);
    setFilePreview(null);
    setImageError(null);
    setSourceImageUrl(recipe.imageUrl);
    setNameError(null);
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const input = {
        name: name.trim(),
        description,
        ingredients,
        instructions,
        mealTypes,
      };
      let dishId = savedId.current;
      if (dishId) {
        await updateDish(dishId, input);
      } else {
        const created = await createDish({
          ...input,
          ...(sourceImageUrl && !file ? { sourceImageUrl } : {}),
        });
        dishId = created.id;
        savedId.current = dishId;
      }
      if (file) await uploadDishImage(dishId, file);
      else if (removeImage && dish?.hasImage) await deleteDishImage(dishId);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['dishes'] });
      void queryClient.invalidateQueries({ queryKey: ['dish'] });
      void queryClient.invalidateQueries({ queryKey: ['meal-plan'] });
      navigate(returnTo);
    },
    onError: (e) => setServerError(errorMessage(e)),
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setServerError(null);
    const trimmed = name.trim();
    if (!trimmed) {
      setNameError('Введите название блюда');
      return;
    }
    if (trimmed.length > 200) {
      setNameError('Название не длиннее 200 символов');
      return;
    }
    setNameError(null);
    saveMutation.mutate();
  };

  return (
    <div className="page">
      <div className="page-head">
        <h1>{dish ? 'Редактирование блюда' : 'Новое блюдо'}</h1>
      </div>
      <div className={styles.container}>
        {!dish && <ExternalRecipeSearch hasFilledData={hasFilledData} onApply={applyRecipe} />}

        <form onSubmit={handleSubmit} noValidate className={styles.form}>
          {serverError && (
            <div role="alert" className={styles.banner}>
              {serverError}
            </div>
          )}

          <div className={styles.field}>
            <label htmlFor="dish-name">
              Название{' '}
              <span className="error-text" aria-hidden="true">
                *
              </span>
            </label>
            <input
              id="dish-name"
              className="input"
              value={name}
              aria-required="true"
              aria-invalid={nameError ? 'true' : undefined}
              aria-describedby={nameError ? 'dish-name-error' : undefined}
              onChange={(e) => setName(e.target.value)}
            />
            {nameError && (
              <p id="dish-name-error" className="error-text">
                {nameError}
              </p>
            )}
          </div>

          <div className={styles.field}>
            <label htmlFor="dish-description">Описание</label>
            <textarea
              id="dish-description"
              className="input"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="dish-ingredients">Состав</label>
            <textarea
              id="dish-ingredients"
              className="input"
              rows={6}
              value={ingredients}
              onChange={(e) => setIngredients(e.target.value)}
            />
          </div>

          <div className={styles.field}>
            <label htmlFor="dish-instructions">Приготовление</label>
            <textarea
              id="dish-instructions"
              className="input"
              rows={8}
              value={instructions}
              onChange={(e) => setInstructions(e.target.value)}
            />
          </div>

          <fieldset className={styles.fieldset}>
            <legend>Метки</legend>
            <div className={styles.tags}>
              {MEAL_TYPES.map((type) => (
                <label
                  key={type}
                  className={`${styles.tag} ${mealTypes.includes(type) ? styles.tagOn : ''}`}
                >
                  <input
                    type="checkbox"
                    className={styles.tagInput}
                    checked={mealTypes.includes(type)}
                    onChange={() => toggleType(type)}
                  />
                  {MEAL_LABELS[type]}
                </label>
              ))}
            </div>
            <p className={styles.hint}>Без меток блюдо предлагается для любого приёма пищи</p>
          </fieldset>

          <div className={styles.field}>
            <span id="dish-image-label">Картинка</span>
            {imageSrc && (
              <img
                className={styles.preview}
                src={imageSrc}
                alt={name.trim() || 'Картинка блюда'}
              />
            )}
            <div className={styles.imageActions}>
              <input
                ref={fileInput}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                aria-label="Выбрать файл"
                className={styles.fileInput}
                tabIndex={-1}
                onChange={handleFile}
              />
              <button type="button" className="btn" onClick={() => fileInput.current?.click()}>
                Выбрать файл
              </button>
              {imageSrc && (
                <button type="button" className="btn" onClick={clearImage}>
                  Убрать
                </button>
              )}
            </div>
            {imageError && <p className="error-text">{imageError}</p>}
          </div>

          <div className={styles.actions}>
            <button type="submit" className="btn btn-primary" disabled={saveMutation.isPending}>
              Сохранить
            </button>
            <Link to={returnTo} className="btn btn-secondary">
              Отмена
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}
