# Managed Server 

**NB!!** As the project is very much a WIP/POC, these instructions are a bit rough around the edges, assuming the
audience are familiar with a lot of the concepts.

Proof of concept for what running WildFly as a managed service could look like. The simple idea is that the user will
upload their war files, and then end up with those applications running on containers in OpenShift that have been
provisioned with appropriate WildFly instances.

This project implements a backend service to manage the applications, and a CLI to perform those tasks remotely.

It builds on top of the following:
* Additions to [wildfly-cekit-modules](https://github.com/kabir/wildfly-cekit-modules/tree/managed-server-poc) and 
a new [wildfly-s2i](https://github.com/kabir/wildfly-s2i/tree/managed-server-poc/wildfly-managed-server-image) managed 
server image. The image can be found in my quay.io [repository](https://quay.io/repository/kabirk/wildfly-managed-server-jdk17). 
* A Helm chart which can be found at https://github.com/kabir/managed-wildfly-chart, used to build the user applications.
This chart uses the wildfly-managed-server-image mentioned above.
* Some example deployments at https://github.com/kabir/managed-wildfly-deployments. These contain some additional config
which is currently needed to deploy an application on a managed wildfly instance.

The architecture of the application:
* An OpenShift instance used to host the user applications, and perform the needed OpenShift builds to make that happen.
* A Postgres database to keep track of the applications and what war files have been deployed
* A backend service exposing REST endpoints to interact with OpenShift and the Postgres database
* The CLI for use by users to manage their applications


## Running the application

Whether running locally or on an OpenShift instance, we will need to perform a few steps first.

* Clone and build
** The examples at https://github.com/kabir/managed-wildfly-deployments. We will need those as input when
trying out the CLI. `$EXAMPLE_DIR` will be used to indicate the location of the root folder of your checkout folder.
** The Helm chart at https://github.com/kabir/managed-wildfly-deployments `$HELM_DIR` will be used to indicate
  the location of the root folder of your checkout folder.

Next we will need to build the contents of this repository. It is just a normal Maven build

```shell
mvn clean install -DskipTests
```

If `-DskipTests` is omitted you will need to have a Postgres instance running. We don't have much in the way of tests
at the moment, so you lose nothing by skipping them :-)

For local development, it is easier to have Postgres and the backend running locally. In this case we still need to
interact a bit with OpenShift when we deploy and remove the applications. When we deploy, it will install the Helm chart,
and trigger the builds leading to a WildFly server getting provisioned, and eventually hosting our application.

Once you have made changes and want to try them out on OpenShift properly you will need to build a Docker image and deploy
that on OpenShift. Alternatively, you may just want to try the latest 


### Running locally
 
Start Postgres in a docker container
```shell
docker run --rm -it \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=managed-server-db \
  -p 5432:5432 \
  postgres:latest
```

Build the project with `mvn clean install`.

Next log in to your OpenShift instance using `oc login` and the credentials you get when selecting 'Copy login command'
from the OpenShift console. If you are using an OpenShift instance with several projects, make sure to 
choose the correct project with `oc project <project-name>`.

Now you are ready to start the server. Go to the `backend/` folder and start the server with 
```
mvn quarkus:dev -Dmanaged.server.helm.chart.location=$HELM_DIR/managed-wildfly-chart-0.1.0.tgz
```
You should now be able to manage applications via the CLI, as covered later.

### Running on OpenShift
Please see the [helm/README.md](./helm/README.md) for how to install this on your
OpenShift instance, and also for instructions on how to modify the application and run it on OpenShift with your changes.

## CLI command examples

### Deploying and undeploying application
To run the CLI, go into the `cli/` directory of the project and run `java -jar target/quarkus-app/quarkus-run.jar`.
Once things are more ready this will be packaged as a native application.

The plain command above will present you with the help:
```shell
% java -jar target/quarkus-app/quarkus-run.jar           
Missing required subcommand
Usage: ms-cli [-hV] [COMMAND]
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  app     Application commands
  server  Operations for the backend server used to manage applications
```
To get more information about a command you can specify that, e.g.:
```shell
% java -jar target/quarkus-app/quarkus-run.jar app
Missing required subcommand
Usage: ms-cli app [-hV] [COMMAND]
Application commands
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  use      Sets the active application
  get      Gets information about an application
  create   Creates a new application
  delete   Deletes an application
  list     Lists all applications
  deploy   Deploy the application
  stop     Stops an application
  status   Gets the application status
  archive  Application archive commands
  config   Application config commands
```

Now let's look at how we can connect to the backend, and deploy an application.

First we need to tell the CLI which server to connect to.
```shell
% java -jar target/quarkus-app/quarkus-run.jar server set http://localhost:8080
Setting server to: http://localhost:8080
```

In this case I am using a locally running backend. If I was connecting to a backend running on OpenShift, I would have
needed the URL of the OpenShift instance's route called `managed-server-ui-backend`.

Now we can create an application called 'one'.
```shell
% java -jar target/quarkus-app/quarkus-run.jar app create one                  
Application one created and set as the active application.
```

An application is just an empty wrapper around other resources, so let's add a deployment to it.
```shell
% java -jar target/quarkus-app/quarkus-run.jar app archive add $EXAMPLE_DIR/simple/target/ROOT.war
Adding ROOT.war to application one
Uploading ROOT.war
```
Note that I am adding a war file from the example project. In this case I am doing this because it contains the Galleon
layers we need to provision for the server in its `META-INF/server-config.xml` file. This file is required at the moment,
but will possibly be generated in the future. If you don't want to bundle this file in the war, you can also add it
with the `java -jar target/quarkus-app/quarkus-run.jar app config set xml /path/to/server-config.xml` command. If it 
appears in both places, the one external to the war will take precedence. Similarly, the war also contains
`META-INF/server-init.cli` and `META-INF/server-init.yml` which can also be externalised/overridden via the CLI.

At this stage we could also add more war files to our application. They are simply stored on the backend server handling
the CLI commands. 

Now, let's actually deploy our application:
```shell
% java -jar target/quarkus-app/quarkus-run.jar app deploy                                                             
Deploying application...
Application deployment registered. Monitor the status with 'app status'
```
If you look in the OpenShift console now you will see a lot of build configs and imagestreams prefixed with `one-`. 
These pertain to our application 'one'. Once all relevant builds are done, our application is deployed.

A user would monitor this with the 'app status' command mentioned in the output of the previous command, e.g:
```shell
% java -jar target/quarkus-app/quarkus-run.jar app status                                                             
Deployment: RUNNING
Build: RUNNING
```
Once `Deployment: RUNNING` is reported, the user's application is accessible.

We can remove the application by running the following

```shell
% java -jar target/quarkus-app/quarkus-run.jar app delete
Deleting application 'one'...
ERROR: Can't delete a running application, or one in the process of being built. Stop it or force delete

% java -jar target/quarkus-app/quarkus-run.jar app delete -f
Deleting application 'one'...
ERROR: Can't delete a running application, or one in the process of being built. Stop it first, or force delete.
```
We had to pass in the `-f` flag to force deletion. This would be similar to running 
`java -jar target/quarkus-app/quarkus-run.jar app stop` followed by 
`java -jar target/quarkus-app/quarkus-run.jar app delete` (no `-f` needed now).

### Active application and --name
A lot of the application commands work on an application. 

As we saw above, when you create an application it becomes set as the active application.

```shell
% java -jar target/quarkus-app/quarkus-run.jar app create one                  
Application one created and set as the active application.

% java -jar target/quarkus-app/quarkus-run.jar app create two                  
Application two created and set as the active application.
```
Let's set 'one' as the active application, and add some contents and deploy to it as we saw above:
```shell
% java -jar target/quarkus-app/quarkus-run.jar app use one
Application one set as the active application.

% java -jar target/quarkus-app/quarkus-run.jar app archive add $EXAMPLE_DIR/simple/target/ROOT.war
Adding ROOT.war to application one
Uploading ROOT.war

% java -jar target/quarkus-app/quarkus-run.jar app deploy                                                             
Deploying application...
Application deployment registered. Monitor the status with 'app status'
```
The commands after `app use one` all work on application 'one' since it is the active application.

However, all commands where it makes sense to specify another app allow you to do so via the `--name' flag. E.g.
```shell
# one is the active app so its status will be reported
% java -jar target/quarkus-app/quarkus-run.jar app status                                                         
Deployment: RUNNING
Build: COMPLETED

# Explicitly say we want one
% java -jar target/quarkus-app/quarkus-run.jar app status --name one
Deployment: RUNNING
Build: COMPLETED

# Get two's status instead
% java -jar target/quarkus-app/quarkus-run.jar app status --name two
Deployment: NOT_DEPLOYED
Build: NOT_RUNNING
```

The active application can be got with `app get`
```shell
% java -jar target/quarkus-app/quarkus-run.jar app get              
one
```

### More application commands

We can list all the applications with an overview of their status
```shell
% java -jar target/quarkus-app/quarkus-run.jar app list
Application                    Deployment      Build          
------------------------------ --------------- ---------------
* one                          RUNNING         COMPLETED      
two                            NOT_DEPLOYED    NOT_RUNNING    
```
The output indicates that 'one' is the active application.

We can get more information about the active application with `app get -v` (or another application if we also use the `--name` flag):
```shell
% java -jar target/quarkus-app/quarkus-run.jar app get -v           
Name: one

Status
------
Deployment: RUNNING
Build: COMPLETED

Configs
-------
XML: 
CLI: 
YML: 

Routes
------
one-kkhan1-dev.apps.sandbox.x8i5.p1.openshiftapps.com

Archives:
Archive                        Has XML Has CLI Has YAML
------------------------------ ------- ------- --------
ROOT.war                       *       *       *       

```

To update an archive in an application we can use `app archive replace`:
```shell
% java -jar target/quarkus-app/quarkus-run.jar app archive replace $EXAMPLE_DIR/simple/target/ROOT.war
Uploading ROOT.war to application one for replacement...
Upload done
```
This will not affect the application running on OpenShift until `app deploy` is called again.


