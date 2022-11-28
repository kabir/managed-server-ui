package org.wildfly.managed.scheduled;

import io.quarkus.scheduler.Scheduled;
import org.wildfly.managed.common.model.DeploymentRecord;
import org.wildfly.managed.common.value.AppState;
import org.wildfly.managed.openshift.OpenshiftFacade;
import org.wildfly.managed.repo.ApplicationRepo;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class DeploymentCompleteReporter {

    @Inject
    ApplicationRepo applicationRepo;

    @Inject
    OpenshiftFacade openshiftFacade;


    @Scheduled(every = "10s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void unlockCompletedDeployments() {
        System.out.println("Unlocking completed deployments");
        try {
            List<DeploymentRecord> records = applicationRepo.getAllRunningDeployments();
            for (DeploymentRecord record : records) {
                if (!record.buildTriggered) {
                    continue;
                }
                AppState.BuildState buildState = openshiftFacade.getBuildState(record.application.name);
                System.out.println("State for " + record.application.name + " " + buildState);
                if (buildState.isDone()) {
                    DeploymentRecord.Status status = buildState == AppState.BuildState.COMPLETED ? DeploymentRecord.Status.COMPLETED : DeploymentRecord.Status.FAILED;
                    applicationRepo.recordDeploymentEnd(record.application.name, status);
                }
            }
        } catch (Exception e) {
            //TODO observe the FailedExecution CDI event mentioned in https://quarkus.io/guides/scheduler-reference#scheduled-methods?
            e.printStackTrace();
        }
    }
}
