package org.wildfly.managed.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Objects;

@Entity
public class AppArchive extends PanacheEntity {

    @ManyToOne
    public Application application;

    public String fileName;

    public boolean serverConfigXml;

    public boolean serverInitCli;

    public boolean serverInitYml;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppArchive)) return false;
        AppArchive that = (AppArchive) o;
        return fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName);
    }
}
