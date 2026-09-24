import assert from 'node:assert/strict'
import test from 'node:test'
import { calculateLeaveDays, compactDate, isValidIsoDate } from '../src/utils/dates.js'

test('ISO日付を厳密に判定する', () => {
  assert.equal(isValidIsoDate('2026-09-01'), true)
  assert.equal(isValidIsoDate('2026-02-30'), false)
})

test('半日休暇は0.5日として計算する', () => {
  assert.equal(calculateLeaveDays('half-am', '2026-09-01', '2026-09-01'), 0.5)
})

test('通常休暇は開始日と終了日を含める', () => {
  assert.equal(calculateLeaveDays('paid', '2026-09-01', '2026-09-02'), 2)
  assert.equal(compactDate('2026-09-01'), '20260901')
})
