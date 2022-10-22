package org.wildfly.managed.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.wildfly.managed.ConfigFileInspection;
import org.wildfly.managed.common.model.AppArchive;
import org.wildfly.managed.common.model.Application;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
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
        appArchives.add(appArchive);
        application.appArchives = appArchives;

        appArchive.persist();
    }

    @Transactional
    public void updateApplicationArchive(Application application, String fileName, ConfigFileInspection configFileInspection) {
        System.out.println("Updating " + fileName);
        // TODO Didn't there use to be something like attach?
        application = findByName(application.name);
        if (application == null) {
            throw new IllegalStateException("Could not find application " + application.name);
        }

        AppArchive found = null;
        Collection<AppArchive> appArchives = application.appArchives;
        for (AppArchive existing : appArchives) {
            System.out.println("--- " + existing.fileName);
            if (existing.fileName.equals(fileName)) {
                found = existing;
                System.out.println("--- matches ");
                break;
            }
        }
        System.out.println("--- Checking if found");
        if (found == null) {
            System.out.println("--- Not found");
            // TODO if not working we should undo the file copy in the caller
            throw new IllegalStateException("No existing application called " + fileName);
        } else {
            found.serverConfigXml = configFileInspection.isServerConfigXml();
            found.serverInitCli = configFileInspection.isServerInitCli();
            found.serverInitYml = configFileInspection.isServerInitYml();
        }
        System.out.println("--- done");
    }

    @Transactional
    public void deleteApplicationArchive(Application application, String fileName) {
        application = findByName(application.name);
        if (application == null) {
            throw new IllegalStateException("Could not find application " + application.name);
        }

        AppArchive found = null;
        Collection<AppArchive> appArchives = application.appArchives;
        for (AppArchive existing : appArchives) {
            System.out.println("--- " + existing.fileName);
            if (existing.fileName.equals(fileName)) {
                found = existing;
                System.out.println("--- matches ");
                break;
            }
        }
        System.out.println("--- Checking if found");
        if (found == null) {
            System.out.println("--- Not found");
            // TODO if not working we should undo the file copy in the caller
            throw new IllegalStateException("No existing application called " + fileName);
        } else {
            appArchives.remove(found);
            found.delete();
            found.application = null;
        }

    }
}
