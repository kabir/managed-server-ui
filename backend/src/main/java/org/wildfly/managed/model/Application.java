package org.wildfly.managed.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;

@Entity
public class Application extends PanacheEntity {
    String name;
    ApplicationState state;
}
