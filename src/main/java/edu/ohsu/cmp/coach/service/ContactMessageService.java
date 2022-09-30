package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.ContactMessage;
import edu.ohsu.cmp.coach.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContactMessageService extends AbstractService {

    @Autowired
    private ContactMessageRepository repository;

    public ContactMessage getMessage(String key) {
        return repository.findOneByMessageKey(key);
    }
}
