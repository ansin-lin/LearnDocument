import { defineStore } from 'pinia'
import * as authApi from '../api/auth.js'
import { toApiError } from '../api/http.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    initialized: false,
    loading: false
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.user)
  },

  actions: {
    async login(input) {
      this.loading = true
      try {
        this.user = await authApi.login(input)
        return this.user
      } catch (error) {
        throw toApiError(error)
      } finally {
        this.loading = false
      }
    },

    async restoreSession() {
      if (this.initialized) return

      try {
        this.user = await authApi.getCurrentUser()
      } catch {
        this.user = null
      } finally {
        this.initialized = true
      }
    },

    async logout() {
      try {
        await authApi.logout()
      } catch (error) {
        const apiError = toApiError(error)
        if (apiError.status !== 401) throw apiError
      } finally {
        this.user = null
      }
    },

    clearUser() {
      this.user = null
    }
  }
})
