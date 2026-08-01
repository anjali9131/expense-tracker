export default function Summary({ summary }) {
  if (!summary) return null;

  const entries = Object.entries(summary.totalsByCategory || {});

  return (
    <div className="summary">
      <div className="summary-total">
        <span className="label">Total spent</span>
        <span className="value">${Number(summary.totalAmount).toFixed(2)}</span>
      </div>
      {entries.length > 0 && (
        <div className="summary-breakdown">
          {entries.map(([category, amount]) => (
            <div className="summary-row" key={category}>
              <span>{category}</span>
              <span>${Number(amount).toFixed(2)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
