package org.example.service.impl;

import org.example.model.RespondToMobile;
import org.example.model.TrackBatch;
import org.example.repository.localTempRepository;
import org.example.service.ReceiverService;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service // sing up ReceiverServiceImpl class and its interface in Spring bean
public class ReceiverServiceImpl implements ReceiverService {

    private final localTempRepository repo;

    public ReceiverServiceImpl(localTempRepository repo) { this.repo = repo;}

    @Override
    public RespondToMobile receiveBatchFromMobile(TrackBatch batch) throws Exception {
        if (batch == null || CollectionUtils.isEmpty(batch.locations)) {
            throw new IllegalArgumentException("invalid payload: locations can not be empty");
        }
        repo.savePackagesFromMobile(batch);
        return RespondToMobile.of(batch.batchId,true);
    }
}
