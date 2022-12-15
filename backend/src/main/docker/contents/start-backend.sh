#!/bin/sh


# Log in to OpenShift before we start the java app to make sure the credentials are picked up
# TODO Check https://developers.redhat.com/blog/2020/05/20/getting-started-with-the-fabric8-kubernetes-java-client#using_fabric8_with_kubernetes
#  to do this programmatically once things settle a bit


# The env var stored in the secret has hyphens which don't work so well (at least I have no clue how to use them normally)
# so 'translate' to a more common format
MANAGED_SERVER_SECRET_TOKEN="$(printenv managed-server-secret-openshiftToken)"

if [ -z "${MANAGED_SERVER_OPENSHIFT_SERVER}" ]; then
  echo "MANAGED_SERVER_OPENSHIFT_SERVER should point to the server. Exiting."
  exit 1
fi
if [ -z "${MANAGED_SERVER_SECRET_TOKEN}" ]; then
  echo "The secret managed-server-secret should exist and contain a token in the field openshiftToken. Exiting."
  exit 1
fi
if [ -z "${MANAGED_SERVER_OPENSHIFT_PROJECT}" ]; then
  echo "MANAGED_SERVER_OPENSHIFT_PROJECT should contain the name of the OpenShift project. Exiting."
  exit 1
fi

echo "Logging in with oc"

oc login --token="${MANAGED_SERVER_SECRET_TOKEN}" --server="${MANAGED_SERVER_OPENSHIFT_SERVER}"
echo "Logged in $?"
oc whoami
echo "Switching to ${MANAGED_SERVER_OPENSHIFT_PROJECT}"
oc project ${MANAGED_SERVER_OPENSHIFT_PROJECT}

# Run the script from the registry.access.redhat.com/ubi8/openjdk-11 image
/usr/local/s2i/run
