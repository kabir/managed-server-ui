package org.wildfly.managed.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class DeploymentRecord extends PanacheEntity {

    @JsonIgnore
    @ManyToOne
    public Application application;

    public boolean buildTriggered;

    @Column(columnDefinition = "TIMESTAMP")
    public LocalDateTime startTime;

    @Column(columnDefinition = "TIMESTAMP")
    public LocalDateTime endTime;

    @Enumerated(EnumType.ORDINAL)
    public Status status;

    boolean isLocked() {
        return endTime == null;
    }

    public enum Status {
        FAILED,
        CANCELLED,
        COMPLETED
    }
}
