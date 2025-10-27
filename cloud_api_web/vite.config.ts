import vue from "@vitejs/plugin-vue";
// config alias
import path from "path";
import { ConfigEnv, defineConfig, UserConfigExport } from "vite";
import ViteComponents, { AntDesignVueResolver } from "vite-plugin-components";
// Introduce eslint plugin
import eslintPlugin from "vite-plugin-eslint";
import OptimizationPersist from "vite-plugin-optimize-persist";
import PkgConfig from "vite-plugin-package-config";
import viteSvgIcons from "vite-plugin-svg-icons";
import { viteVConsole } from "vite-plugin-vconsole";
import cesium from "vite-plugin-cesium";

// https://vitejs.dev/config/
export default ({ command, mode }: ConfigEnv): UserConfigExport =>
  defineConfig({
    plugins: [
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
        iconDirs: [path.resolve(process.cwd(), "src/assets/icons")],
        // 指定symbolId格式
        symbolId: "icon-[dir]-[name]",
      }),
      viteVConsole({
        entry: path.resolve(__dirname, "./src/main.ts"), // 入口文件
        localEnabled: command === "serve", // serve开发环境下
        // enabled: command !== 'serve' || mode === 'test', // 打包环境下/发布测试包,
        config: {
          // vconsole 配置项
          maxLogNumber: 1000,
          theme: "light",
        },
      }),
      PkgConfig(),
      OptimizationPersist(),
      // [svgBuilder('./src/assets/icons/')] // All svg under src/icons/svg/ have been imported here, no need to import separately
    ],
    server: {
      port: 8089,
      host: "0.0.0.0",  
      proxy: {
        // 代理配置
        // '/api': {
        //   target: 'http://127.0.0.1:18082',
        //   changeOrigin: true,
        //   rewrite: (path) => path.replace(/^\/api/, ''),
        // },
        "/pathplanning": {
          target: "http://172.20.63.238:9527",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/path/, ""),
        },
        "/pathtest1": {
          target: "https://shona-unsyntactical-quotidianly.ngrok-free.dev",
          changeOrigin: true,
          secure: false,
          rewrite: (path) => {
            const newPath = path.replace(/^\/pathtest1/, "");
            console.log("[Proxy] 原始路径:", path, "-> 代理路径:", newPath);
            return newPath;
          },
          configure: (proxy, options) => {
            console.log("[Proxy] 代理配置已加载:", options.target); // 确认代理初始化
            proxy.on("proxyReq", (proxyReq, req) => {
              console.log(
                "[Proxy] 代理目标:",
                `${proxyReq.protocol}//${proxyReq.host}${proxyReq.path}`
              );
              console.log("[Proxy] 请求头:", proxyReq.getHeaders());
            });
            proxy.on("error", (err) => {
              console.error("[Proxy] 错误:", err);
            });
          },
        },
        // '/mapi': {
        //   target: 'http://172.20.63.157:9000',
        //   changeOrigin: true,
        //   rewrite: (path) => path.replace(/^\/mapi/, ''),
        // },
        // '/api': {
        //   target: 'http://172.20.63.88:6789',  // 后端接口地址
        //   changeOrigin: true,  // 是否更改请求头中的 Origin
        //   rewrite: (path) => path.replace(/^\/api/, '')  // 去除 /api 前缀
        // }
      },
    },
    envDir: "./env",
    resolve: {
      alias: [
        {
          // https://github.com/vitejs/vite/issues/279#issuecomment-635646269
          find: "/@",
          replacement: path.resolve(__dirname, "./src"),
        },
      ],
    },
    css: {
      preprocessorOptions: {
        scss: {
          // example : additionalData: `@import "./src/design/styles/variables";`
          // dont need include file extend .scss
          silenceDeprecations: ["legacy-js-api"],
          additionalData: `
          @use "./src/styles/variables" as *;
          @use "./src/styles/common" as *;
        `,
        },
      },
    },
    base: "/",
    build: {
      target: ["es2020"], // 最低支持 es2015
      sourcemap: false,
    },
  });
