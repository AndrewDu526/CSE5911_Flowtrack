package org.example.service.listerning;

import org.example.model.bundle.Batch;
import org.example.model.internet.RespondToMobile;

public interface ReceiverService{
    RespondToMobile receiveBatchFromMobile(Batch trackBatch) throws Exception;
}