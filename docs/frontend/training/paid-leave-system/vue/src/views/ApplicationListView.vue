<script setup>
import { onMounted, reactive, ref } from 'vue'
import AppMessage from '../components/common/AppMessage.vue'
import ApplicationStatusBadge from '../components/applications/ApplicationStatusBadge.vue'
import { applicationStatuses, leaveTypeLabels } from '../constants/masterData.js'
import { useApplicationsStore } from '../stores/applications.js'
import { formatBusinessDate, formatDateTime } from '../utils/date.js'

const applicationsStore = useApplicationsStore()
const filters = reactive({ status: '', keyword: '' })
const errorMessage = ref('')
const cancelTarget = ref(null)
const cancelling = ref(false)

async function search() {
  errorMessage.value = ''
  try {
    await applicationsStore.fetchApplications({
      ...(filters.status ? { status: filters.status } : {}),
      ...(filters.keyword.trim() ? { keyword: filters.keyword.trim() } : {})
    })
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function clearFilters() {
  filters.status = ''
  filters.keyword = ''
  await search()
}

async function confirmCancel() {
  if (!cancelTarget.value || cancelling.value) return

  cancelling.value = true
  errorMessage.value = ''
  try {
    await applicationsStore.cancel(cancelTarget.value.id)
    cancelTarget.value = null
    await search()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    cancelling.value = false
  }
}

onMounted(search)
</script>

<template>
  <div class="content-card">
    <div class="d-flex flex-column flex-sm-row justify-space-between align-sm-center mb-4 ga-3">
      <h1 class="page-title mb-0">申請一覧</h1>
      <v-btn color="primary" :to="{ name: 'application-new' }">新しい申請</v-btn>
    </div>

    <AppMessage v-if="errorMessage" type="error" :text="errorMessage" />

    <v-card class="pa-4 mb-5">
      <v-form @submit.prevent="search">
        <v-row align="center">
          <v-col cols="12" md="4">
            <v-select v-model="filters.status" label="申請状態" :items="applicationStatuses" hide-details />
          </v-col>
          <v-col cols="12" md="5">
            <v-text-field v-model="filters.keyword" label="受付番号・申請理由" hide-details clearable />
          </v-col>
          <v-col cols="12" md="3" class="d-flex ga-2">
            <v-btn type="submit" color="primary" :loading="applicationsStore.loading">検索</v-btn>
            <v-btn variant="outlined" :disabled="applicationsStore.loading" @click="clearFilters">クリア</v-btn>
          </v-col>
        </v-row>
      </v-form>
    </v-card>

    <div v-if="applicationsStore.loading" class="text-center py-12">
      <v-progress-circular indeterminate color="primary" />
      <p class="mt-4">申請を読み込んでいます</p>
    </div>

    <v-alert
      v-else-if="applicationsStore.items.length === 0 && !errorMessage"
      type="info"
      variant="tonal"
    >
      該当する申請はありません。
    </v-alert>

    <v-card v-else-if="applicationsStore.items.length > 0">
      <v-card-subtitle class="pt-4">検索結果：{{ applicationsStore.items.length }}件</v-card-subtitle>
      <div class="overflow-x-auto">
        <v-table>
          <thead>
            <tr>
              <th scope="col">受付番号</th>
              <th scope="col">休暇種別</th>
              <th scope="col">期間</th>
              <th scope="col">日数</th>
              <th scope="col">状態</th>
              <th scope="col">申請日時</th>
              <th scope="col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in applicationsStore.items" :key="item.id">
              <td>{{ item.receiptNumber }}</td>
              <td>{{ leaveTypeLabels[item.leaveType] ?? item.leaveType }}</td>
              <td>{{ formatBusinessDate(item.startDate) }}～{{ formatBusinessDate(item.endDate) }}</td>
              <td>{{ item.leaveDays }}日</td>
              <td><ApplicationStatusBadge :status="item.status" /></td>
              <td>{{ formatDateTime(item.submittedAt) }}</td>
              <td>
                <v-btn
                  v-if="item.status === 'pending'"
                  color="error"
                  variant="outlined"
                  size="small"
                  @click="cancelTarget = item"
                >
                  取消
                </v-btn>
                <span v-else>-</span>
              </td>
            </tr>
          </tbody>
        </v-table>
      </div>
    </v-card>

    <v-dialog :model-value="Boolean(cancelTarget)" max-width="480" @update:model-value="cancelTarget = null">
      <v-card>
        <v-card-title>申請を取り消しますか</v-card-title>
        <v-card-text>
          受付番号「{{ cancelTarget?.receiptNumber }}」を取消済に更新します。
        </v-card-text>
        <v-card-actions class="justify-end">
          <v-btn :disabled="cancelling" @click="cancelTarget = null">戻る</v-btn>
          <v-btn color="error" :loading="cancelling" :disabled="cancelling" @click="confirmCancel">
            取り消す
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
