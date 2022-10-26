#!/bin/sh

appName="${1}"
appFolder="${3}"

oc start-build ${appName}-update-build  --from-file=${appFolder}/ROOT.war

