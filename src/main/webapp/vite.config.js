import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
    plugins: [react()],
    build: {
        outDir: '../resources/static',
        emptyOutDir: true,
    },
    base: '/app',
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
            '@assets': `${path.resolve(__dirname, './src/assets')}`,
            '@components': `${path.resolve(__dirname, './src/components')}`,
            '@config': `${path.resolve(__dirname, './src/config')}`,
            '@contexts': `${path.resolve(__dirname, './src/contexts')}`,
            '@hooks': `${path.resolve(__dirname, './src/hooks')}`,
            '@i18n': `${path.resolve(__dirname, './src/i18n')}`,
            '@img': `${path.resolve(__dirname, './src/img')}`,
            '@interfaces': `${path.resolve(__dirname, './src/interfaces')}`,
            '@models': `${path.resolve(__dirname, './src/models')}`,
            '@pages': `${path.resolve(__dirname, './src/pages')}`,
            '@routes': `${path.resolve(__dirname, './src/routes')}`,
            '@services': `${path.resolve(__dirname, './src/services')}`,
            '@themes': `${path.resolve(__dirname, './src/themes')}`,
            '@utils': `${path.resolve(__dirname, './src/utils')}`,
        },
    },
});
