<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppMessage from '../components/common/AppMessage.vue'
import { handoverStatusLabels, leaveTypeLabels } from '../constants/masterData.js'
import { useApplicationsStore } from '../stores/applications.js'

const router = useRouter()
const applicationsStore = useApplicationsStore()
const errorMessage = ref('')
const fieldErrors = ref([])
const draft = computed(() => applicationsStore.draft)

async function submit() {
  errorMessage.value = ''
  fieldErrors.value = []
  try {
    const created = await applicationsStore.submitDraft()
    if (created) {
      await router.replace({ name: 'application-complete', params: { id: created.id } })
    }
  } catch (error) {
    if (error.details.length > 0 || error.status === 400 || error.status === 409) {
      applicationsStore.setSubmissionErrors(error)
      await router.push({ name: 'application-new' })
      return
    }

    errorMessage.value = error.message
    fieldErrors.value = error.details
  }
}
</script>

<template>
  <div class="content-card">
    <h1 class="page-title">申請内容確認</h1>

    <template v-if="draft">
      <AppMessage v-if="errorMessage" type="error" :text="errorMessage" />
      <v-alert v-if="fieldErrors.length" type="warning" variant="tonal" class="mb-4">
        <ul class="pl-5">
          <li v-for="item in fieldErrors" :key="`${item.field}-${item.message}`">
            {{ item.message }}
          </li>
        </ul>
      </v-alert>

      <v-card>
        <v-list lines="two">
          <v-list-item title="休暇種別" :subtitle="leaveTypeLabels[draft.leaveType] ?? draft.leaveType" />
          <v-list-item title="期間" :subtitle="`${draft.startDate || '-'} ～ ${draft.endDate || '-'}`" />
          <v-list-item title="申請理由" :subtitle="draft.reason || '-'" />
          <v-list-item title="引継ぎ状況" :subtitle="handoverStatusLabels[draft.handoverStatus] ?? draft.handoverStatus" />
          <v-list-item title="連絡事項" :subtitle="draft.note || '-'" />
        </v-list>
        <v-card-actions class="justify-end flex-wrap pa-4">
          <v-btn variant="outlined" :disabled="applicationsStore.submitting" :to="{ name: 'application-new' }">
            修正する
          </v-btn>
          <v-btn
            color="primary"
            :loading="applicationsStore.submitting"
            :disabled="applicationsStore.submitting"
            @click="submit"
          >
            申請する
          </v-btn>
        </v-card-actions>
      </v-card>
    </template>

    <v-alert v-else type="warning" variant="tonal">
      確認する申請内容がありません。
      <div class="mt-3">
        <v-btn color="primary" :to="{ name: 'application-new' }">申請画面へ</v-btn>
      </div>
    </v-alert>
  </div>
</template>
