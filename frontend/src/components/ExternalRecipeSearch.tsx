import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import type { FormEvent } from 'react';
import { errorMessage } from '../api/client';
import { getExternalRecipe, searchExternalRecipes } from '../api/externalRecipes';
import type { ExternalRecipe, ExternalRecipeSearchItem } from '../api/types';
import { ConfirmDialog } from './ConfirmDialog';
import styles from './ExternalRecipeSearch.module.css';

interface ExternalRecipeSearchProps {
  /** Есть ли в форме уже введённые данные, которые импорт перезапишет. */
  hasFilledData: boolean;
  onApply: (recipe: ExternalRecipe) => void;
}

/** Блок «Найти на iamcook.ru»: поиск рецепта и подстановка данных в форму. */
export function ExternalRecipeSearch({ hasFilledData, onApply }: ExternalRecipeSearchProps) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<ExternalRecipeSearchItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loadingId, setLoadingId] = useState<string | null>(null);
  const [pending, setPending] = useState<ExternalRecipe | null>(null);

  const apply = (recipe: ExternalRecipe) => {
    onApply(recipe);
    setResults(null);
    setPending(null);
  };

  const searchMutation = useMutation({
    mutationFn: (text: string) => searchExternalRecipes(text),
    onSuccess: (items) => setResults(items),
    onError: (e) => {
      setResults(null);
      setError(errorMessage(e));
    },
  });

  const detailMutation = useMutation({
    mutationFn: (id: string) => getExternalRecipe(id),
    onSuccess: (recipe) => {
      if (hasFilledData) setPending(recipe);
      else apply(recipe);
    },
    onError: (e) => setError(errorMessage(e)),
    onSettled: () => setLoadingId(null),
  });

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    const text = query.trim();
    setError(null);
    if (text.length < 2) {
      setResults(null);
      setError('Введите не менее 2 символов');
      return;
    }
    searchMutation.mutate(text);
  };

  const handleSelect = (item: ExternalRecipeSearchItem) => {
    setError(null);
    setLoadingId(item.id);
    detailMutation.mutate(item.id);
  };

  return (
    <section className={styles.block} aria-labelledby="external-search-title">
      <h2 id="external-search-title" className={styles.title}>
        Найти на iamcook.ru
      </h2>
      <form className={styles.form} onSubmit={handleSearch}>
        <input
          type="text"
          className="input"
          placeholder="Название блюда, например, паста аль помодоро"
          aria-label="Название блюда, например, паста аль помодоро"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" className="btn btn-primary" disabled={searchMutation.isPending}>
          {searchMutation.isPending ? 'Ищем…' : 'Найти'}
        </button>
      </form>

      {error && <p className="error-text">{error}</p>}
      {results && results.length === 0 && <p>Ничего не найдено. Заполните блюдо вручную</p>}
      {results && results.length > 0 && (
        <ul className={styles.results}>
          {results.map((item) => (
            <li key={item.id} className={styles.row}>
              {item.imageUrl ? (
                <img className={styles.thumb} src={item.imageUrl} alt={item.title} loading="lazy" />
              ) : (
                <div className={styles.thumb} aria-hidden="true" />
              )}
              <span className={styles.rowTitle}>{item.title}</span>
              <button
                type="button"
                className="btn"
                disabled={loadingId !== null}
                onClick={() => handleSelect(item)}
              >
                {loadingId === item.id ? 'Загружаем…' : 'Выбрать'}
              </button>
            </li>
          ))}
        </ul>
      )}

      {pending && (
        <ConfirmDialog
          title="Замена данных"
          message="Заменить заполненные поля данными с iamcook.ru?"
          confirmLabel="Заменить"
          onConfirm={() => apply(pending)}
          onCancel={() => setPending(null)}
        />
      )}
    </section>
  );
}
