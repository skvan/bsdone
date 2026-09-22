import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';
import { installI18n } from './i18n';
import { useAppConfigStore } from './stores/appConfig';
import './styles/index.css';

// 说明：全局注册 Element Plus 与旧版编译产物行为对齐（旧入口 chunk 全局注册）。
// 路由骨架（B1）已按契约注册全部 99 条；页面/布局/守卫在后续批次按 meta.plannedComponent 替换。
const app = createApp(App);

app.use(createPinia());
app.use(ElementPlus);
app.use(router);
installI18n(app);

app.mount('#app');

// 站点配置预加载（失败静默，保留默认值）
try {
  useAppConfigStore().init();
} catch {
  // ignore
}
