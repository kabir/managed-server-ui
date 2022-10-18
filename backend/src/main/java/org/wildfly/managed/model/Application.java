package org.wildfly.managed.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.List;

@Entity
public class Application extends PanacheEntity {
    @Column(unique = true)
    String name;
    ApplicationState state = ApplicationState.INITIAL;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ApplicationState getState() {
        return state;
    }

    public void setState(ApplicationState state) {
        this.state = state;
    }

    static Application create(String name, ApplicationState state) {
        Application app = new Application();
        app.name = name;
        app.state = state;
        persist(app);
        return app;
    }

    static Application findByName(String name) {
        return find("name", name).firstResult();
    }

    static void delete(String name) {
        delete("name", name);
    }

    static List<Application> all() {
        return listAll();
    }
}
