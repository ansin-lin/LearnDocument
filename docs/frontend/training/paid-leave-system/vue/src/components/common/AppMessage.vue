<script setup>
defineProps({
  type: {
    type: String,
    default: 'info'
  },
  text: {
    type: String,
    required: true
  }
})
</script>

<template>
  <v-alert :type="type" variant="tonal" class="mb-4" role="status">
    {{ text }}
  </v-alert>
</template>
