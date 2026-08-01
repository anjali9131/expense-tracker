export default function ExpenseList({ expenses, categories, activeCategory, onFilterChange, onDelete }) {
  return (
    <div className="expense-list">
      <div className="list-header">
        <h2>Expenses</h2>
        <label className="filter">
          Filter by category
          <select value={activeCategory} onChange={(e) => onFilterChange(e.target.value)}>
            <option value="">All categories</option>
            {categories.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </select>
        </label>
      </div>

      {expenses.length === 0 ? (
        <p className="empty-state">No expenses yet — add one above.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Title</th>
              <th>Category</th>
              <th>Amount</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {expenses.map((expense) => (
              <tr key={expense.id}>
                <td>{expense.date}</td>
                <td>{expense.title}</td>
                <td>
                  <span className="category-pill">{expense.category}</span>
                </td>
                <td className="amount">${Number(expense.amount).toFixed(2)}</td>
                <td>
                  <button className="delete-btn" onClick={() => onDelete(expense.id)} aria-label={`Delete ${expense.title}`}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
