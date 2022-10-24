package org.wildfly.managed.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import org.wildfly.managed.ConfigFileInspection;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
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
        System.out.println("===> Loaded " + application.name);
        for (AppArchive appArchive : application.appArchives) {
            System.out.println(appArchive.fileName);
        }

        return application;
    }



    @Transactional
    public void delete(String name) {
        delete("name", name);
    }

    @Transactional
    public List<AppArchive> listArchivesForApp(String name) {
        Application application = find("name", name).firstResult();
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
        // TODO Didn't there use to be something like attach?
        application = findByName(application.name);
        if (application == null) {
            throw new IllegalStateException("Could not find application " + application.name);
        }

        AppArchive appArchive = new AppArchive();
        appArchive.application = application;
        appArchive.fileName = fileName;
        appArchive.serverConfigXml = configFileInspection.isServerConfigXml();
        appArchive.serverInitCli = configFileInspection.isServerInitCli();
        appArchive.serverInitYml = configFileInspection.isServerInitYml();

        Collection<AppArchive> appArchives = application.appArchives;
        if (appArchives.contains(appArchive)) {
            throw new IllegalStateException("Archive " + appArchive.fileName + " already exists");
        }
        checkNoDuplicateConfigFiles(application, appArchive);
        appArchives.add(appArchive);
        application.appArchives = appArchives;

        appArchive.persist();
    }

    @Transactional
    public void updateApplicationArchive(Application application, String fileName, ConfigFileInspection configFileInspection) {
        System.out.println("Updating " + fileName);
        application = findByName(application.name);
        if (application == null) {
            throw new IllegalStateException("Could not find application " + application.name);
        }

        AppArchive found = findByApplicationAndName(application, fileName);
        if (found == null) {
            // TODO if not working we should undo the file copy in the caller
            throw new IllegalStateException("No existing application called " + fileName);
        } else {
            found.serverConfigXml = configFileInspection.isServerConfigXml();
            found.serverInitCli = configFileInspection.isServerInitCli();
            found.serverInitYml = configFileInspection.isServerInitYml();
        }
        checkNoDuplicateConfigFiles(application, found);
        System.out.println("--- done");
    }

    private void checkNoDuplicateConfigFiles(Application application, AppArchive archive) {
        if (!archive.serverConfigXml && !archive.serverInitCli && !archive.serverInitYml) {
            return;
        }
        for (AppArchive curr : application.appArchives) {
            if (!curr.equals(archive)) {
                if (archive.serverConfigXml && curr.serverConfigXml) {
                    throw new IllegalStateException("Only one application can contain a server-config.xml");
                }
                if (archive.serverInitCli && curr.serverInitCli) {
                    throw new IllegalStateException("Only one application can contain a server-init.cli");
                }
                if (archive.serverInitYml && curr.serverInitYml) {
                    throw new IllegalStateException("Only one application can contain a server-init.yml");
                }
            }
        }
    }

    @Transactional
    public void deleteApplicationArchive(Application application, String fileName) {
        application = findByName(application.name);
        if (application == null) {
            throw new IllegalStateException("Could not find application " + application.name);
        }

        AppArchive appArchive = findByApplicationAndName(application, fileName);
        if (appArchive != null) {
            application.appArchives.remove(appArchive);
            appArchive.delete();
            appArchive.application = null;
        } else {
            throw new IllegalStateException("No existing archive called " + fileName);
        }
    }

    @Transactional
    public String getConfigFileContents(String appName, String type) {
        Application application = findByName(appName);
        if (application == null) {
            throw new IllegalStateException("Could not find application " + appName);
        }

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
    public void setConfigFileContents(String appName, String type, String contents) {
        Application application = findByName(appName);
        if (application == null) {
            throw new IllegalStateException("Could not find application " + appName);
        }

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
        // These don't make a difference
        //getEntityManager().merge(application);
        //getEntityManager().flush();
    }

    @Transactional
    public void deleteConfigFileContents(String appName, String type) {
        Application application = findByName(appName);
        if (application == null) {
            throw new IllegalStateException("Could not find application " + appName);
        }

        System.out.println("Deleting Config: ");
        if (type.equals("xml")) {
            System.out.println("xml!");
            application.serverConfigXml = null;
            application.hasServerConfigXml = false;
        } else if (type.equals("cli")) {
            System.out.println("cli!");
            application.serverInitCli = null;
            application.hasServerInitCli =false;
        } else if (type.equals("yml")) {
            System.out.println("yml!");
            application.serverInitYml = null;
            application.hasServerInitYml = false;
        }
    }

    // TODO Get this working and use this rather than all the iterating I am doing
    private AppArchive findByApplicationAndName(Application application, String name) {
        AppArchive appArchive = AppArchive.find("application=:application AND fileName=:name",
                Parameters
                        .with("application", application)
                        .and("name", name)).firstResult();
        return appArchive;
    }
}
