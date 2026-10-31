// Calcule les empreintes CSP (SHA-256, Base64) des scripts en ligne des pages HTML produites par Vite
// (thème initial de index.html, silent-check-sso.html de keycloak-js) et les injecte dans le modèle
// Nginx à la place de __CSP_SCRIPT_HASHES__. Évite 'unsafe-inline' tout en restant juste si une page
// change : l'empreinte est recalculée à chaque construction de l'image.
// Usage : node csp-hashes.mjs <répertoire dist> <modèle nginx>
import { createHash } from 'node:crypto'
import { readFileSync, readdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const [dist, modele] = process.argv.slice(2)
if (!dist || !modele) {
  console.error('Usage : node csp-hashes.mjs <dist> <modele>')
  process.exit(2)
}

const SCRIPT_EN_LIGNE = /<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/gi
const empreintes = new Set()
for (const fichier of readdirSync(dist).filter((f) => f.endsWith('.html'))) {
  const html = readFileSync(join(dist, fichier), 'utf8')
  for (const [, contenu] of html.matchAll(SCRIPT_EN_LIGNE)) {
    if (contenu.trim() === '') continue
    empreintes.add(`'sha256-${createHash('sha256').update(contenu, 'utf8').digest('base64')}'`)
    console.log(`CSP : script en ligne de ${fichier} autorisé par empreinte`)
  }
}

const texte = readFileSync(modele, 'utf8')
if (!texte.includes('__CSP_SCRIPT_HASHES__')) {
  console.error('Marqueur __CSP_SCRIPT_HASHES__ absent du modèle Nginx')
  process.exit(1)
}
writeFileSync(modele, texte.replaceAll('__CSP_SCRIPT_HASHES__', [...empreintes].join(' ')))
