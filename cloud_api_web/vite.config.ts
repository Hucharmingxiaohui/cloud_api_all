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
// import { viteVConsole } from "vite-plugin-vconsole";
import cesium from 'vite-plugin-cesium'

// https://vitejs.dev/config/
export default ({ command, mode }: ConfigEnv): UserConfigExport =>
  defineConfig({
    plugins: [
      {
        name: 'solar-3d-preview-dev-api',
        configureServer (server) {
          let latestPreview = null
          let latestPreviewVersion = 0

          server.middlewares.use('/dev-api/solar3d-edited', (req, res, next) => {
            if (req.method === 'OPTIONS') {
              res.statusCode = 204
              res.end()
              return
            }

            if (req.method !== 'POST') {
              next()
              return
            }

            let body = ''
            req.on('data', chunk => {
              body += chunk
            })
            req.on('end', async () => {
              const requestId = req.headers['x-solar-edited-request-id'] || `solar-edited-${Date.now()}`
              console.log(`[SolarEdited][${requestId}] forward start, bytes=${body.length}`)
              const controller = new AbortController()
              const timer = setTimeout(() => controller.abort(), 10000)

              try {
                const upstream = await fetch('http://172.20.63.157:5001/solar/edited', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body,
                  signal: controller.signal
                })
                const text = await upstream.text()
                console.log(`[SolarEdited][${requestId}] forward done, status=${upstream.status}, bytes=${text.length}`)
                res.statusCode = upstream.status
                res.setHeader('Content-Type', upstream.headers.get('content-type') || 'application/json; charset=utf-8')
                res.end(text)
              } catch (error) {
                console.error(`[SolarEdited][${requestId}] forward failed:`, error)
                res.statusCode = 504
                res.setHeader('Content-Type', 'application/json; charset=utf-8')
                res.end(JSON.stringify({
                  code: 504,
                  message: error?.name === 'AbortError' ? '航线服务回传超时' : '航线服务回传失败',
                  detail: error?.message || String(error)
                }))
              } finally {
                clearTimeout(timer)
              }
            })
          })

          server.middlewares.use('/dev-api/solar3d-preview', (req, res, next) => {
            if (req.method === 'OPTIONS') {
              res.statusCode = 204
              res.end()
              return
            }

            if (req.method === 'GET') {
              res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate')
              res.setHeader('Pragma', 'no-cache')
              res.setHeader('Expires', '0')
              res.setHeader('Content-Type', 'application/json; charset=utf-8')
              res.end(JSON.stringify({ code: 0, data: latestPreview, version: latestPreviewVersion }))
              return
            }

            if (req.method === 'POST') {
              let body = ''
              req.on('data', chunk => { body += chunk })
              req.on('end', () => {
                try {
                  latestPreview = JSON.parse(body || '{}')
                  latestPreviewVersion = Date.now()
                  server.ws.send('solar3d-preview-updated', {
                    payload: latestPreview,
                    version: latestPreviewVersion
                  })
                  res.setHeader('Content-Type', 'application/json; charset=utf-8')
                  res.end(JSON.stringify({ code: 0, message: 'preview received' }))
                } catch (error) {
                  res.statusCode = 400
                  res.setHeader('Content-Type', 'application/json; charset=utf-8')
                  res.end(JSON.stringify({ code: 400, message: 'invalid json' }))
                }
              })
              return
            }

            next()
          })
        }
      },
      vue(),
      cesium({
        injectCss: false, // 关闭自动注入 CSS
      }),
      eslintPlugin({
        fix: true,
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
      // viteVConsole({
      //   entry: path.resolve(__dirname, "./src/main.ts"), // 入口文件
      //   localEnabled: command === "serve", // serve开发环境下
      //   // enabled: command !== 'serve' || mode === 'test', // 打包环境下/发布测试包,
      //   config: {
      //     // vconsole 配置项
      //     maxLogNumber: 1000,
      //     theme: "light",
      //   },
      // }),
      PkgConfig(),
      OptimizationPersist(),
      // [svgBuilder('./src/assets/icons/')] // All svg under src/icons/svg/ have been imported here, no need to import separately
    ],
    server: {
      port: 8089,
      host: '0.0.0.0',
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
        '/solar-route': {
          target: 'http://172.20.63.157:5001',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/solar-route/, ''),
        },
        '/pathtest1': {
          target: 'https://shona-unsyntactical-quotidianly.ngrok-free.dev',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => {
            const newPath = path.replace(/^\/pathtest1/, '')
            console.log('[Proxy] 原始路径:', path, '-> 代理路径:', newPath)
            return newPath
          },
          configure: (proxy, options) => {
            console.log('[Proxy] 代理配置已加载:', options.target) // 确认代理初始化
            proxy.on('proxyReq', (proxyReq, req) => {
              console.log(
                '[Proxy] 代理目标:',
                `${proxyReq.protocol}//${proxyReq.host}${proxyReq.path}`
              )
              console.log('[Proxy] 请求头:', proxyReq.getHeaders())
            })
            proxy.on('error', (err) => {
              console.error('[Proxy] 错误:', err)
            })
          },
        },
        // '/mapi': {
        //   target: 'http://172.20.63.157:9000',
        //   changeOrigin: true,
        //   rewrite: (path) => path.replace(/^\/mapi/, ''),
        // },
        '/api': {
          target: 'http://192.168.160.1:19922', // 后端接口地址
          changeOrigin: true, // 是否更改请求头中的 Origin
          rewrite: (path) => path.replace(/^\/api/, '') // 去除 /api 前缀
        }
      },
    },
    envDir: './env',
    resolve: {
      alias: [
        {
          // https://github.com/vitejs/vite/issues/279#issuecomment-635646269
          find: '/@',
          replacement: path.resolve(__dirname, './src'),
        },
      ],
    },
    css: {
      preprocessorOptions: {
        scss: {
          // example : additionalData: `@import "./src/design/styles/variables";`
          // dont need include file extend .scss
          silenceDeprecations: ['legacy-js-api'],
          additionalData: `
          @use "./src/styles/variables" as *;
          @use "./src/styles/common" as *;
        `,
        },
      },
    },
    base: '/',
    build: {
      target: ['es2020'], // 最低支持 es2015
      sourcemap: false,
      rollupOptions: {
        output: {
          manualChunks: {
            agora: ['agora-rtc-sdk-ng'],
            flv: ['flv.js'],
            pixelstreaming: [
              '@epicgames-ps/lib-pixelstreamingfrontend-ue5.4',
              '@epicgames-ps/lib-pixelstreamingfrontend-ui-ue5.4'
            ],
            cesium: ['cesium', '@cesium-china/cesium-map'],
            antd: ['ant-design-vue'],
            'element-plus': ['element-plus'],
          }
        }
      }
    },
  })
