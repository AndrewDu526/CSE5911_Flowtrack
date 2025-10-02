package org.example.repository;

import org.example.model.bundle.Batch;
import org.example.model.map.FloorMap;

import java.util.List;


public interface LocalRepository {

    public FloorMap loadMapSetting(String dir, String fileName);

    public List<Batch> loadBatches(String dir, int size);

    public void saveBatches(Batch batches) throws Exception;
}
