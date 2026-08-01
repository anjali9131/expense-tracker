import { useCallback, useEffect, useMemo, useState } from 'react';
import ExpenseForm from './components/ExpenseForm.jsx';
import ExpenseList from './components/ExpenseList.jsx';
import Summary from './components/Summary.jsx';
import { createExpense, deleteExpense, fetchExpenses, fetchSummary } from './api.js';

export default function App() {
  const [expenses, setExpenses] = useState([]);
  const [allExpenses, setAllExpenses] = useState([]);
  const [summary, setSummary] = useState(null);
  const [activeCategory, setActiveCategory] = useState('');
  const [loadError, setLoadError] = useState(null);
  const [loading, setLoading] = useState(true);

  // Fetches the filtered list for display, plus the unfiltered list (used only
  // to populate the category dropdown) and the summary, in parallel.
  const loadData = useCallback(async (category) => {
    setLoadError(null);
    try {
      const [expenseData, everything, summaryData] = await Promise.all([
        fetchExpenses(category),
        fetchExpenses(),
        fetchSummary(),
      ]);
      setExpenses(expenseData);
      setAllExpenses(everything);
      setSummary(summaryData);
    } catch (err) {
      setLoadError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData(activeCategory);
  }, [activeCategory, loadData]);

  const categories = useMemo(() => {
    const all = new Set(allExpenses.map((e) => e.category));
    return Array.from(all).sort();
  }, [allExpenses]);

  async function handleAdd(expense) {
    await createExpense(expense);
    await loadData(activeCategory);
  }

  async function handleDelete(id) {
    await deleteExpense(id);
    await loadData(activeCategory);
  }

  return (
    <div className="app">
      <header>
        <h1>💰 Smart Expense Tracker</h1>
      </header>

      {loadError && (
        <div className="banner-error">
          Couldn't reach the API at localhost:8080 — is the backend running? ({loadError})
        </div>
      )}

      <main>
        <div className="left-column">
          <ExpenseForm onAdd={handleAdd} />
          <Summary summary={summary} />
        </div>
        <div className="right-column">
          {loading ? (
            <p>Loading…</p>
          ) : (
            <ExpenseList
              expenses={expenses}
              categories={categories}
              activeCategory={activeCategory}
              onFilterChange={setActiveCategory}
              onDelete={handleDelete}
            />
          )}
        </div>
      </main>
    </div>
  );
}
