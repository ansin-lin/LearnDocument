import { defineStore } from 'pinia'
import * as applicationsApi from '../api/applications.js'
import { toApiError } from '../api/http.js'

export const useApplicationsStore = defineStore('applications', {
  state: () => ({
    draft: null,
    items: [],
    loading: false,
    submitting: false,
    submissionMessage: '',
    submissionFieldErrors: {}
  }),

  actions: {
    setDraft(input) {
      this.draft = { ...input }
    },

    clearDraft() {
      this.draft = null
    },

    clearSubmissionErrors() {
      this.submissionMessage = ''
      this.submissionFieldErrors = {}
    },

    setSubmissionErrors(error) {
      this.submissionMessage = error.message
      this.submissionFieldErrors = Object.fromEntries(
        error.details.map((item) => [item.field, item.message])
      )
    },

    async fetchApplications(params = {}) {
      this.loading = true
      try {
        const result = await applicationsApi.getApplications(params)
        this.items = result.data
        return result
      } catch (error) {
        throw toApiError(error)
      } finally {
        this.loading = false
      }
    },

    async submitDraft() {
      if (this.submitting || !this.draft) return null

      this.submitting = true
      try {
        const created = await applicationsApi.createApplication(this.draft)
        this.clearDraft()
        this.clearSubmissionErrors()
        return created
      } catch (error) {
        throw toApiError(error)
      } finally {
        this.submitting = false
      }
    },

    async cancel(id) {
      try {
        const updated = await applicationsApi.cancelApplication(id)
        const index = this.items.findIndex((item) => item.id === updated.id)
        if (index >= 0) this.items[index] = updated
        return updated
      } catch (error) {
        throw toApiError(error)
      }
    }
  }
})
