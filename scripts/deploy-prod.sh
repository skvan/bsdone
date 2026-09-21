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
# Staging inputs: /root/deploy-prod/staging/bsball-server-1.0.0.jar.new (required)
#                 /root/deploy-prod/staging/webapps-prod.tar.gz.new   (optional:
#                 frontend package; when present it is deployed in STEP 4 and
#                 renamed to webapps-prod.tar.gz.deployed-<stamp> afterwards)
# Backups       : /root/backup-prod-deploy/<stamp>/  (jar + webapps.tar.gz)
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

FRONT_NEW=$STAGING/webapps-prod.tar.gz.new
FRONT_RESULT="skipped (no package staged)"
if [ -f "$FRONT_NEW" ]; then
    tar -tzf "$FRONT_NEW" >/dev/null 2>&1 || fail "staged frontend package is not a valid tar.gz"
    tar -tzf "$FRONT_NEW" | grep -q '^webapps/' || fail "staged frontend package lacks a top-level webapps/ root"
    echo "frontend package staged: $(du -h "$FRONT_NEW" | cut -f1) - will be deployed in STEP 4"
fi

echo "--- staged jar md5 (must match the CI build log) ---"
md5sum "$JAR_NEW"
echo "--- current prod jar md5 (to be replaced) ---"
md5sum "$APP_DIR/bsball-server.jar"

echo "--- baseline ---"
systemctl show java-server -p MainPID -p NRestarts -p ActiveEnterTimestamp
BASE_CODE=$(curl -s -o /dev/null -m 5 -w '%{http_code}' "$HEALTH")
echo "current health code = $BASE_CODE"

# ---------------- STEP 1: backup ----------------
log "STEP 1/6  BACKUP -> $BAK_DIR"
mkdir -p "$BAK_DIR" || fail "mkdir backup failed"
cp -p "$APP_DIR/bsball-server.jar" "$BAK_DIR/bsball-server.jar.bak" || fail "jar backup failed"
ls -la "$BAK_DIR"
ok "backup complete"

# ---------------- STEP 2: swap jar (atomic) ----------------
log "STEP 2/6  SWAP JAR"
PERM=$(stat -c '%a' "$APP_DIR/bsball-server.jar")
cp "$JAR_NEW" "$APP_DIR/bsball-server.jar.tmp" || fail "jar copy failed"
chmod "$PERM" "$APP_DIR/bsball-server.jar.tmp"
mv -f "$APP_DIR/bsball-server.jar.tmp" "$APP_DIR/bsball-server.jar" || fail "jar swap failed"
M1=$(md5sum "$JAR_NEW" | cut -d' ' -f1); M2=$(md5sum "$APP_DIR/bsball-server.jar" | cut -d' ' -f1)
[ "$M1" = "$M2" ] || fail "installed jar md5 mismatch"
echo "installed jar md5=$M2  (perm $PERM)"
ok "new jar in place"

# ---------------- STEP 3: restart + health ----------------
log "STEP 3/6  RESTART java-server & HEALTH (up to 150s)"
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

# ---------------- STEP 4: frontend (optional) ----------------
log "STEP 4/6  FRONTEND (optional)"
FRONT_DIR=/usr/local/webapps
if [ -f "$FRONT_NEW" ]; then
    echo "deploying staged frontend package ..."
    rm -rf "$STAGING/webapps"
    tar -C "$STAGING" -xzf "$FRONT_NEW" || fail "frontend tar extract failed (corrupt package?)"
    [ -d "$STAGING/webapps" ] || fail "frontend package has no webapps/ root after extract"

    tar -C /usr/local -czf "$BAK_DIR/webapps.tar.gz" webapps || fail "frontend backup failed"
    echo "old frontend backed up: $(du -h "$BAK_DIR/webapps.tar.gz" | cut -f1)"

    OLD=$FRONT_DIR.old-$STAMP
    mv "$FRONT_DIR" "$OLD" || fail "cannot move current frontend aside"
    mv "$STAGING/webapps" "$FRONT_DIR" || fail "cannot move new frontend into place"
    # keep top-level entries that exist only in the current deployment (e.g. home/)
    for item in "$OLD"/*; do
        b=$(basename "$item")
        if [ ! -e "$FRONT_DIR/$b" ]; then
            echo "  keeping prod-only entry: $b"
            mv "$item" "$FRONT_DIR/$b"
        fi
    done
    chown -R root:root "$FRONT_DIR" 2>/dev/null || true
    chmod -R a+rX "$FRONT_DIR" || true

    FS1=$(curl -s -o /dev/null -m 5 -w '%{http_code}' -H 'Host: bsdone.com' http://127.0.0.1/)
    FS2=$(curl -s -o /dev/null -m 5 -w '%{http_code}' -H 'Host: bsdone.com' http://127.0.0.1/portal.html)
    FS3=$(curl -s -o /dev/null -m 5 -w '%{http_code}' -H 'Host: bsdone.com' http://127.0.0.1/bs-ball/bs-ball)
    echo "frontend smoke: / -> $FS1  /portal.html -> $FS2  /bs-ball/bs-ball -> $FS3 (expect 200)"
    if [ "$FS1" = "200" ] && [ "$FS2" = "200" ] && [ "$FS3" = "200" ]; then
        rm -rf "$OLD"
        mv -f "$FRONT_NEW" "$STAGING/webapps-prod.tar.gz.deployed-$STAMP" || true
        FRONT_RESULT="deployed (backup: $BAK_DIR/webapps.tar.gz)"
        ok "frontend deployed"
    else
        echo "--- FRONTEND ROLLBACK ---"
        for item in "$FRONT_DIR"/*; do
            b=$(basename "$item")
            if [ ! -e "$OLD/$b" ]; then mv "$item" "$OLD/$b"; fi
        done
        mv "$FRONT_DIR" "$FRONT_DIR.failed-$STAMP" 2>/dev/null || true
        mv "$OLD" "$FRONT_DIR" || echo "!! frontend rollback move failed - MANUAL ATTENTION"
        RC2=$(curl -s -o /dev/null -m 5 -w '%{http_code}' -H 'Host: bsdone.com' http://127.0.0.1/)
        echo "rollback frontend smoke / -> $RC2"
        fail "frontend smoke failed - frontend rolled back"
    fi
else
    echo "no frontend package staged - frontend unchanged (normal when only the backend changed)"
fi
ok "frontend step done: $FRONT_RESULT"

# ---------------- STEP 5: smoke via nginx ----------------
log "STEP 5/6  SMOKE VIA NGINX"
P1=$(curl -s -o /dev/null -m 5 -w '%{http_code}' -H 'Host: bsdone.com' http://127.0.0.1/)
P2=$(curl -sk -o /dev/null -m 5 -w '%{http_code}' -H 'Host: bsdone.com' https://127.0.0.1/)
P3=$(curl -s -m 5 -H 'Host: bsdone.com' http://127.0.0.1/bsball-server/health)
echo "prod  http  -> $P1 (expect 301)"
echo "prod  https -> $P2 (expect 200)"
echo "prod  api   /health -> $P3"

# ---------------- STEP 6: summary ----------------
log "STEP 6/6  SUMMARY"
systemctl show java-server -p MainPID -p NRestarts -p ActiveEnterTimestamp
echo
echo "===== PROD DEPLOY COMPLETE ====="
echo "deployed jar md5 : $M2"
echo "frontend         : $FRONT_RESULT"
echo "backup           : $BAK_DIR"
echo "manual rollback (only if ever needed):"
echo "  systemctl stop java-server"
echo "  cp -p $BAK_DIR/bsball-server.jar.bak $APP_DIR/bsball-server.jar"
echo "  systemctl start java-server"
