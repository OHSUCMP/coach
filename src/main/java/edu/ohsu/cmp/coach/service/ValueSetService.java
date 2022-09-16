package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.ValueSet;
import edu.ohsu.cmp.coach.repository.ValueSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class ValueSetService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private VSACService vsacService;

    @Autowired
    private ValueSetRepository repository;

    @Value("${vsac.value-set.oid-list.csv}")
    private String oidListCSV;
    private List<String> oidList = null;

    public ValueSet getValueSet(String oid) {
        return repository.findOneByOid(oid);
    }

    public void refreshDefinedValueSets() {
        for (String oid : getOidList()) {
            refresh(oid);
        }
    }

    public List<String> getOidList() {
        if (oidList == null) {
            oidList = Arrays.asList(oidListCSV.trim().split("\\s*,\\s*"));
        }
        return oidList;
    }


//////////////////////////////////////////////////////////////////////
// private stuff
//

    private void refresh(String oid) {
        try {
            ValueSet valueSet = getValueSet(oid);
            if (valueSet != null) {
                logger.info("refreshing ValueSet with oid=" + oid);
                valueSet.update(vsacService.getValueSet(oid));
            } else {
                logger.info("acquiring new ValueSet with oid=" + oid);
                valueSet = vsacService.getValueSet(oid);
            }
            repository.save(valueSet);

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " refreshing ValueSet with oid=" + oid, e);
        }
    }
}
