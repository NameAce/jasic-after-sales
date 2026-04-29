import { defineConfig } from '@soybeanjs/eslint-config';

export default defineConfig(
  { vue: true, unocss: true },
  {
    rules: {
      'vue/multi-word-component-names': [
        'warn',
        {
          ignores: ['index', 'App', 'Register', '[id]', '[url]']
        }
      ],
      'vue/component-name-in-template-casing': [
        'warn',
        'PascalCase',
        {
          registeredComponentsOnly: false,
          ignores: ['/^icon-/']
        }
      ],
      'unocss/order-attributify': 'off'
    }
  },
  // 大块业务页：先放宽规则以便通过 pre-commit；后续可逐步拆函数、收紧规则
  {
    files: ['src/views/advanced-modules/**/*.vue', 'src/views/org/**/*.vue'],
    rules: {
      complexity: 'off',
      'no-nested-ternary': 'off',
      'no-eq-null': 'off',
      eqeqeq: 'off',
      '@typescript-eslint/no-unused-vars': 'off',
      '@typescript-eslint/no-dynamic-delete': 'off',
      'logical-assignment-operators': 'off',
      'consistent-return': 'off',
      '@typescript-eslint/no-shadow': 'off',
      'no-template-curly-in-string': 'off',
      'vue/no-use-v-else-with-v-for': 'off'
    }
  }
);
