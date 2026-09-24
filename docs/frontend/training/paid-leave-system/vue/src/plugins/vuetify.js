import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { aliases, mdi } from 'vuetify/iconsets/mdi-svg'

export const vuetify = createVuetify({
  components,
  directives,
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: { mdi }
  },
  theme: {
    defaultTheme: 'paidLeaveLight',
    themes: {
      paidLeaveLight: {
        dark: false,
        colors: {
          primary: '#315b7d',
          secondary: '#54748d',
          background: '#f4f7f9',
          surface: '#ffffff',
          error: '#b3261e',
          success: '#276749'
        }
      }
    }
  }
})
