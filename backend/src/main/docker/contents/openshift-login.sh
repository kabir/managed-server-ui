#!/bin/sh


openshiftServer="${1}"
openshiftToken="${2}"

echo "Logging in with oc"
oc login --token=${openshiftToken} --server=${openshiftServer}
echo "Logged in $?"
oc whoami


