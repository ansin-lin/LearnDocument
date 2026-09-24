<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppMessage from '../components/common/AppMessage.vue'
import { useAuthStore } from '../stores/auth.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const form = reactive({ account: '', password: '' })
const errorMessage = ref('')

async function submit() {
  errorMessage.value = ''
  try {
    await authStore.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = error.message
  }
}
</script>

<template>
  <v-main>
    <v-container class="d-flex align-center justify-center" style="min-height: 100vh">
      <v-card class="auth-card pa-4 pa-sm-7" elevation="3">
        <v-card-title class="text-h5 text-center mb-2">ログイン</v-card-title>
        <v-card-subtitle class="text-center mb-6">有給休暇申請システム</v-card-subtitle>

        <AppMessage
          v-if="route.query.registered"
          type="success"
          :text="`登録が完了しました。社員番号：${route.query.registered}`"
        />
        <AppMessage v-if="errorMessage" type="error" :text="errorMessage" />

        <v-form novalidate @submit.prevent="submit">
          <v-text-field v-model="form.account" label="アカウント" autocomplete="username" />
          <v-text-field
            v-model="form.password"
            label="パスワード"
            type="password"
            autocomplete="current-password"
          />
          <v-btn
            type="submit"
            color="primary"
            size="large"
            block
            :loading="authStore.loading"
            :disabled="authStore.loading"
          >
            ログイン
          </v-btn>
        </v-form>

        <v-card-actions class="justify-center mt-3">
          <span>アカウントをお持ちでない方</span>
          <v-btn variant="text" :to="{ name: 'register' }">新規登録</v-btn>
        </v-card-actions>
      </v-card>
    </v-container>
  </v-main>
</template>
