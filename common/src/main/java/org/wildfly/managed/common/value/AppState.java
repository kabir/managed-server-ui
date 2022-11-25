package org.wildfly.managed.common.value;

import java.io.Serializable;

/**
 * The status of the app on OpenShift
 */
public class AppState implements Serializable {

    private final DeploymentState deploymentState;
    private final BuildState buildState;
    private StageState stageState;

    public AppState(DeploymentState deploymentState, BuildState buildState, StageState stageState) {
        this.deploymentState = deploymentState;
        this.buildState = buildState;
        this.stageState = stageState;
    }

    public DeploymentState getDeploymentState() {
        return deploymentState;
    }

    public BuildState getBuildState() {
        return buildState;
    }

    public StageState getStageState() {
        return stageState;
    }

    // Whether the application is deployed or not
    public enum DeploymentState {
        NOT_DEPLOYED,
        RUNNING,
        DEPLOYING
    }

    // Whether a build is in progress
    public enum BuildState {
        NOT_RUNNING(false),
        RUNNING(false),
        COMPLETED(true),
        FAILED(true);

        private final boolean done;

        BuildState(boolean done) {
            this.done = done;
        }

        public boolean isDone() {
            return done;
        }
    }


    // Whether the files on the server are newer than the ones in a deployed application
    public enum StageState {
        STAGED_CHANGES,
        UP_TO_DATE
    }
}
