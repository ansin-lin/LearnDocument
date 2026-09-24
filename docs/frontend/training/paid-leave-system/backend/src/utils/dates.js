const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

export function isValidIsoDate(value) {
  if (!ISO_DATE_PATTERN.test(value)) return false

  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))

  return date.getUTCFullYear() === year
    && date.getUTCMonth() === month - 1
    && date.getUTCDate() === day
}

export function calculateLeaveDays(leaveType, startDate, endDate) {
  if (leaveType === 'half-am' || leaveType === 'half-pm') return 0.5

  const start = Date.parse(`${startDate}T00:00:00Z`)
  const end = Date.parse(`${endDate}T00:00:00Z`)
  return (end - start) / 86_400_000 + 1
}

export function compactDate(isoDate) {
  return isoDate.replaceAll('-', '')
}
