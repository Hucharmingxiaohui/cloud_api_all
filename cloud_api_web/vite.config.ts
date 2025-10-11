import vue from '@vitejs/plugin-vue'
// config alias
import path from 'path'
import { ConfigEnv, defineConfig, UserConfigExport } from 'vite'
import ViteComponents, { AntDesignVueResolver } from 'vite-plugin-components'
// Introduce eslint plugin
import eslintPlugin from 'vite-plugin-eslint'
import OptimizationPersist from 'vite-plugin-optimize-persist'
import PkgConfig from 'vite-plugin-package-config'
import viteSvgIcons from 'vite-plugin-svg-icons'
import { viteVConsole } from 'vite-plugin-vconsole'
import cesium from 'vite-plugin-cesium'

// https://vitejs.dev/config/
export default ({ command, mode }: ConfigEnv): UserConfigExport => defineConfig({
  plugins: [
    vue(),
    cesium(), // 添加 Cesium 插件
    eslintPlugin({
      fix: true
    }),
    ViteComponents({
      customComponentResolvers: [AntDesignVueResolver()],
    }),
    viteSvgIcons({
      // 指定需要缓存的图标文件夹
      iconDirs: [path.resolve(process.cwd(), 'src/assets/icons')],
      // 指定symbolId格式
      symbolId: 'icon-[dir]-[name]',
    }),
    viteVConsole({
      entry: path.resolve(__dirname, './src/main.ts'), // 入口文件
      localEnabled: command === 'serve', // serve开发环境下
      // enabled: command !== 'serve' || mode === 'test', // 打包环境下/发布测试包,
      config: { // vconsole 配置项
        maxLogNumber: 1000,
        theme: 'light'
      }
    }),
    PkgConfig(),
    OptimizationPersist(),
    // [svgBuilder('./src/assets/icons/')] // All svg under src/icons/svg/ have been imported here, no need to import separately
  ],
  server: {
    open: false,
    host: '0.0.0.0',
    port: 8080,
    proxy: {
      // 代理配置
      // '/api': {
      //   target: 'http://127.0.0.1:18082',
      //   changeOrigin: true,
      //   rewrite: (path) => path.replace(/^\/api/, ''),
      // },
      '/pathplanning': {
        target: 'http://172.20.63.238:9527',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/path/, ''),
      },
      '/mapi': {
        target: 'http://172.20.63.157:9000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/mapi/, ''), // 修改此处
      },
      '/api': {//2025/8/1 xtj
        target: 'http://172.20.63.88:6789',  // 后端接口地址
        changeOrigin: true,  // 是否更改请求头中的 Origin
        rewrite: (path) => path.replace(/^\/api/, '')  // 去除 /api 前缀
      }
    },
  },
  envDir: './env',
  resolve: {
    alias: [{
      // https://github.com/vitejs/vite/issues/279#issuecomment-635646269
      find: '/@',
      replacement: path.resolve(__dirname, './src'),
    }
    ]
  },
  css: {
    preprocessorOptions: {
      scss: {
        // example : additionalData: `@import "./src/design/styles/variables";`
        // dont need include file extend .scss
        additionalData: '@import "./src/styles/variables";'
      },
    }
  },
  base: './',
  build: {
    target: ['es2020'], // 最低支持 es2015
    sourcemap: false
  }
})
