import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Relative base './' ensures the build works out-of-the-box on GitHub Pages
// (both https://<user>.github.io/<repo>/ and custom domains).
export default defineConfig({
  plugins: [react()],
  base: './',
});
