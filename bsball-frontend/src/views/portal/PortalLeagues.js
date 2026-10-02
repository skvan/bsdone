// PortalLeagues —— 门户「联盟一览」+ 自助申请创建联盟（建盟链路批次①）
// 数据：GET /api/league/list（公开可读）；申请：POST /api/portal/league-create（登录用户；
// 需审核租户落申请→管理端审核，关闭开关则直建并自动授权为主办方）。
import { defineComponent, h, ref, onMounted } from 'vue';
import { ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElPagination, ElEmpty, ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { leagueApi } from '../../api/business';
import { useAuthStore } from '../../stores/auth';
import { useTenantRouter } from '../../composables/useTenantRouter';
import '../../styles/legacy/portal-leagues.css';

export default defineComponent({
  name: 'PortalLeagues',
  setup() {
    const { t } = useI18n();
    const router = useRouter();
    const auth = useAuthStore();
    const { portalPath } = useTenantRouter();

    const loading = ref(false);
    const rows = ref([]);
    const page = ref(1);
    const pageSize = ref(24);
    const total = ref(0);

    const dlg = ref(false);
    const saving = ref(false);
    const formRef = ref(null);
    const form = ref({ name: '', nameEn: '', description: '' });
    const rules = {
      name: [{ required: true, message: t('portalLeagues.name'), trigger: 'blur' }],
      description: [{ max: 2000, trigger: 'blur' }]
    };

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

    function openApply() {
      if (!auth.user) {
        ElMessage.warning(t('portalLeagues.loginRequired'));
        router.push({ path: portalPath('account/login'), query: { redirect: router.currentRoute.value.fullPath } });
        return;
      }
      form.value = { name: '', nameEn: '', description: '' };
      dlg.value = true;
    }

    async function submit() {
      try {
        await formRef.value.validate();
      } catch {
        return;
      }
      saving.value = true;
      try {
        const res = await leagueApi.portalCreate({ ...form.value });
        const data = (res && res.data) || res || {};
        if (data.pending) ElMessage.success(t('portalLeagues.pending'));
        else ElMessage.success(t('portalLeagues.created'));
        dlg.value = false;
        page.value = 1;
        await load();
      } catch (e) {
        ElMessage.error((e && e.message) || t('portalLeagues.failed'));
      } finally {
        saving.value = false;
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
        h('div', { class: 'leagues-toolbar' }, [
          h('h2', { class: 'leagues-title' }, t('portalLeagues.title')),
          h(ElButton, { type: 'primary', onClick: openApply }, () => t('portalLeagues.apply'))
        ]),
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
          : null,
        h(
          ElDialog,
          {
            modelValue: dlg.value,
            'onUpdate:modelValue': (v) => {
              dlg.value = v;
            },
            title: t('portalLeagues.apply'),
            width: '480px'
          },
          {
            default: () =>
              h(ElForm, { ref: formRef, model: form.value, rules, labelWidth: '90px' }, () => [
                h(ElFormItem, { label: t('portalLeagues.name'), prop: 'name' }, () =>
                  h(ElInput, {
                    modelValue: form.value.name,
                    'onUpdate:modelValue': (v) => {
                      form.value.name = v;
                    }
                  })
                ),
                h(ElFormItem, { label: t('portalLeagues.nameEn') }, () =>
                  h(ElInput, {
                    modelValue: form.value.nameEn,
                    'onUpdate:modelValue': (v) => {
                      form.value.nameEn = v;
                    }
                  })
                ),
                h(ElFormItem, { label: t('portalLeagues.description') }, () =>
                  h(ElInput, {
                    modelValue: form.value.description,
                    'onUpdate:modelValue': (v) => {
                      form.value.description = v;
                    },
                    type: 'textarea',
                    rows: 3,
                    maxlength: 2000,
                    showWordLimit: true
                  })
                )
              ]),
            footer: () => [
              h(
                ElButton,
                {
                  onClick: () => {
                    dlg.value = false;
                  }
                },
                () => t('portalLeagues.cancel')
              ),
              h(ElButton, { type: 'primary', loading: saving.value, onClick: submit }, () => t('portalLeagues.submit'))
            ]
          }
        )
      ]);
    };
  }
});
