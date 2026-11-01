import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { CalendarPage } from './pages/CalendarPage';
import { DayPage } from './pages/DayPage';
import { DishFormPage } from './pages/DishFormPage';
import { DishesPage } from './pages/DishesPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<CalendarPage />} />
        <Route path="/day/:date" element={<DayPage />} />
        <Route path="/dishes" element={<DishesPage />} />
        <Route path="/dishes/new" element={<DishFormPage />} />
        <Route path="/dishes/:id/edit" element={<DishFormPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
