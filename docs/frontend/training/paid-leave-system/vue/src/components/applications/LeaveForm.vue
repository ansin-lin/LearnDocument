<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { handoverStatuses, leaveTypes } from '../../constants/masterData.js'

const props = defineProps({
  initialValue: {
    type: Object,
    default: () => ({})
  },
  fieldErrors: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['submit'])

const leaveTypeField = ref(null)
const startDateField = ref(null)
const endDateField = ref(null)
const reasonField = ref(null)
const handoverStatusField = ref(null)
const noteField = ref(null)

const form = reactive({
  leaveType: '',
  startDate: '',
  endDate: '',
  reason: '',
  handoverStatus: '',
  note: ''
})

const clientErrors = reactive({})
const serverErrors = reactive({})
const touched = reactive({})
const errors = computed(() => ({ ...serverErrors, ...clientErrors }))

const fieldRefs = {
  leaveType: leaveTypeField,
  startDate: startDateField,
  endDate: endDateField,
  reason: reasonField,
  handoverStatus: handoverStatusField,
  note: noteField
}

const fieldOrder = ['leaveType', 'startDate', 'endDate', 'reason', 'handoverStatus', 'note']

function todayInJapan() {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Tokyo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(new Date())
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]))
  return `${values.year}-${values.month}-${values.day}`
}

const minimumDate = todayInJapan()

watch(
  () => props.initialValue,
  (value) => Object.assign(form, value),
  { immediate: true }
)

watch(
  () => props.fieldErrors,
  (value) => {
    Object.keys(serverErrors).forEach((field) => delete serverErrors[field])
    Object.assign(serverErrors, value)
  },
  { immediate: true }
)

function validateField(field) {
  delete clientErrors[field]
  delete serverErrors[field]

  if (field === 'leaveType' && !leaveTypes.some((item) => item.value === form.leaveType)) {
    clientErrors.leaveType = '休暇種別を選択してください。'
  }

  if (field === 'startDate') {
    if (!form.startDate) clientErrors.startDate = '開始日を入力してください。'
    else if (form.startDate < minimumDate) clientErrors.startDate = '開始日は本日以降の日付を入力してください。'
  }

  if (field === 'endDate') {
    if (!form.endDate) clientErrors.endDate = '終了日を入力してください。'
    else if (form.startDate && form.endDate < form.startDate) {
      clientErrors.endDate = '終了日は開始日以降の日付を入力してください。'
    } else if (['half-am', 'half-pm'].includes(form.leaveType) && form.startDate !== form.endDate) {
      clientErrors.endDate = '半日休暇の開始日と終了日は同じ日にしてください。'
    }
  }

  if (field === 'reason') {
    if (!form.reason.trim()) clientErrors.reason = '申請理由を入力してください。'
    else if (form.reason.trim().length > 200) clientErrors.reason = '申請理由は200文字以内で入力してください。'
  }

  if (field === 'handoverStatus'
      && !handoverStatuses.some((item) => item.value === form.handoverStatus)) {
    clientErrors.handoverStatus = '引継ぎ状況を選択してください。'
  }

  if (field === 'note') {
    if (form.note.trim().length > 300) clientErrors.note = '連絡事項は300文字以内で入力してください。'
    else if (form.leaveType === 'special' && !form.note.trim()) {
      clientErrors.note = '特別休暇の制度名を入力してください。'
    }
  }

  return !clientErrors[field]
}

function validateRelatedFields(field) {
  touched[field] = true
  validateField(field)

  if (field === 'leaveType') {
    if (touched.endDate) validateField('endDate')
    if (touched.note) validateField('note')
  }
  if (field === 'startDate' && touched.endDate) validateField('endDate')
}

async function validateAfterUpdate(field) {
  await nextTick()
  validateRelatedFields(field)
}

async function submit() {
  fieldOrder.forEach((field) => {
    touched[field] = true
    validateField(field)
  })

  const firstError = fieldOrder.find((field) => errors.value[field])
  if (firstError) {
    await nextTick()
    fieldRefs[firstError].value?.focus()
    return
  }

  emit('submit', { ...form })
}
</script>

<template>
  <v-form novalidate @submit.prevent="submit">
    <v-select
      ref="leaveTypeField"
      v-model="form.leaveType"
      label="休暇種別"
      :items="leaveTypes"
      :error-messages="errors.leaveType"
      @update:model-value="validateAfterUpdate('leaveType')"
    />

    <v-row>
      <v-col cols="12" sm="6">
        <v-text-field
          ref="startDateField"
          v-model="form.startDate"
          label="開始日"
          type="date"
          :min="minimumDate"
          :error-messages="errors.startDate"
          @blur="validateRelatedFields('startDate')"
          @update:model-value="validateAfterUpdate('startDate')"
        />
      </v-col>
      <v-col cols="12" sm="6">
        <v-text-field
          ref="endDateField"
          v-model="form.endDate"
          label="終了日"
          type="date"
          :min="form.startDate || minimumDate"
          :error-messages="errors.endDate"
          @blur="validateRelatedFields('endDate')"
          @update:model-value="validateAfterUpdate('endDate')"
        />
      </v-col>
    </v-row>

    <v-textarea
      ref="reasonField"
      v-model="form.reason"
      label="申請理由"
      rows="3"
      counter="200"
      maxlength="200"
      :error-messages="errors.reason"
      @blur="validateRelatedFields('reason')"
    />

    <v-radio-group
      ref="handoverStatusField"
      v-model="form.handoverStatus"
      label="引継ぎ状況"
      inline
      :error-messages="errors.handoverStatus"
      @update:model-value="validateAfterUpdate('handoverStatus')"
    >
      <v-radio
        v-for="item in handoverStatuses"
        :key="item.value"
        :label="item.title"
        :value="item.value"
      />
    </v-radio-group>

    <v-textarea
      ref="noteField"
      v-model="form.note"
      label="連絡事項"
      rows="3"
      counter="300"
      maxlength="300"
      :error-messages="errors.note"
      @blur="validateRelatedFields('note')"
    />

    <div class="d-flex justify-end">
      <v-btn type="submit" color="primary" size="large">
        確認画面へ
      </v-btn>
    </div>
  </v-form>
</template>
