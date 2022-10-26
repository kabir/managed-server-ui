package org.wildfly.managed.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import org.wildfly.managed.ConfigFileInspection;
import org.wildfly.managed.ServerException;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.core.Response;
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
        if (application == null) {
            throw new ServerException(Response.Status.NOT_FOUND, "No application called: " + name);
        }
        return application;
    }


    @Transactional
    public void delete(String name) {
        // For validation, will throw an error if not found
        findByName(name);

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

        appArchive.persist();
    }

    @Transactional
    public void updateApplicationArchive(Application application, String fileName, ConfigFileInspection configFileInspection) {
        System.out.println("Updating " + fileName);
        application = findByName(application.name);

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
        AppArchive appArchive = findByApplicationAndName(application, fileName);
        if (appArchive != null) {
            application.appArchives.remove(appArchive);
            appArchive.delete();
            appArchive.application = null;
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
    public void setConfigFileContents(String appName, String type, String contents) {
        Application application = findByName(appName);

        System.out.println("Setting Contents: " + contents);
        if (type.equals("xml")) {
            System.out.println("xml!");
            // Fake read to load the lazy field to be able update it
            String temp = application.serverConfigXml;

            application.serverConfigXml = contents;
            application.hasServerConfigXml = contents != null;
        } else if (type.equals("cli")) {
            System.out.println("cli!");
            // Fake read to load the lazy field to be able update it
            String temp = application.serverInitCli;

            application.serverInitCli = contents;
            application.hasServerInitCli = contents != null;
        } else if (type.equals("yml")) {
            System.out.println("yml!");
            // Fake read to load the lazy field to be able update it
            String temp = application.serverInitYml;

            application.serverInitYml = contents;
            application.hasServerInitYml = contents != null;
        }
    }

    @Transactional
    public void deleteConfigFileContents(String appName, String type) {
        Application application = findByName(appName);

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

    private AppArchive findByApplicationAndName(Application application, String name) {
        AppArchive appArchive = AppArchive.find("application=:application AND fileName=:name",
                Parameters
                        .with("application", application)
                        .and("name", name)).firstResult();
        return appArchive;
    }
}
