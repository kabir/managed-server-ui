package org.wildfly.managed.repo;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import org.wildfly.managed.common.model.Application;
import org.wildfly.managed.common.model.ApplicationState;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ApplicationRepo implements PanacheRepository<Application> {


    public Application create(String name, ApplicationState state) {
        Application app = new Application();
        app.setName(name);
        app.setState(state);
        persist(app);
        return app;
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
