package org.wildfly.managed.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(
        uniqueConstraints = {
                // Doesn't seem to show up in postgres?
                @UniqueConstraint(columnNames = {
                        "application",
                        "fileName"})
        }
)
public class AppArchive extends PanacheEntity {

    @JsonIgnore
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
