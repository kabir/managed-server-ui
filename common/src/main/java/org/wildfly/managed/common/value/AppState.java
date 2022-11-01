package org.wildfly.managed.common.value;

import java.io.Serializable;

/**
 * The status of the app on OpenShift
 */
public class AppState implements Serializable {

    private final DeploymentState deploymentState;
    private final BuildState buildState;

    public AppState(DeploymentState deploymentState, BuildState buildState) {
        this.deploymentState = deploymentState;
        this.buildState = buildState;
    }

    public DeploymentState getDeploymentState() {
        return deploymentState;
    }

    public BuildState getBuildState() {
        return buildState;
    }

    public enum DeploymentState {
        NOT_DEPLOYED,
        RUNNING,
        DEPLOYING
    }

    public enum BuildState {
        RUNNING,
        COMPLETED,
        FAILED
    }
}
