#!/bin/sh
# Turbo Core ICE BREAKER Protocol
# Emergency cooling for low-end Samsung A-series

echo "Stop Compile..."
cmd package compile --reset -a

echo "Log Nuke..."
setprop ctl.stop logd

echo "Sync Freeze..."
content stop-sync
settings put global activity_manager_constants background_settle_time=0

echo "Kill Bloat..."
am force-stop com.samsung.android.game.gos
am force-stop com.samsung.android.bixby.agent
am force-stop com.samsung.android.bbc.bbcagent
am force-stop com.sec.android.app.samsungapps

echo "Reset GMS..."
am force-stop com.google.android.gms

echo "Cancel Dexopt Jobs..."
cmd package bg-dexopt-job --cancel

echo "Cool down initiated."
