<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import LeaveForm from '../components/applications/LeaveForm.vue'
import AppMessage from '../components/common/AppMessage.vue'
import { useApplicationsStore } from '../stores/applications.js'

const router = useRouter()
const applicationsStore = useApplicationsStore()
const message = computed(() => applicationsStore.submissionMessage)
const fieldErrors = computed(() => applicationsStore.submissionFieldErrors)

function goToConfirm(input) {
  applicationsStore.setDraft(input)
  applicationsStore.clearSubmissionErrors()
  router.push({ name: 'application-confirm' })
}
</script>

<template>
  <div class="content-card">
    <h1 class="page-title">休暇申請</h1>
    <AppMessage v-if="message" type="error" :text="message" />
    <v-card class="pa-4 pa-sm-6">
      <LeaveForm
        :initial-value="applicationsStore.draft ?? {}"
        :field-errors="fieldErrors"
        @submit="goToConfirm"
      />
    </v-card>
  </div>
</template>
