#!/bin/bash
# =====================================================================
# PROD deploy script - executed on the server by the GitHub Actions
# workflow "Deploy Production (manual)" (or manually for emergency).
# ---------------------------------------------------------------------
# Replaces ONLY /usr/local/java-server/bsball-server.jar and restarts
# java-server. Backs up the previous jar; auto-rolls-back when the
# health check fails.
#
# SAFETY GATE: the new jar has NO built-in secrets (all sensitive config
# comes from /etc/bsball/java-server.env via the systemd EnvironmentFile,
# plus the external config/application-prod.yml). This script REFUSES to
# proceed if that wiring is missing - otherwise a restart would take the
# service down.
#
# Staging input : /root/deploy-prod/staging/bsball-server-1.0.0.jar.new
# Backups       : /root/backup-prod-deploy/<stamp>/
# =====================================================================
set -u

STAMP=$(date +%Y%m%d-%H%M%S)
STAGING=/root/deploy-prod/staging
JAR_NEW=$STAGING/bsball-server-1.0.0.jar.new
APP_DIR=/usr/local/java-server
BAK_DIR=/root/backup-prod-deploy/$STAMP
HEALTH=http://127.0.0.1:8080/bsball-server/health

log()  { echo; echo "=== $* ==="; }
ok()   { echo "[OK] $*"; }
fail() {
    echo
    echo "!!!!! ABORT: $*"
    echo "--- state snapshot ---"
    systemctl --no-pager --lines=0 status java-server 2>/dev/null | head -5 || true
    echo "backup dir (if created): $BAK_DIR"
    ls -la "$BAK_DIR" 2>/dev/null || true
    echo "!!  Inspect the snapshot above before re-running anything."
    exit 1
}

log "PROD DEPLOY (stamp=$STAMP)"
echo "host=$(hostname)  user=$(id -un)  app=$APP_DIR  port=8080"

# ---------------- STEP 0: preflight + SAFETY GATE ----------------
log "STEP 0/5  PREFLIGHT + SAFETY GATE"
[ -f "$JAR_NEW" ] || fail "staged jar missing: $JAR_NEW"
JSZ=$(stat -c%s "$JAR_NEW"); [ "$JSZ" -ge 50000000 ] || fail "staged jar too small ($JSZ bytes)"
[ -f "$APP_DIR/bsball-server.jar" ] || fail "current prod jar missing"
systemctl list-unit-files 2>/dev/null | grep -q "^java-server" || fail "unit java-server not found"
command -v curl >/dev/null || fail "curl not found"

echo "--- safety gate: sensitive config must come from env + external config ---"
[ -f /etc/bsball/java-server.env ] || fail "/etc/bsball/java-server.env missing - the new jar would have NO DB_PASSWORD"
systemctl show java-server -p EnvironmentFiles | grep -q '/etc/bsball/java-server.env' || fail "java-server has NO EnvironmentFile binding - the new jar would lack secrets"
[ -f "$APP_DIR/config/application-prod.yml" ] || fail "external config $APP_DIR/config/application-prod.yml missing"
ok "env + external-config wiring verified"

echo "--- staged jar md5 (must match the CI build log) ---"
md5sum "$JAR_NEW"
echo "--- current prod jar md5 (to be replaced) ---"
md5sum "$APP_DIR/bsball-server.jar"

echo "--- baseline ---"
systemctl show java-server -p MainPID -p NRestarts -p ActiveEnterTimestamp
BASE_CODE=$(curl -s -o /dev/null -m 5 -w '%{http_code}' "$HEALTH")
echo "current health code = $BASE_CODE"

# ---------------- STEP 1: backup ----------------
log "STEP 1/5  BACKUP -> $BAK_DIR"
mkdir -p "$BAK_DIR" || fail "mkdir backup failed"
cp -p "$APP_DIR/bsball-server.jar" "$BAK_DIR/bsball-server.jar.bak" || fail "jar backup failed"
ls -la "$BAK_DIR"
ok "backup complete"

# ---------------- STEP 2: swap jar (atomic) ----------------
log "STEP 2/5  SWAP JAR"
PERM=$(stat -c '%a' "$APP_DIR/bsball-server.jar")
cp "$JAR_NEW" "$APP_DIR/bsball-server.jar.tmp" || fail "jar copy failed"
chmod "$PERM" "$APP_DIR/bsball-server.jar.tmp"
mv -f "$APP_DIR/bsball-server.jar.tmp" "$APP_DIR/bsball-server.jar" || fail "jar swap failed"
M1=$(md5sum "$JAR_NEW" | cut -d' ' -f1); M2=$(md5sum "$APP_DIR/bsball-server.jar" | cut -d' ' -f1)
[ "$M1" = "$M2" ] || fail "installed jar md5 mismatch"
echo "installed jar md5=$M2  (perm $PERM)"
ok "new jar in place"

# ---------------- STEP 3: restart + health ----------------
log "STEP 3/5  RESTART java-server & HEALTH (up to 150s)"
systemctl restart java-server || { journalctl -u java-server -n 40 --no-pager; fail "systemctl restart failed"; }
UP=0
for i in $(seq 1 30); do
    CODE=$(curl -s -o /dev/null -m 3 -w '%{http_code}' "$HEALTH" 2>/dev/null)
    echo "  [$i/30] health -> $CODE"
    [ "$CODE" = "200" ] && { UP=1; break; }
    sleep 5
done
if [ "$UP" != "1" ]; then
    echo "--- journalctl tail (new jar startup) ---"
    journalctl -u java-server -n 80 --no-pager | tail -40
    echo
    echo "--- ROLLBACK: restoring previous jar ---"
    systemctl stop java-server
    cp -p "$BAK_DIR/bsball-server.jar.bak" "$APP_DIR/bsball-server.jar" && echo "jar restored"
    systemctl start java-server
    sleep 10
    RC=$(curl -s -o /dev/null -m 5 -w '%{http_code}' "$HEALTH")
    echo "rollback health code = $RC  (200 = prod back on the previous jar)"
    [ "$RC" = "200" ] || echo "!! rollback health still failing - MANUAL ATTENTION NEEDED"
    fail "new jar failed health - auto-rolled back to previous state"
fi
echo "health: $(curl -s -m 5 "$HEALTH")"

echo "--- startup highlights ---"
journalctl -u java-server --since "3 min ago" --no-pager 2>/dev/null | grep -Ei "started|profile|flyway|migrat" | tail -8 || true

# ---------------- STEP 4: smoke via nginx ----------------
log "STEP 4/5  SMOKE VIA NGINX"
P1=$(curl -s -o /dev/null -m 5 -w '%{http_code}' -H 'Host: bsdone.com' http://127.0.0.1/)
P2=$(curl -sk -o /dev/null -m 5 -w '%{http_code}' -H 'Host: bsdone.com' https://127.0.0.1/)
P3=$(curl -s -m 5 -H 'Host: bsdone.com' http://127.0.0.1/bsball-server/health)
echo "prod  http  -> $P1 (expect 301)"
echo "prod  https -> $P2 (expect 200)"
echo "prod  api   /health -> $P3"

# ---------------- STEP 5: summary ----------------
log "STEP 5/5  SUMMARY"
systemctl show java-server -p MainPID -p NRestarts -p ActiveEnterTimestamp
echo
echo "===== PROD DEPLOY COMPLETE ====="
echo "deployed jar md5 : $M2"
echo "backup           : $BAK_DIR"
echo "manual rollback (only if ever needed):"
echo "  systemctl stop java-server"
echo "  cp -p $BAK_DIR/bsball-server.jar.bak $APP_DIR/bsball-server.jar"
echo "  systemctl start java-server"
