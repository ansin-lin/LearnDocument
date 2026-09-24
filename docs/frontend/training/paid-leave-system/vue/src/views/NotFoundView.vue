<template>
  <v-main>
    <v-container class="text-center py-16">
      <h1 class="text-h4 mb-4">ページが見つかりません</h1>
      <p class="mb-6">URLを確認してください。</p>
      <v-btn color="primary" :to="{ name: 'home' }">ホームへ</v-btn>
    </v-container>
  </v-main>
</template>
