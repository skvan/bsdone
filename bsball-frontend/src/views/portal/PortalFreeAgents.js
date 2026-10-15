// PortalFreeAgents —— 门户「自由球员」列表：本租户无现属球队球员（Issue #329，口径与后台一致）
import { defineComponent, h, onMounted, ref } from 'vue';
import { ElEmpty, ElPagination } from 'element-plus';
import { RouterLink } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { playerApi } from '../../api/business';
import { resolveAssetUrl } from '../../api/request';
import { useTenantRouter } from '../../composables/useTenantRouter';
import { DEFAULT_AVATAR_SVG_PATH, onAvatarError } from '../../utils/placeholderAssets';
import { POSITION_LABELS } from '../../utils/playerOptions';
import '../../styles/legacy/portal-free-agents.css';

export default defineComponent({
  name: 'PortalFreeAgents',
  setup() {
    const { t, te, locale } = useI18n();
    const { portalPath } = useTenantRouter();

    const loading = ref(false);
    const rows = ref([]);
    const page = ref(1);
    const pageSize = ref(24);
    const total = ref(0);

    async function load() {
      loading.value = true;
      try {
        const { list, total: count } = await playerApi.list({ teamId: 0, page: page.value, pageSize: pageSize.value });
        rows.value = list ?? [];
        total.value = count ?? 0;
      } finally {
        loading.value = false;
      }
    }

    onMounted(load);

    function positionsText(player) {
      const list = Array.isArray(player.positions) ? player.positions : [];
      return list
        .slice(0, 2)
        .map((v) => {
          const key = `player.position.${v}`;
          if (te(key)) return t(key);
          // 词典外位置码（如垒球 XF 系）：中文语种沿用 POSITION_LABELS 标签，其余语种回退原始代码（对齐 PlayerDetailContent 先例）
          return String(locale.value).startsWith('zh') ? POSITION_LABELS[v] ?? v : v;
        })
        .join(' / ');
    }

    function playerCard(player) {
      return h(
        RouterLink,
        { key: player.id, class: 'free-agent-card portal-card-link', to: portalPath(`players/${player.id}`) },
        [
          h('div', { class: 'free-agent-avatar' }, [
            h('img', {
              src: resolveAssetUrl(player.avatar) || DEFAULT_AVATAR_SVG_PATH,
              alt: player.name || '',
              onError: onAvatarError
            })
          ]),
          h('div', { class: 'free-agent-body' }, [
            h('h3', { class: 'free-agent-name' }, player.name || player.shortName || ''),
            h('p', { class: 'free-agent-pos' }, positionsText(player))
          ])
        ]
      );
    }

    return () =>
      h('div', { class: 'portal-free-agents' }, [
        h('div', { class: 'free-agents-toolbar' }, [h('h2', { class: 'free-agents-title' }, t('freeAgents.title'))]),
        rows.value.length === 0 && !loading.value
          ? h(ElEmpty, { description: t('freeAgents.empty') })
          : h('div', { class: 'free-agents-grid' }, rows.value.map(playerCard)),
        total.value > pageSize.value
          ? h('div', { class: 'free-agents-pager' }, [
              h(ElPagination, {
                currentPage: page.value,
                pageSize: pageSize.value,
                total: total.value,
                layout: 'prev, pager, next',
                'onUpdate:currentPage': (v) => {
                  page.value = v;
                  load();
                }
              })
            ])
          : null
      ]);
  }
});
