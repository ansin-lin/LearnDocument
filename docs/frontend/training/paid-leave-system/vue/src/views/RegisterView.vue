<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppMessage from '../components/common/AppMessage.vue'
import { getDepartments, registerUser } from '../api/auth.js'
import { toApiError } from '../api/http.js'

const router = useRouter()
const form = reactive({
  account: '',
  password: '',
  passwordConfirmation: '',
  name: '',
  department: ''
})

const departments = ref([])
const loadingDepartments = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const fieldErrors = ref({})

function mapFieldErrors(details) {
  return Object.fromEntries(details.map((item) => [item.field, item.message]))
}

async function loadDepartments() {
  loadingDepartments.value = true
  try {
    departments.value = await getDepartments()
  } catch (error) {
    errorMessage.value = toApiError(error).message
  } finally {
    loadingDepartments.value = false
  }
}

async function submit() {
  if (submitting.value) return

  submitting.value = true
  errorMessage.value = ''
  fieldErrors.value = {}
  try {
    const user = await registerUser(form)
    await router.push({
      name: 'login',
      query: { registered: user.employeeNumber }
    })
  } catch (error) {
    const apiError = toApiError(error)
    errorMessage.value = apiError.message
    fieldErrors.value = mapFieldErrors(apiError.details)
  } finally {
    submitting.value = false
  }
}

onMounted(loadDepartments)
</script>

<template>
  <v-main>
    <v-container class="d-flex align-center justify-center py-8">
      <v-card class="auth-card pa-4 pa-sm-7" elevation="3">
        <v-card-title class="text-h5 text-center mb-6">新規利用者登録</v-card-title>

        <AppMessage v-if="errorMessage" type="error" :text="errorMessage" />

        <v-form novalidate @submit.prevent="submit">
          <v-text-field
            v-model="form.account"
            label="アカウント"
            autocomplete="username"
            :error-messages="fieldErrors.account"
          />
          <v-text-field
            v-model="form.password"
            label="パスワード"
            type="password"
            autocomplete="new-password"
            :error-messages="fieldErrors.password"
          />
          <v-text-field
            v-model="form.passwordConfirmation"
            label="パスワード（確認）"
            type="password"
            autocomplete="new-password"
            :error-messages="fieldErrors.passwordConfirmation"
          />
          <v-text-field
            v-model="form.name"
            label="氏名"
            autocomplete="name"
            :error-messages="fieldErrors.name"
          />
          <v-select
            v-model="form.department"
            label="所属部署"
            :items="departments"
            item-title="label"
            item-value="value"
            :loading="loadingDepartments"
            :error-messages="fieldErrors.department"
          />
          <v-btn
            type="submit"
            color="primary"
            size="large"
            block
            :loading="submitting"
            :disabled="submitting || loadingDepartments"
          >
            登録する
          </v-btn>
        </v-form>

        <v-card-actions class="justify-center mt-3">
          <v-btn variant="text" :to="{ name: 'login' }">ログイン画面へ戻る</v-btn>
        </v-card-actions>
      </v-card>
    </v-container>
  </v-main>
</template>
