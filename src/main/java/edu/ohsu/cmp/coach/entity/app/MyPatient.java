package edu.ohsu.cmp.coach.entity.app;

import javax.persistence.*;

@Entity
@Table(schema = "coach", name = "patient")
public class MyPatient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patIdHash;

    protected MyPatient() {
    }

    public MyPatient(String patIdHash) {
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
