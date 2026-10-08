// PortalLeagues —— 门户「联盟一览」（只读；申请入口已按口径移至管理台发起，管理员/上级审核）
import { defineComponent, h, ref, onMounted } from 'vue';
import { ElPagination, ElEmpty, ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { leagueApi } from '../../api/business';
import '../../styles/legacy/portal-leagues.css';

export default defineComponent({
  name: 'PortalLeagues',
  setup() {
    const { t } = useI18n();

    const loading = ref(false);
    const rows = ref([]);
    const page = ref(1);
    const pageSize = ref(24);
    const total = ref(0);

    async function load() {
      loading.value = true;
      try {
        const { list, total: count } = await leagueApi.list({ page: page.value, pageSize: pageSize.value });
        rows.value = list ?? [];
        total.value = count ?? 0;
      } catch (e) {
        ElMessage.error((e && e.message) || t('portalLeagues.failed'));
      } finally {
        loading.value = false;
      }
    }

    onMounted(load);

    return () => {
      const cards = rows.value.map((l) =>
        h('div', { class: 'league-card', key: l.id }, [
          h('div', { class: 'league-head' }, [
            h('span', { class: 'league-title' }, l.name || ''),
            l.nameEn ? h('span', { class: 'league-sub' }, l.nameEn) : null
          ]),
          l.description ? h('p', { class: 'league-desc' }, l.description) : null
        ])
      );
      return h('div', { class: 'portal-leagues' }, [
        h('div', { class: 'leagues-toolbar' }, [h('h2', { class: 'leagues-title' }, t('portalLeagues.title'))]),
        rows.value.length === 0 && !loading.value
          ? h(ElEmpty, { description: t('portalLeagues.empty') })
          : h('div', { class: 'leagues-grid' }, cards),
        total.value > pageSize.value
          ? h('div', { class: 'leagues-pager' }, [
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
    };
  }
});
