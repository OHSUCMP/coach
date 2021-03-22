package edu.ohsu.cmp.htnu18app.entity.app;

import javax.persistence.*;

@Entity
@Table(schema = "htnu18app")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pat_id_hash")
    private String patIdHash;

    protected Patient() {
    }

    public Patient(String patIdHash) {
        this.patIdHash = patIdHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatIdHash() {
        return patIdHash;
    }

    public void setPatIdHash(String patIdHash) {
        this.patIdHash = patIdHash;
    }
}
