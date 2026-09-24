<script setup>
import { onMounted, ref } from 'vue'
import AppMessage from '../components/common/AppMessage.vue'
import { getDashboard } from '../api/applications.js'
import { toApiError } from '../api/http.js'

const dashboard = ref(null)
const loading = ref(false)
const errorMessage = ref('')

async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    dashboard.value = await getDashboard()
  } catch (error) {
    errorMessage.value = toApiError(error).message
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="content-card">
    <h1 class="page-title">ホーム</h1>
    <AppMessage v-if="errorMessage" type="error" :text="errorMessage" />

    <div v-if="loading" class="text-center py-12">
      <v-progress-circular indeterminate color="primary" />
      <p class="mt-4">データを読み込んでいます</p>
    </div>

    <template v-else-if="dashboard">
      <v-card class="mb-5">
        <v-card-title>社員情報</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="12" sm="4"><strong>社員番号</strong><br>{{ dashboard.employeeNumber }}</v-col>
            <v-col cols="12" sm="4"><strong>氏名</strong><br>{{ dashboard.name }}</v-col>
            <v-col cols="12" sm="4"><strong>所属部署</strong><br>{{ dashboard.departmentName }}</v-col>
          </v-row>
        </v-card-text>
      </v-card>

      <v-row>
        <v-col cols="12" md="4">
          <v-card class="pa-4 text-center" height="100%">
            <div>有給残日数</div>
            <div class="summary-value">{{ dashboard.remainingPaidLeaveDays }}</div>
            <div>日</div>
          </v-card>
        </v-col>
        <v-col cols="12" md="4">
          <v-card class="pa-4 text-center" height="100%">
            <div>申請中</div>
            <div class="summary-value">{{ dashboard.pendingCount }}</div>
            <div>件</div>
          </v-card>
        </v-col>
        <v-col cols="12" md="4">
          <v-card class="pa-4 text-center" height="100%">
            <div>当月承認済</div>
            <div class="summary-value">{{ dashboard.approvedDaysThisMonth }}</div>
            <div>日</div>
          </v-card>
        </v-col>
      </v-row>

      <div class="d-flex flex-column flex-sm-row ga-3 mt-6">
        <v-btn color="primary" size="large" :to="{ name: 'application-new' }">休暇を申請する</v-btn>
        <v-btn variant="outlined" size="large" :to="{ name: 'applications' }">申請一覧を見る</v-btn>
      </div>
    </template>

    <v-btn v-if="errorMessage" variant="outlined" @click="loadDashboard">再読み込み</v-btn>
  </div>
</template>
