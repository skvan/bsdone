#!/bin/bash
# =====================================================================
# TEST-ENV DEPLOY (repo version, executed by .github/workflows/deploy-test.yml)
# ---------------------------------------------------------------------
# Baseline: .qoder/deploy-dev.sh (local emergency channel) + CI preflight.
# Replaces ONLY test-env resources (production is never touched):
#   1. /usr/local/java-server-test/bsball-server.jar  <- dev build (v1.1/dev)
#   2. /usr/local/webapps-test/                       <- dev frontend
#   3. restarts the java-server-test unit
#
# Invoked by CI as: bash -n $(basename).new && bash $(basename).new
# Staging inputs (uploaded + md5-verified by the workflow):
#   /root/deploy-dev/bsball-server-1.0.0.jar
#   /root/deploy-dev/webapps-dev.tar.gz
#
# Backups       : /root/backup-test-deploy/<stamp>/
# Auto-rollback : if 8081 /health does not answer within 120s, the previous
#                 jar + webapps are restored and the service restarted.
# NEVER TOUCHED : java-server unit, /usr/local/java-server/*,
#                 /usr/local/webapps/*, nginx config, database "bsball".
# =====================================================================

set -u

STAMP=$(date +%Y%m%d-%H%M%S)
STAGE=/root/deploy-dev
JAR_NEW=$STAGE/bsball-server-1.0.0.jar
TGZ_NEW=$STAGE/webapps-dev.tar.gz
APP_DIR=/usr/local/java-server-test
WEB_DIR=/usr/local/webapps-test
BAK_DIR=/root/backup-test-deploy/$STAMP
HEALTH=http://127.0.0.1:8081/bsball-server/health
ENVFILE=/etc/bsball/java-server-test.env

log()  { echo; echo "=== $* ==="; }
ok()   { echo "[OK] $*"; }
fail() {
    echo
    echo "!!!!! ABORT: $*"
    echo "--- state snapshot ---"
    systemctl --no-pager --lines=0 status java-server-test 2>/dev/null | head -5 || true
    echo "backup dir (if created): $BAK_DIR"
    ls -la "$BAK_DIR" 2>/dev/null || true
    if [ -f "$APP_DIR/bsball-server.jar" ] && ! systemctl is-active --quiet java-server-test; then
        echo "--- safety net: trying to (re)start java-server-test with jar currently on disk ---"
        systemctl start java-server-test 2>/dev/null || true
        sleep 10
        RC=$(curl -s -o /dev/null -m 5 -w "%{http_code}" "$HEALTH" 2>/dev/null)
        echo "post-fail health code = $RC  (200 = test env is serving again)"
        [ "$RC" = "200" ] || echo "!! test env still down - check: journalctl -u java-server-test -n 50"
    fi
    echo "!!  Inspect the snapshot above before re-running anything."
    exit 1
}
pgq() { sudo -u postgres psql -Atd "$1" -c "$2" 2>/dev/null; }

log "TEST DEPLOY (stamp=$STAMP)"
echo "host=$(hostname)  user=$(id -un)  app=$APP_DIR  port=8081"

# ---------------- STEP 0: preflight ----------------
log "STEP 0/6  PREFLIGHT & BASELINE"
[ -f "$JAR_NEW" ] || fail "staged jar missing: $JAR_NEW"
[ -f "$TGZ_NEW" ] || fail "staged tarball missing: $TGZ_NEW"
JSZ=$(stat -c%s "$JAR_NEW"); [ "$JSZ" -ge 50000000 ] || fail "staged jar too small ($JSZ bytes)"
[ -d "$APP_DIR" ] && [ -f "$APP_DIR/bsball-server.jar" ] || fail "$APP_DIR/bsball-server.jar missing"
systemctl list-unit-files 2>/dev/null | grep -q "^java-server-test" || fail "unit java-server-test not found"
command -v curl >/dev/null || fail "curl not found"
echo "--- unit definition (safety check) ---"
systemctl cat java-server-test | grep -E "ExecStart|WorkingDirectory|DB_NAME"
systemctl cat java-server-test | grep -q "DB_NAME=bsball_test" || fail "unit does not target bsball_test - refusing to deploy"
systemctl cat java-server-test | grep -q "server.port=8081" || fail "unit is not on port 8081 - refusing to deploy"
echo "--- env-file wiring (sensitive config via EnvironmentFile) ---"
[ -f "$ENVFILE" ] || fail "$ENVFILE missing - sensitive config must come from EnvironmentFile; deploy refused"
systemctl show java-server-test -p EnvironmentFiles | grep -q "$ENVFILE" || fail "java-server-test has NO EnvironmentFile binding to $ENVFILE - deploy refused"
ok "env-file wiring verified"
echo "--- staged artifact md5 (must match CI build log) ---"
md5sum "$JAR_NEW" "$TGZ_NEW"
echo "--- current test jar md5 (to be replaced) ---"
md5sum "$APP_DIR/bsball-server.jar"
echo "--- prod baseline (must be identical at the end) ---"
systemctl show java-server -p MainPID -p NRestarts -p ActiveEnterTimestamp
BASE_CONN=$(pgq postgres "select count(*) from pg_stat_activity where datname='bsball'")
echo "prod db connections baseline = $BASE_CONN"
echo "--- bsball_test flyway history (top 5, BEFORE) ---"
pgq bsball_test "select version||' | '||description||' | success='||success from flyway_schema_history order by installed_rank desc limit 5"
AV_KB=$(df --output=avail / | tail -1 | tr -d ' ')
[ "$AV_KB" -ge 1048576 ] || fail "less than 1G free disk"
ok "preflight passed"

# ---------------- STEP 1: backup ----------------
log "STEP 1/6  BACKUP (jar + webapps -> $BAK_DIR)"
mkdir -p "$BAK_DIR" || fail "mkdir backup failed"
cp -p "$APP_DIR/bsball-server.jar" "$BAK_DIR/bsball-server.jar.bak" || fail "jar backup failed"
echo "  backing up webapps-test (may take a minute)..."
tar -C /usr/local -czf "$BAK_DIR/webapps-test.tar.gz" webapps-test || fail "webapps backup failed"
ls -la "$BAK_DIR"
ok "backup complete"

# ---------------- STEP 2: stop ----------------
log "STEP 2/6  STOP java-server-test"
systemctl stop java-server-test || fail "stop failed"
sleep 2
echo "is-active now: $(systemctl is-active java-server-test || true)  (expect inactive)"
ok "test service stopped (port 8081 freed)"

# ---------------- STEP 3: replace jar ----------------
log "STEP 3/6  REPLACE JAR"
PERM=$(stat -c '%a' "$APP_DIR/bsball-server.jar")
cp "$JAR_NEW" "$APP_DIR/bsball-server.jar.tmp" || fail "jar copy failed"
chmod "$PERM" "$APP_DIR/bsball-server.jar.tmp"
mv -f "$APP_DIR/bsball-server.jar.tmp" "$APP_DIR/bsball-server.jar" || fail "jar swap failed"
M1=$(md5sum "$JAR_NEW" | cut -d' ' -f1); M2=$(md5sum "$APP_DIR/bsball-server.jar" | cut -d' ' -f1)
[ "$M1" = "$M2" ] || fail "installed jar md5 mismatch"
echo "installed jar md5=$M2  (perm $PERM)"
ok "dev jar in place"

# ---------------- STEP 4: replace webapps ----------------
log "STEP 4/6  REPLACE WEBAPPS-TEST"
rm -rf "$STAGE/webapps"
tar -C "$STAGE" -xzf "$TGZ_NEW" || fail "tar extract failed"
[ -d "$STAGE/webapps" ] || fail "tarball has no webapps/ root"
rm -rf "$WEB_DIR" || fail "remove old webapps failed"
mv "$STAGE/webapps" "$WEB_DIR" || fail "move new webapps failed"
chown -R root:root "$WEB_DIR" 2>/dev/null || true
chmod -R a+rX "$WEB_DIR" || true
echo "webapps-test size: $(du -sh "$WEB_DIR" | cut -f1)"
ls "$WEB_DIR"
ok "frontend files replaced"

# ---------------- STEP 5: start + health ----------------
log "STEP 5/6  START & HEALTH (up to 120s)"
systemctl start java-server-test || { journalctl -u java-server-test -n 40 --no-pager; fail "systemctl start failed"; }
UP=0
for i in $(seq 1 24); do
    CODE=$(curl -s -o /tmp/dev-health-$STAMP.json -m 3 -w "%{http_code}" "$HEALTH" 2>/dev/null)
    echo "  [$i/24] health probe -> $CODE"
    [ "$CODE" = "200" ] && { UP=1; break; }
    sleep 5
done
if [ "$UP" != "1" ]; then
    echo "--- journalctl tail (dev jar startup) ---"
    journalctl -u java-server-test -n 80 --no-pager | tail -40
    echo
    echo "--- ROLLBACK: restoring previous jar + webapps ---"
    systemctl stop java-server-test
    cp -p "$BAK_DIR/bsball-server.jar.bak" "$APP_DIR/bsball-server.jar" && echo "jar restored"
    rm -rf "$WEB_DIR"
    tar -C /usr/local -xzf "$BAK_DIR/webapps-test.tar.gz" && echo "webapps restored"
    systemctl start java-server-test
    sleep 10
    RC=$(curl -s -o /dev/null -m 5 -w "%{http_code}" "$HEALTH")
    echo "rollback health code = $RC  (200 = test env back on previous jar)"
    [ "$RC" = "200" ] || echo "!! rollback health still failing - manual attention needed (worst case: re-restore bsball_test from newest dump)"
    fail "dev jar failed health - auto-rolled back to previous state"
fi
echo "health: $(cat /tmp/dev-health-$STAMP.json)"
echo "--- startup highlights ---"
journalctl -u java-server-test --since "5 min ago" --no-pager 2>/dev/null | grep -Ei "started|local:|migrat|flyway|schema" | tail -12 || true
echo "--- flyway history (top 5, AFTER) ---"
pgq bsball_test "select version||' | '||description||' | success='||success from flyway_schema_history order by installed_rank desc limit 5"
echo "--- db wiring ---"
pgq postgres "select datname||' = '||count(*) from pg_stat_activity where datname in ('bsball','bsball_test') group by datname order by datname"
TEST_CONN=$(pgq postgres "select count(*) from pg_stat_activity where datname='bsball_test'")
[ "${TEST_CONN:-0}" -ge 1 ] || fail "no connection on bsball_test - wiring problem"
ok "dev backend is up on 8081"

# ---------------- STEP 6: smoke + impact proof ----------------
log "STEP 6/6  SMOKE VIA NGINX + PROD IMPACT PROOF"
P1=$(curl -s -o /dev/null -m 5 -w "%{http_code}" -H "Host: test.bsdone.com" http://127.0.0.1/)
P2=$(curl -s -m 5 -H "Host: test.bsdone.com" http://127.0.0.1/bsball-server/health)
P3=$(curl -s -o /dev/null -m 5 -w "%{http_code}" -H "Host: bsdone.com" http://127.0.0.1/)
P4=$(curl -sk -o /dev/null -m 5 -w "%{http_code}" -H "Host: bsdone.com" https://127.0.0.1/)
echo "test  http  /        -> $P1 (expect 200)"
echo "test  api   /health  -> $P2"
echo "prod  http  redirect -> $P3 (expect 301)"
echo "prod  https ssl      -> $P4 (expect 200)"
echo
echo "--- prod impact proof (must equal STEP 0 baseline) ---"
systemctl show java-server -p MainPID -p NRestarts -p ActiveEnterTimestamp
echo "prod db connections now = $(pgq postgres "select count(*) from pg_stat_activity where datname='bsball'") (baseline $BASE_CONN)"
echo "--- test service state ---"
systemctl show java-server-test -p MainPID -p ActiveEnterTimestamp -p NRestarts

echo
echo "===== TEST DEPLOY COMPLETE ====="
echo "deployed : v1.1/dev jar + webapps  (stamp $STAMP)"
echo "backup   : $BAK_DIR"
echo "manual rollback (only if ever needed):"
echo "  systemctl stop java-server-test"
echo "  cp -p $BAK_DIR/bsball-server.jar.bak $APP_DIR/bsball-server.jar"
echo "  rm -rf $WEB_DIR && tar -C /usr/local -xzf $BAK_DIR/webapps-test.tar.gz"
echo "  systemctl start java-server-test"
