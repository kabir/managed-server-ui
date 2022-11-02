#!/bin/sh


appName="${1}"

helm list --filter "${appName}$" | grep -q "${appName}" && found=1 || found=0

if [ ${found} -eq 1 ]; then
  helm uninstall ${appName}
fi


