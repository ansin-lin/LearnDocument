<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppMessage from '../components/common/AppMessage.vue'
import ApplicationStatusBadge from '../components/applications/ApplicationStatusBadge.vue'
import { getApplication } from '../api/applications.js'
import { toApiError } from '../api/http.js'
import { formatBusinessDate, formatDateTime } from '../utils/date.js'

const route = useRoute()
const application = ref(null)
const loading = ref(false)
const errorMessage = ref('')

async function loadApplication() {
  loading.value = true
  errorMessage.value = ''
  try {
    application.value = await getApplication(route.params.id)
  } catch (error) {
    errorMessage.value = toApiError(error).message
  } finally {
    loading.value = false
  }
}

onMounted(loadApplication)
</script>

<template>
  <div class="content-card">
    <h1 class="page-title">申請完了</h1>
    <AppMessage v-if="errorMessage" type="error" :text="errorMessage" />

    <div v-if="loading" class="text-center py-12">
      <v-progress-circular indeterminate color="primary" />
    </div>

    <v-card v-else-if="application" class="pa-4 pa-sm-7 text-center">
      <v-card-title class="text-h5">申請を受け付けました</v-card-title>
      <v-card-text>
        <p class="mb-2">受付番号</p>
        <p class="text-h4 font-weight-bold mb-5">{{ application.receiptNumber }}</p>
        <p class="mb-4">申請状態：<ApplicationStatusBadge :status="application.status" />
        </p>
        <p>申請期間：{{ formatBusinessDate(application.startDate) }} ～ {{ formatBusinessDate(application.endDate) }}</p>
        <p>受付日時：{{ formatDateTime(application.submittedAt) }}</p>
      </v-card-text>
      <v-card-actions class="justify-center flex-wrap">
        <v-btn color="primary" :to="{ name: 'home' }">ホームへ</v-btn>
        <v-btn variant="outlined" :to="{ name: 'applications' }">申請一覧へ</v-btn>
      </v-card-actions>
    </v-card>

    <v-btn v-if="errorMessage" variant="outlined" @click="loadApplication">再読み込み</v-btn>
  </div>
</template>
