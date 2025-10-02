package org.example.service.listerning;

import org.example.model.bundle.Batch;
import org.example.model.internet.RespondToMobile;
import org.example.repository.LocalRepository;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service // sing up ReceiverServiceImpl class and its interface in Spring bean
public class ReceiverServiceImpl implements ReceiverService {

    private final LocalRepository repo;

    public ReceiverServiceImpl(LocalRepository repo) { this.repo = repo;}

    @Override
    public RespondToMobile receiveBatchFromMobile(Batch batch) throws Exception {
        if (batch == null || CollectionUtils.isEmpty(batch.points)) {
            throw new IllegalArgumentException("invalid payload: locations can not be empty");
        }
        repo.saveBatches(batch);
        return RespondToMobile.of(batch.batchId,true);
    }
}
