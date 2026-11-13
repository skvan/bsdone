<template>
  <el-button
    link
    type="primary"
    class="event-tournament-link"
    :aria-label="t('events.standings')"
    @click="open"
  >
    <el-icon><Trophy /></el-icon>
    <span v-if="!iconOnly">{{ t('events.standings') }}</span>
  </el-button>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { Trophy } from '@element-plus/icons-vue';

const props = defineProps({
  eventId: { type: [Number, String], required: true },
  iconOnly: { type: Boolean, default: false }
});
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

function open() {
  router.push({
    name: 'PortalEventDetail',
    params: {
      tenantCode: String(route.params.tenantCode || ''),
      eventId: String(props.eventId)
    }
  });
}
</script>

<style scoped>
.event-tournament-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 4px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}
</style>
