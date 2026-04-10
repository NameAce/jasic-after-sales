import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import vue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'
import globals from 'globals'
import vueParser from 'vue-eslint-parser'
import { readFileSync } from 'fs'

const autoImports = JSON.parse(readFileSync('./.eslintrc-auto-import.json', 'utf-8'))

export default [
  {
    ignores: ['dist', 'node_modules', '**/*.d.ts']
  },

  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...vue.configs['flat/recommended'],

  {
    files: ['**/*.{js,ts,vue}'],

    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tseslint.parser, // ⭐ 再用 ts parser 解析 script
        ecmaVersion: 'latest',
        sourceType: 'module'
      },
      globals: {
        ...globals.browser,
        ...autoImports.globals,

        uni: 'readonly',
        wx: 'readonly',
        console: 'readonly',
        getCurrentPages: 'readonly'
      }
    },

    rules: {
      'vue/multi-word-component-names': 'off',

      'vue/no-multiple-template-root': 'off',

      'vue/max-attributes-per-line': [
        'error',
        {
          singleline: { max: 3 },
          multiline: { max: 1 }
        }
      ],

      '@typescript-eslint/no-explicit-any': 'off',

      '@typescript-eslint/no-unused-vars': [
        'warn',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_'
        }
      ],

      'no-unused-vars': 'off'
    }
  },

  {
    files: ['vite.config.ts'],
    rules: {
      '@typescript-eslint/no-unused-expressions': 'off'
    }
  },

  prettier
]
