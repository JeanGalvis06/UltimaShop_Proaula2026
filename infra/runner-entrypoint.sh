#!/bin/sh
set -eu
if [ ! -f /data/.runner ]; then
  act_runner register --no-interactive --instance "$GITEA_INSTANCE_URL" --token "$GITEA_RUNNER_REGISTRATION_TOKEN" --name ultimashop-local --labels ultimashop:host
fi
unset GITEA_RUNNER_REGISTRATION_TOKEN
exec act_runner daemon
