package org.example.repository;

import org.example.model.TrackBatch;
import org.springframework.stereotype.Repository;


public interface localTempRepository {

    void savePackagesFromMobile(TrackBatch batch) throws Exception;
}
