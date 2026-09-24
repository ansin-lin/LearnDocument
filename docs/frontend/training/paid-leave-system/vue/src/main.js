import { createApp } from 'vue'
import App from './App.vue'
import { vuetify } from './plugins/vuetify.js'
import { pinia } from './stores/index.js'
import router from './router/index.js'
import './styles/main.css'

createApp(App)
  .use(pinia)
  .use(router)
  .use(vuetify)
  .mount('#app')
