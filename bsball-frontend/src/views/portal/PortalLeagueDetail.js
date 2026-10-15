// PortalLeagueDetail —— 门户「联盟详情」：联盟信息 + 下辖赛事列表（Issue #329）
import { defineComponent, h, ref, watch } from 'vue';
import { ElButton, ElEmpty, ElSkeleton, ElTag } from 'element-plus';
import { RouterLink, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { fetchAllPages, fetchResult } from '../../api/request';
import { useDetailLoad } from '../../composables/useDetailLoad';
import { useTenantRouter } from '../../composables/useTenantRouter';
import { formatSmartDateTime } from '../../utils/dateExtras';
import { filterEventsByLeague } from '../../utils/leagueEvents';
import { resolveYearQuery } from '../../utils/portalYearQuery';
import '../../styles/legacy/portal-leagues.css';
import '../../styles/legacy/portal-league-detail.css';

const EVENT_STATUS_TAG_TYPES = { draft: 'info', ongoing: 'success' };

export default defineComponent({
  name: 'PortalLeagueDetail',
  setup() {
    const { t, te } = useI18n();
    const route = useRoute();
    const { portalPath } = useTenantRouter();

    const info = ref(null);
    const events = ref([]);

    const { loading, error, retry } = useDetailLoad(async () => {
      const id = Number(route.params.id);
      if (!Number.isFinite(id) || id <= 0) {
        info.value = null;
        events.value = [];
        return;
      }
      const { data, error: loadError } = await fetchResult(`/api/league/${id}`);
      if (loadError) throw new Error(loadError);
      info.value = data ?? null;
      if (info.value == null) {
        events.value = [];
        return;
      }
      const { list, error: listError } = await fetchAllPages('/api/event/list');
      if (listError) throw new Error(listError);
      events.value = filterEventsByLeague(list, id);
    });

    watch(() => route.params.id, retry);

    function eventStatusText(status) {
      const key = `events.eventStatus.${status}`;
      return status && te(key) ? t(key) : String(status || '');
    }

    function eventCard(ev) {
      const dates =
        ev.startDate && ev.endDate
          ? `${formatSmartDateTime(ev.startDate)} ~ ${formatSmartDateTime(ev.endDate)}`
          : formatSmartDateTime(ev.startDate || ev.endDate);
      return h(
        RouterLink,
        {
          key: ev.id,
          class: 'league-card portal-card-link',
          to: { path: portalPath('events'), query: { eventId: String(ev.id), ...resolveYearQuery(ev) } }
        },
        [
          h('div', { class: 'league-head' }, [
            h('span', { class: 'league-title' }, ev.name || ''),
            ev.season ? h('span', { class: 'league-sub' }, ev.season) : null
          ]),
          ev.startDate || ev.endDate ? h('p', { class: 'league-meta' }, dates) : null,
          ev.status
            ? h(ElTag, { size: 'small', type: EVENT_STATUS_TAG_TYPES[ev.status] ?? 'info' }, () => eventStatusText(ev.status))
            : null
        ]
      );
    }

    return () => {
      if (loading.value) {
        return h('div', { class: 'portal-leagues' }, [h(ElSkeleton, { rows: 6, animated: true })]);
      }
      if (error.value) {
        return h('div', { class: 'portal-leagues' }, [
          h(ElEmpty, { description: error.value }, () => [
            h(ElButton, { type: 'primary', onClick: retry }, () => t('common.retry'))
          ])
        ]);
      }
      if (!info.value) {
        return h('div', { class: 'portal-leagues' }, [h(ElEmpty, { description: t('leagueDetail.notFound') })]);
      }
      return h('div', { class: 'portal-leagues' }, [
        h('div', { class: 'league-detail-info' }, [
          h('h2', { class: 'leagues-title' }, info.value.name || ''),
          info.value.nameEn ? h('span', { class: 'league-detail-name-en' }, info.value.nameEn) : null,
          info.value.description ? h('p', { class: 'league-detail-desc' }, info.value.description) : null
        ]),
        h('h3', { class: 'league-detail-events-title' }, t('leagueDetail.events')),
        events.value.length === 0
          ? h(ElEmpty, { description: t('leagueDetail.emptyEvents') })
          : h('div', { class: 'leagues-grid' }, events.value.map(eventCard))
      ]);
    };
  }
});
