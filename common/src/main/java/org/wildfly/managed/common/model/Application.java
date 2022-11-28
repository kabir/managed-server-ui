package org.wildfly.managed.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.Type;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static javax.persistence.FetchType.LAZY;

@Entity
public class Application extends PanacheEntity {
    @Column(unique = true)
    public String name;

    @OneToMany(mappedBy = "application")
    public Collection<AppArchive> appArchives = new HashSet<>();

    @JsonIgnore
    @Basic(fetch = LAZY)
    @LazyGroup("deploymentRecord")
    @OneToMany(mappedBy = "application")
    public List<DeploymentRecord> deploymentRecords = new ArrayList<>();

    @JsonIgnore
    @Basic(fetch = LAZY)
    @LazyGroup("dbConnection")
    @OneToMany(mappedBy = "application")
    public List<DatabaseConnection> dbConnections = new ArrayList<>();

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

    @Column(columnDefinition = "TIMESTAMP")
    public LocalDateTime lastConfigChange;

    @Column(columnDefinition = "TIMESTAMP")
    public LocalDateTime lastArchiveChange;

    @PrePersist
    public void prePersist() {
        lastConfigChange = LocalDateTime.now();
        lastArchiveChange = LocalDateTime.now();
    }

    public void loadConfigFields() {
        Object tmp = serverConfigXml;
        tmp = serverInitCli;
        tmp = serverInitYml;
        tmp = appArchives;
    }

    public void loadDeploymentRecords() {
        Object tmp = deploymentRecords;
    }

}

