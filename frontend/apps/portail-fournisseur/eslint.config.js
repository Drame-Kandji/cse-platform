import { cseEslintConfig } from '../web-interne/eslint.config.js'

/** Meme configuration ESLint que la plateforme interne (strictTypeChecked, a11y, hooks). */
export default cseEslintConfig(import.meta.dirname)
