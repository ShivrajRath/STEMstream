import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { createHash } from 'node:crypto'
import { readFileSync } from 'node:fs'

const CATALOG_FILE = new URL('./src/data/data.json', import.meta.url)

// Minified catalogue JSON. Parsing and re-serialising also validates the file
// during the build, so a broken catalogue fails CI instead of shipping.
function readCatalog() {
  return JSON.stringify(JSON.parse(readFileSync(CATALOG_FILE, 'utf8')))
}

/**
 * Publishes the catalogue next to the built site as `catalog.json`.
 *
 * The Android TV / Fire TV app downloads that file on launch, so a daily
 * curation commit reaches televisions without rebuilding or reinstalling the
 * APK. Keeping it a build artifact rather than a second committed copy means
 * `src/data/data.json` stays the only source of truth.
 */
function catalogJson() {
  return {
    name: 'stemstream-catalog-json',
    configureServer(server) {
      server.middlewares.use('/catalog.json', (_request, response, next) => {
        try {
          const body = readCatalog()
          const etag = `"${createHash('sha1').update(body).digest('base64')}"`

          if (_request.headers['if-none-match'] === etag) {
            response.statusCode = 304
            response.end()
            return
          }

          response.setHeader('Content-Type', 'application/json; charset=utf-8')
          response.setHeader('Cache-Control', 'no-cache')
          response.setHeader('ETag', etag)
          response.end(body)
        } catch (error) {
          next(error)
        }
      })
    },
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: 'catalog.json', source: readCatalog() })
    },
  }
}

export default defineConfig({
  plugins: [react(), catalogJson()],
  // Use relative base path for GitHub Pages compatibility
  base: './',
})
