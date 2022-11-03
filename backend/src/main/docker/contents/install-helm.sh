#!/bin/sh


appName="${1}"
helmChart="${2}"

chart_dir=$(dirname "${BASH_SOURCE[0]}")

helm list --filter "${appName}$" | grep -q "${appName}" && found=1 || found=0

if [ ${found} -eq 0 ]; then
  # TODO populated mode is currently broken
  # TODO helm repo add rather than hardcoding the path to the tar?
  #helm install ${appName} \
  # /Users/kabir/sourcecontrol/wildfly/managed-wildfly-chart/managed-wildfly-chart-0.1.0.tgz \
  #  --set builder.mode=populated
  helm install ${appName} \
   ${chart_dir}/managed-wildfly-chart-0.1.0.tgz
fi


