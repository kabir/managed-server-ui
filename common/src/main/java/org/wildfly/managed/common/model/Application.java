package org.wildfly.managed.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.Collection;
import java.util.HashSet;

import static javax.persistence.FetchType.LAZY;

@Entity
public class Application extends PanacheEntity {
    @Column(unique = true)
    public String name;
    public ApplicationState state = ApplicationState.INITIAL;

    @OneToMany(mappedBy = "application")
    public Collection<AppArchive> appArchives = new HashSet<>();

    @javax.persistence.Lob
    @Basic(fetch=LAZY)
    public String serverConfigXml;

    public boolean hasServerConfigXml;
}

