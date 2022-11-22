# Helm chart to deploy on OpenShift

You might want to simply try the latest WIP from this repository, or you might want to try our your own fixes there.

In both cases you will need a Postgres instance running on OpenShift. To install Postgres, go to your OpenShift console, and select the 'Developer' perspective, and select 'Add' from the
list on the right. Go to 'Database' and select 'PostgreSQL'. Keep the defaults for everything and install it.

The latest image of the backend is deployed at https://quay.io/repository/kabirk/managed-server-ui-backend-jvm.

We will install this using Helm. First we need to gather some data, the values are

* `TOKEN` - the token to log in to the OpenShift instance. The backend needs this since it is interacting with the OpenShift APIs. This can be obtained from the output of the `Copy login command` in the OpenShift console.
* `SERVER` - the server as output when figuring out the token
* `PROJECT` - the name of the OpenShift project to install the back-end into

Then simply run a `helm install` from this directory:
```shell
helm install managed-server-backend managed-server-backend-0.1.0.tgz \
  --set backend.openshift.server="${SERVER}" \
  --set backend.openshift.token="${TOKEN}" \
  --set backend.openshift.project="${PROJECT}"
```
-----
**Note:** On some environments like the Red Hat Developer Sandbox, the token expires regularly so you will have to refresh it in the configuration. If you don't mind starting from scratch, the easiest way is to simply run `helm uninstall managed-server-backend` and then do the above steps again.

-------

Once installed, find the URL used for the application, e.g:

```shell

% oc get route managed-server-ui-backend                          
NAME                        HOST/PORT             PATH   SERVICES                    PORT    TERMINATION     WILDCARD
managed-server-ui-backend   this.is.the.address          managed-server-ui-backend   <all>   edge/Redirect   None
```
And then set the server before running the CLI commands, e.g.:

```shell
% java -jar target/quarkus-app/quarkus-run.jar server set https://this.is.the.address
Setting server to: http://this.is.the.address:8080
```

#### Trying your own changes on OpenShift

If you wish to make changes to the backend application, and deploy on OpenShift, we need to perform a few more steps. There is a GitHub action that performs a lot of these steps too. Once forked to your repository, you can trigger the action manually, modify it to point to your own docker registry.


First, build the application from the root checkout folder, and then rebuild the CLI in uber-jar mode:
```shell
mvn install -DskipTests
mvn install -pl cli -Dquarkus.package.type=uber-jar -DskipTests
```

Copy the Helm chart we packaged earlier to where the docker file expects to find it. **Note:** This is NOT the Helm chart contained in this folder, rather the one from https://github.com/kabir/managed-wildfly-chart!
```shell
mkdir backend/target/docker
cp $HELM_DIR/managed-wildfly-chart-*.tgz backend/target/docker
```
Do the same for the built CLI jar
```shell
cp cli/target/*.jar backend/target/docker/managed-server-runner.jar
```

Once built we need to build the docker image. In this case I am simply replacing the URL of the image tag to
be your repository on Quay.io.
```shell
docker build -f backend/src/main/docker/Dockerfile.jvm -t quay.io/<your repo>/managed-server-ui-backend-jvm backend
docker push quay.io/<your repo>/managed-server-ui-backend-jvm
```
Once pushed you should be able to install the Helm chart as above, but you will need to 
append `--set backend.image=quay.io/<your repo>/managed-server-ui-backend-jvm` to the
arguments passed in to the `helm install` command for it to use your image.
