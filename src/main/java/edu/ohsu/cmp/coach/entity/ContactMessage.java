package edu.ohsu.cmp.coach.entity;

import javax.persistence.*;

@Entity
@Table(name = "contact_message")
public class ContactMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String messageKey;
    private String subject;
    private String body;
    private String aboveText;
    private String belowText;

    protected ContactMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAboveText() {
        return aboveText;
    }

    public void setAboveText(String beforeText) {
        this.aboveText = beforeText;
    }

    public String getBelowText() {
        return belowText;
    }

    public void setBelowText(String afterText) {
        this.belowText = afterText;
    }
}
