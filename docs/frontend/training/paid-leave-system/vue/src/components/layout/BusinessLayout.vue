<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useDisplay } from 'vuetify'
import { useAuthStore } from '../../stores/auth.js'
import { useApplicationsStore } from '../../stores/applications.js'

const router = useRouter()
const authStore = useAuthStore()
const applicationsStore = useApplicationsStore()
const drawer = ref(false)
const logoutError = ref('')
const { mdAndUp } = useDisplay()

async function handleLogout() {
  logoutError.value = ''
  try {
    await authStore.logout()
    applicationsStore.$reset()
    await router.replace({ name: 'login' })
  } catch (error) {
    logoutError.value = error.message
  }
}

</script>

<template>
  <v-app-bar color="primary">
    <v-app-bar-nav-icon
      v-if="!mdAndUp"
      aria-label="メニューを開く"
      @click="drawer = !drawer"
    />
    <v-app-bar-title>有給休暇申請システム</v-app-bar-title>
    <template v-if="mdAndUp">
      <v-btn variant="text" :to="{ name: 'home' }">ホーム</v-btn>
      <v-btn variant="text" :to="{ name: 'application-new' }">休暇申請</v-btn>
      <v-btn variant="text" :to="{ name: 'applications' }">申請一覧</v-btn>
    </template>
    <v-spacer />
    <span v-if="mdAndUp" class="mr-4">{{ authStore.user?.name }}</span>
    <v-btn variant="text" @click="handleLogout">ログアウト</v-btn>
  </v-app-bar>

  <v-navigation-drawer v-if="!mdAndUp" v-model="drawer" temporary>
    <v-list nav>
      <v-list-item title="ホーム" :to="{ name: 'home' }" />
      <v-list-item title="休暇申請" :to="{ name: 'application-new' }" />
      <v-list-item title="申請一覧" :to="{ name: 'applications' }" />
    </v-list>
  </v-navigation-drawer>

  <v-main>
    <v-container class="py-6">
      <v-alert v-if="logoutError" type="error" variant="tonal" class="mb-4">
        {{ logoutError }}
      </v-alert>
      <RouterView />
    </v-container>
  </v-main>
</template>
