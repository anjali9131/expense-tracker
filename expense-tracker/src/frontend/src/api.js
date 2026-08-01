const BASE_URL = 'http://localhost:8080/api/expenses';

async function handleResponse(response) {
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const message = data?.message || `Request failed with status ${response.status}`;
    const details = data?.details?.length ? ` (${data.details.join(', ')})` : '';
    throw new Error(message + details);
  }
  return data;
}

export async function fetchExpenses(category) {
  const url = category ? `${BASE_URL}?category=${encodeURIComponent(category)}` : BASE_URL;
  const response = await fetch(url);
  return handleResponse(response);
}

export async function fetchSummary() {
  const response = await fetch(`${BASE_URL}/summary`);
  return handleResponse(response);
}

export async function createExpense(expense) {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(expense),
  });
  return handleResponse(response);
}

export async function deleteExpense(id) {
  const response = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' });
  return handleResponse(response);
}
