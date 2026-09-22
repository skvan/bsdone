import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus, { ElDialog, ElMessageBox } from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';
import { i18n } from './i18n';
import { useSettingsStore } from './stores/settings';
import { startVersionCheck } from './composables/useVersionCheck';
import './styles/index.css';

// 装配顺序对齐编译产物入口：pinia → 弹窗默认值补丁 → i18n → Element Plus → router → mount → 版本检查
// 说明：全局注册 Element Plus 与旧版编译产物行为对齐（旧入口 chunk 全局注册）。
// 路由 99 条已全量注册；页面/布局按批次以 meta.plannedComponent 逐个替换。
const app = createApp(App);

app.use(createPinia());

// 弹窗点击遮罩关闭默认值取自界面设置（编译产物对两个 EP 弹窗组件打补丁）
try {
  const settings = useSettingsStore();
  ElMessageBox.props.closeOnClickModal.default = () => settings.modalCloseOnClickMask;
  ElDialog.props.closeOnClickModal.default = () => settings.modalCloseOnClickMask;
} catch {
  // ignore
}

app.use(i18n);
app.use(ElementPlus);
app.use(router);

// 浏览器历史滚动恢复交由路由 scrollBehavior 处理（与编译产物一致）
if (typeof window !== 'undefined' && 'scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual';
}

app.mount('#app');

// 版本更新检查（立即检查 + 5 分钟轮询 + 可见性触发）
startVersionCheck();
