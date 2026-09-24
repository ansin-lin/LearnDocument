export const leaveTypes = [
  { title: '有給休暇', value: 'paid' },
  { title: '半日休暇（午前）', value: 'half-am' },
  { title: '半日休暇（午後）', value: 'half-pm' },
  { title: '特別休暇', value: 'special' }
]

export const handoverStatuses = [
  { title: '対応済', value: 'done' },
  { title: '対応不要', value: 'not-required' }
]

export const applicationStatuses = [
  { title: 'すべて', value: '' },
  { title: '申請中', value: 'pending' },
  { title: '承認済', value: 'approved' },
  { title: '差戻し', value: 'returned' },
  { title: '取消済', value: 'cancelled' }
]

export const leaveTypeLabels = Object.fromEntries(
  leaveTypes.map((item) => [item.value, item.title])
)

export const handoverStatusLabels = Object.fromEntries(
  handoverStatuses.map((item) => [item.value, item.title])
)
