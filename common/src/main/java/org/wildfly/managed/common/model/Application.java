package org.wildfly.managed.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.Type;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.sql.Date;
import java.util.Collection;
import java.util.HashSet;

import static javax.persistence.FetchType.LAZY;

@Entity
public class Application extends PanacheEntity {
    @Column(unique = true)
    public String name;

    @OneToMany(mappedBy = "application")
    public Collection<AppArchive> appArchives = new HashSet<>();

    @Basic(fetch = LAZY)
    @LazyGroup("serverConfigXml")
    @Type( type = "text")
    public String serverConfigXml;

    public boolean hasServerConfigXml;

    @Basic(fetch = LAZY)
    @LazyGroup("serverInitCli")
    @Type( type = "text")
    public String serverInitCli;

    public boolean hasServerInitCli;

    @Basic(fetch = LAZY)
    @LazyGroup("serverInitYml")
    @Type( type = "text")
    public String serverInitYml;

    public boolean hasServerInitYml;

    public void loadLazyFields() {
        Object tmp = serverConfigXml;
        tmp = serverInitCli;
        tmp = serverInitYml;
        tmp = appArchives;
    }
}

