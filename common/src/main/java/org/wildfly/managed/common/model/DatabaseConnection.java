package org.wildfly.managed.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        uniqueConstraints = {
                // Doesn't seem to show up in postgres?
                @UniqueConstraint(columnNames = {
                        "application_id",
                        "jndiName"})
        }
)
public class DatabaseConnection extends PanacheEntity {

    @JsonIgnore
    @ManyToOne
    public Application application;

    public Type type;

    public String databaseName;

    public String username;

    public String password;

    public String url;

    public String jndiName;


    public enum Type {
        POSTGRES
    }
}
