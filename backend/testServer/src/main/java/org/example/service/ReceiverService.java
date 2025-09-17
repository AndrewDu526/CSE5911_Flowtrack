package org.example.service;

import org.example.model.RespondToMobile;
import org.example.model.TrackBatch;

public interface ReceiverService{
    RespondToMobile receiveBatchFromMobile(TrackBatch trackBatch) throws Exception;
}