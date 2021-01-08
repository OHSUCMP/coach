package edu.ohsu.cmp.htnu18app.entity;

import javax.persistence.*;

@Entity
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "pat_id_hash")
    private String patIdHash;

    protected Patient() {
    }

    public Patient(String patIdHash) {
        this.patIdHash = patIdHash;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPatIdHash() {
        return patIdHash;
    }

    public void setPatIdHash(String patIdHash) {
        this.patIdHash = patIdHash;
    }
}
