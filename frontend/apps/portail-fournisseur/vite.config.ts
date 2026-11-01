/// <reference types="vitest" />
import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const interne = (path: string) => fileURLToPath(new URL(`../web-interne/${path}`, import.meta.url))

// Client Keycloak du portail par defaut (shared/config/env.ts lit VITE_KEYCLOAK_CLIENT_ID, dont la
// valeur par defaut est celle de web-interne). Un .env ou l'environnement reste prioritaire.
process.env.VITE_KEYCLOAK_CLIENT_ID ??= 'cse-portail-fournisseur'

/**
 * Portail fournisseur : reutilise le socle de web-interne (design system, client HTTP, auth, i18n)
 * par alias. `@/shared` et `@/locales` pointent vers web-interne, le reste de `@/` vers ce portail.
 */
export default defineConfig({
  plugins: [react()],
  publicDir: interne('public'),
  resolve: {
    alias: [
      { find: /^@\/shared\//, replacement: `${interne('src/shared')}/` },
      { find: /^@\/locales\//, replacement: `${interne('src/locales')}/` },
      { find: /^@\//, replacement: `${fileURLToPath(new URL('./src', import.meta.url))}/` },
    ],
    // Une seule instance des bibliotheques a contexte, quel que soit le dossier importateur.
    dedupe: [
      'react',
      'react-dom',
      'react-router-dom',
      '@tanstack/react-query',
      'react-i18next',
      'i18next',
      'sonner',
      'react-hook-form',
    ],
  },
  server: {
    port: 5174,
    strictPort: true,
    host: true,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  preview: { port: 4174 },
  build: {
    target: 'es2022',
    sourcemap: true,
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          query: ['@tanstack/react-query'],
          radix: [
            '@radix-ui/react-dialog',
            '@radix-ui/react-dropdown-menu',
            '@radix-ui/react-popover',
            '@radix-ui/react-tooltip',
          ],
        },
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    include: ['src/**/*.test.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      include: ['src/**/lib/**', 'src/**/hooks/**', 'src/**/schemas/**', 'src/api/**', 'src/domain/**'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**'],
    },
  },
})
