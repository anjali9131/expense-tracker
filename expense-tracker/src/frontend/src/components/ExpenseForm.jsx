import { useState } from 'react';

const EMPTY_FORM = { title: '', amount: '', category: '', date: '' };

export default function ExpenseForm({ onAdd }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onAdd({
        title: form.title.trim(),
        amount: Number(form.amount),
        category: form.category.trim(),
        date: form.date,
      });
      setForm(EMPTY_FORM);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="expense-form" onSubmit={handleSubmit}>
      <h2>Add Expense</h2>
      <div className="form-row">
        <label>
          Title
          <input
            type="text"
            required
            value={form.title}
            onChange={(e) => updateField('title', e.target.value)}
            placeholder="Groceries"
          />
        </label>
        <label>
          Amount
          <input
            type="number"
            step="0.01"
            min="0.01"
            required
            value={form.amount}
            onChange={(e) => updateField('amount', e.target.value)}
            placeholder="45.50"
          />
        </label>
      </div>
      <div className="form-row">
        <label>
          Category
          <input
            type="text"
            required
            value={form.category}
            onChange={(e) => updateField('category', e.target.value)}
            placeholder="Food"
          />
        </label>
        <label>
          Date
          <input
            type="date"
            required
            value={form.date}
            onChange={(e) => updateField('date', e.target.value)}
          />
        </label>
      </div>
      {error && <p className="form-error">{error}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Adding…' : 'Add Expense'}
      </button>
    </form>
  );
}
