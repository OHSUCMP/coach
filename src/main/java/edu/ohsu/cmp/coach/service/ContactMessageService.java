package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.app.ContactMessage;
import edu.ohsu.cmp.coach.repository.app.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContactMessageService extends BaseService {

    @Autowired
    private ContactMessageRepository repository;

    public ContactMessage getMessage(String key) {
        return repository.findOneByMessageKey(key);
    }
}
