// PlatformAsset —— 平台资产页（超管）；批次 4b Task 4b-4 / #154
// 数据源：GET /sys/platform-asset/summary（src/api/system.js: platformAssetApi.summary）
// 语义：各实体 platform_owned=true 且已软删的计数（租户删除=归还平台）→ league/team/player/event/game 五类
// 约定：加载态 vLoading、错误提示 ElMessage.error（对齐 admin 页约定）；渲染函数写成，避免新增 scoped CSS
// 可见性：菜单经后端菜单树下发（seedPlatformAssetMenu，仅超管绑定）；接口层 PlatformAssetService 有超管守卫
import { defineComponent, ref, computed, onMounted, h, withDirectives } from 'vue';
import { ElCard, ElButton, ElTable, ElTableColumn, ElMessage, vLoading } from 'element-plus';
import { platformAssetApi } from '../../api/system';

// 五类资产：字段 key 对齐后端 summary 返回（league/team/player/event/game）
const ASSET_ROWS = [
  { key: 'league', label: '联盟' },
  { key: 'team', label: '球队' },
  { key: 'player', label: '球员' },
  { key: 'event', label: '赛事' },
  { key: 'game', label: '比赛' }
];

const PAGE_DESC = '平台回收资产（租户删除=归还）汇总';

function toCount(value) {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

export default defineComponent({
  name: 'PlatformAsset',
  setup() {
    const loading = ref(false);
    const summary = ref({});

    const rows = computed(() =>
      ASSET_ROWS.map((r) => ({ key: r.key, label: r.label, count: toCount(summary.value?.[r.key]) }))
    );
    const total = computed(() => rows.value.reduce((sum, r) => sum + r.count, 0));

    async function load() {
      loading.value = true;
      try {
        summary.value = await platformAssetApi.summary();
      } catch (e) {
        ElMessage.error((e && e.message) || '加载平台资产汇总失败');
      } finally {
        loading.value = false;
      }
    }

    onMounted(load);

    return () =>
      h('div', { class: 'admin-page' }, [
        h(ElCard, null, {
          header: () => [
            h('span', null, '平台资产'),
            h(
              ElButton,
              { type: 'primary', size: 'small', style: { float: 'right' }, onClick: load },
              { default: () => '刷新' }
            )
          ],
          default: () => [
            h(
              'p',
              { style: { color: 'var(--el-text-color-secondary)', margin: '0 0 16px', fontSize: '13px' } },
              PAGE_DESC
            ),
            withDirectives(
              h(
                ElTable,
                { data: rows.value, border: true, emptyText: '暂无平台回收资产', style: { width: '100%' } },
                {
                  default: () => [
                    h(ElTableColumn, { prop: 'label', label: '资产类型', minWidth: '140' }),
                    h(ElTableColumn, { prop: 'count', label: '回收数量', minWidth: '120', align: 'right' })
                  ]
                }
              ),
              [[vLoading, loading.value]]
            ),
            h(
              'p',
              { style: { color: 'var(--el-text-color-secondary)', margin: '12px 0 0', fontSize: '13px' } },
              `合计：${total.value}`
            )
          ]
        })
      ]);
  }
});
