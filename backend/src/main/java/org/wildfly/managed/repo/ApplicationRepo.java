package org.wildfly.managed.repo;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import org.wildfly.managed.ConfigFileInspection;
import org.wildfly.managed.ServerException;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.common.model.DeploymentRecord;
import org.wildfly.managed.common.value.AppState;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.core.Response;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class ApplicationRepo implements PanacheRepository<Application> {

    @Transactional
    public Application create(Application application) {
        persist(application);
        return application;
    }

    @Transactional
    public Application findByName(String name) {
        Application application = find("name", name).firstResult();
        if (application == null) {
            throw new ServerException(Response.Status.NOT_FOUND, "No application called: " + name);
        }
        return application;
    }

    @Transactional
    public Application getApplication(String name, boolean verbose) {
        Application application = findByName(name);
        if (verbose) {
            application.loadConfigFields();
        }
        getEntityManager().detach(application);

        if (!verbose) {
            application.appArchives = Collections.emptyList();
            application.serverConfigXml = null;
            application.serverInitCli = null;
            application.serverInitYml = null;
        }
        return application;
    }

    @Transactional
    public void delete(String name) {
        // For validation, will throw an error if not found
        Application app = findByName(name);

        // We don't need to do this here, since if we reach this point we have removed it on OpenShift
        //checkCanModifyApplication(app);

        for (AppArchive archive : app.appArchives) {
            archive.application = null;
            archive.delete();
        }
        for (DeploymentRecord deploymentRecord : app.deploymentRecords) {
            deploymentRecord.application = null;
            deploymentRecord.delete();
        }
        delete("name", name);
    }

    @Transactional
    public List<AppArchive> listArchivesForApp(String name) {
        Application application = findByName(name);
        Collection<AppArchive> archives = application.appArchives;
        List<AppArchive> appArchives = new ArrayList<>(archives);
        appArchives.sort(new Comparator<AppArchive>() {
            @Override
            public int compare(AppArchive o1, AppArchive o2) {
                return o1.fileName.compareTo(o2.fileName);
            }
        });
        return appArchives;
    }


    @Transactional
    public void createApplicationArchive(Application application, String fileName, ConfigFileInspection configFileInspection) {
        application = findByName(application.name);

        checkCanModifyApplication(application);

        AppArchive appArchive = new AppArchive();
        appArchive.application = application;
        appArchive.fileName = fileName;
        appArchive.serverConfigXml = configFileInspection.isServerConfigXml();
        appArchive.serverInitCli = configFileInspection.isServerInitCli();
        appArchive.serverInitYml = configFileInspection.isServerInitYml();

        Collection<AppArchive> appArchives = application.appArchives;
        if (appArchives.contains(appArchive)) {
            throw new ServerException(Response.Status.CONFLICT,
                    "There is already an archive called '" + appArchive.fileName + " associated with '" + application.name + "'");
        }
        checkNoDuplicateConfigFiles(application, appArchive);
        appArchives.add(appArchive);
        application.appArchives = appArchives;
        application.lastArchiveChange = LocalDateTime.now();

        appArchive.persist();
    }

    @Transactional
    public void updateApplicationArchive(Application application, String fileName, ConfigFileInspection configFileInspection) {
        System.out.println("Updating " + fileName);
        application = findByName(application.name);

        checkCanModifyApplication(application);

        AppArchive found = findByApplicationAndName(application, fileName);
        if (found == null) {
            // TODO if not working we should undo the file copy in the caller
            throw new ServerException(Response.Status.NOT_FOUND, "No existing application called " + fileName);
        } else {
            found.serverConfigXml = configFileInspection.isServerConfigXml();
            found.serverInitCli = configFileInspection.isServerInitCli();
            found.serverInitYml = configFileInspection.isServerInitYml();
        }
        checkNoDuplicateConfigFiles(application, found);
        application.lastArchiveChange = LocalDateTime.now();
        System.out.println("--- done");
    }

    private void checkNoDuplicateConfigFiles(Application application, AppArchive archive) {
        if (!archive.serverConfigXml && !archive.serverInitCli && !archive.serverInitYml) {
            return;
        }
        for (AppArchive curr : application.appArchives) {
            if (!curr.equals(archive)) {
                if (archive.serverConfigXml && curr.serverConfigXml) {
                    throw new ServerException(Response.Status.CONFLICT, "Only one application can contain a server-config.xml");
                }
                if (archive.serverInitCli && curr.serverInitCli) {
                    throw new ServerException(Response.Status.CONFLICT, "Only one application can contain a server-init.cli");
                }
                if (archive.serverInitYml && curr.serverInitYml) {
                    throw new ServerException(Response.Status.CONFLICT, "Only one application can contain a server-init.yml");
                }
            }
        }
    }

    @Transactional
    public void deleteApplicationArchive(Application application, String fileName) {
        application = findByName(application.name);
        checkCanModifyApplication(application);
        AppArchive appArchive = findByApplicationAndName(application, fileName);
        if (appArchive != null) {
            application.appArchives.remove(appArchive);
            appArchive.delete();
            appArchive.application = null;
            application.lastArchiveChange = LocalDateTime.now();
        } else {
            throw new ServerException(Response.Status.NOT_FOUND, "No existing archive called " + fileName);
        }
    }

    @Transactional
    public String getConfigFileContents(String appName, String type) {
        Application application = findByName(appName);

        String config = null;
        if (type.equals("xml")) {
            System.out.println("read xml!");
            config = application.serverConfigXml;
        } else if (type.equals("cli")) {
            System.out.println("read cli!");
            config = application.serverInitCli;
        } else if (type.equals("yml")) {
            System.out.println("read yml!");
            config = application.serverInitYml;
        }
        System.out.println("---> config");
        return config;
    }

    @Transactional
    public ApplicationConfigs getConfigFileContents(String appName) {
        Application application = findByName(appName);
        return new ApplicationConfigs(
                application.serverConfigXml,
                application.serverInitYml,
                application.serverInitCli);
    }

    @Transactional
    public void setConfigFileContents(String appName, String type, String contents) {
        Application application = findByName(appName);

        checkCanModifyApplication(application);

        System.out.println("Setting Contents: " + contents);
        if (type.equals("xml")) {
            System.out.println("xml!");
            application.serverConfigXml = contents;
            application.hasServerConfigXml = contents != null;
        } else if (type.equals("cli")) {
            System.out.println("cli!");
            application.serverInitCli = contents;
            application.hasServerInitCli = contents != null;
        } else if (type.equals("yml")) {
            System.out.println("yml!");
            application.serverInitYml = contents;
            application.hasServerInitYml = contents != null;
        }
        application.lastConfigChange = LocalDateTime.now();
    }

    @Transactional
    public void deleteConfigFileContents(String appName, String type) {
        Application application = findByName(appName);

        checkCanModifyApplication(application);

        System.out.println("Deleting Config: ");
        if (type.equals("xml")) {
            System.out.println("xml!");
            // Lazy load field to make clearing it take effect
            String tmp = application.serverConfigXml;
            application.serverConfigXml = null;
            application.hasServerConfigXml = false;
        } else if (type.equals("cli")) {
            System.out.println("cli!");
            String tmp = application.serverInitCli;
            application.serverInitCli = null;
            application.hasServerInitCli = false;
        } else if (type.equals("yml")) {
            System.out.println("yml!");
            String tmp = application.serverInitYml;
            application.serverInitYml = null;
            application.hasServerInitYml = false;
        }
        application.lastConfigChange = LocalDateTime.now();
    }

    private AppArchive findByApplicationAndName(Application application, String name) {
        AppArchive appArchive = AppArchive.find(
                "application=:application AND fileName=:name",
                Parameters
                        .with("application", application)
                        .and("name", name)).firstResult();
        return appArchive;
    }

    @Transactional
    public void recordDeploymentStart(String appName) {
        Application application = findByName(appName);
        DeploymentRecord deploymentRecord = new DeploymentRecord();
        deploymentRecord.startTime = LocalDateTime.now();
        application.deploymentRecords.add(deploymentRecord);
        deploymentRecord.application = application;
        deploymentRecord.persist();
    }


    @Transactional
    public void recordDeploymentEnd(String appName, DeploymentRecord.Status status) {
        DeploymentRecord record = getRunningDeployment(appName);
        if (record != null) {
            record.endTime = LocalDateTime.now();
            record.status = status;
        }
    }

    @Transactional
    public DeploymentRecord getRunningDeployment(String appName) {
        Application application;
        try {
            application = findByName(appName);
        } catch (Exception e) {
            return null;
        }
        return getRunningDeployment(application);
    }

    private DeploymentRecord getRunningDeployment(Application application) {
        DeploymentRecord record = null;
        try {
            record = DeploymentRecord.find(
                    "application=:application AND endTime IS NULL",
                    Parameters
                            .with("application", application)).firstResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return record;
    }

    private void checkCanModifyApplication(Application application) {
        DeploymentRecord record = getRunningDeployment(application);
        if (record != null) {
            throw new ServerException(
                    Response.Status.CONFLICT,
                    "The application is currently being deployed. While that is happening you cannot modify the application. " +
                            "Cancel the deploy in order to be able to modify the application.");
        }
    }

    @Transactional
    public List<DeploymentRecord> getAllRunningDeployments() {
        return DeploymentRecord.find("endTime IS NULL").list();
    }

    public AppState.StageState getStageStatus(String appName) {
        Application application = findByName(appName);

        // Find last successful deployment
        PanacheQuery<DeploymentRecord> query = DeploymentRecord.find(
                "application=:application AND status=:status ORDER BY startTime DESC",
                Parameters
                        .with("application", application)
                        .and("status", DeploymentRecord.Status.COMPLETED)
                );

        PanacheQuery<DeploymentRecord> query2 = DeploymentRecord.find(
                "application=:application AND status=:status ORDER BY startTime DESC",
                Parameters
                        .with("application", application)
                        .and("status", DeploymentRecord.Status.FAILED)
        );

        PanacheQuery<DeploymentRecord> query3 = DeploymentRecord.find(
                "application=:application AND status=:status ORDER BY startTime DESC",
                Parameters
                        .with("application", application)
                        .and("status", DeploymentRecord.Status.CANCELLED)
        );
        System.out.println(query.count());
        System.out.println(query2.count());
        System.out.println(query3.count());

        if (query.count() > 0) {
            LocalDateTime lastTime = query.firstResult().startTime;
            if (lastTime.isBefore(application.lastArchiveChange) || lastTime.isBefore(application.lastConfigChange)) {
                return AppState.StageState.STAGED_CHANGES;
            }
        }
        return AppState.StageState.UP_TO_DATE;
    }
}
