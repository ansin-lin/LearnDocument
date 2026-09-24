<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    required: true
  }
})

const settings = {
  pending: { text: '申請中', color: 'warning' },
  approved: { text: '承認済', color: 'success' },
  returned: { text: '差戻し', color: 'error' },
  cancelled: { text: '取消済', color: 'grey' }
}

const current = computed(() => settings[props.status] ?? { text: props.status, color: 'grey' })
</script>

<template>
  <v-chip :color="current.color" size="small" label>
    {{ current.text }}
  </v-chip>
</template>
