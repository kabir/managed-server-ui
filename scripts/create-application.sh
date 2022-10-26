#!/bin/sh


appName="${1}"
helmChart="${2}"
appFolder="${3}"

# TODO helm install if we need a helm chart

# TODO Don't hardcode the path to the chart, rather it should be installed with `helm repo add`
# populated mode is currently broken
#helm install ${appName} \
# /Users/kabir/sourcecontrol/wildfly/managed-wildfly-chart/managed-wildfly-chart-0.1.0.tgz \
#  --set builder.mode=populated

helm install ${appName} \
 /Users/kabir/sourcecontrol/wildfly/managed-wildfly-chart/managed-wildfly-chart-0.1.0.tgz

oc start-build ${appName}-deployment-build  --from-file=${appFolder}/ROOT.war



#!/bin/sh


appName="${1}"
helmChart="${2}"
appFolder="${3}"

# TODO helm install if we need a helm chart

helm install ${appName} \
 /Users/kabir/sourcecontrol/wildfly/managed-wildfly-chart/managed-wildfly-chart-0.1.0.tgz

oc start-build ${appName}-deployment-build  --from-file=${appFolder}/ROOT.war
