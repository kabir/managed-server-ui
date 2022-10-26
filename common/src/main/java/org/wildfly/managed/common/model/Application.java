package org.wildfly.managed.common.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.Type;

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

    @Column
    @Basic(fetch = LAZY)
    @LazyGroup("serverConfigXml")
    // org.hibernate.type.TextType is Postgres only, and avoids the large object store
    @Type( type = "text")
    public String serverConfigXml;

    public boolean hasServerConfigXml;

    @Column
    @Basic(fetch = LAZY)
    @LazyGroup("serverInitCli")
    // org.hibernate.type.TextType is Postgres only, and avoids the large object store
    @Type( type = "text")
    public String serverInitCli;

    public boolean hasServerInitCli;

    @Column
    @Basic(fetch = LAZY)
    @LazyGroup("serverInitYml")
    // org.hibernate.type.TextType is Postgres only, and avoids the large object store
    @Type( type = "text")
    public String serverInitYml;

    public boolean hasServerInitYml;

}

