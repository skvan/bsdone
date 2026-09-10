-- 赛事、比赛、业务新闻、后台公告、系统公告组件：按租户隔离
DO $flyway$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_event') THEN
        ALTER TABLE bs_event ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_game') THEN
        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_news') THEN
        ALTER TABLE bs_news ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'sys_notice') THEN
        ALTER TABLE sys_notice ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'sys_announcement') THEN
        ALTER TABLE sys_announcement ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_event')
       AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_league') THEN
        UPDATE bs_event ev
        SET tenant_id = l.tenant_id
        FROM bs_league l
        WHERE ev.league_id = l.id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_game')
       AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_event') THEN
        UPDATE bs_game g
        SET tenant_id = e.tenant_id
        FROM bs_event e
        WHERE g.event_id = e.id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_event') THEN
        CREATE INDEX IF NOT EXISTS idx_bs_event_tenant_id ON bs_event(tenant_id);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_game') THEN
        CREATE INDEX IF NOT EXISTS idx_bs_game_tenant_id ON bs_game(tenant_id);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_news') THEN
        CREATE INDEX IF NOT EXISTS idx_bs_news_tenant_id ON bs_news(tenant_id);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'sys_notice') THEN
        CREATE INDEX IF NOT EXISTS idx_sys_notice_tenant_id ON sys_notice(tenant_id);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'sys_announcement') THEN
        CREATE INDEX IF NOT EXISTS idx_sys_announcement_tenant_id ON sys_announcement(tenant_id);
    END IF;
END
$flyway$;
