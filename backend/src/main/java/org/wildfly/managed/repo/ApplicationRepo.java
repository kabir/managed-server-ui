package org.wildfly.managed.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.wildfly.managed.common.model.Application;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class ApplicationRepo implements PanacheRepository<Application> {

    @Transactional
    public Application create(Application application) {
        persistAndFlush(application);
        return application;
    }

    public Application findByName(String name) {
        return find("name", name).firstResult();
    }

    public void delete(String name) {
        delete("name", name);
    }

    public List<Application> all() {
        return listAll();
    }

}
