package org.example.controller;


import org.example.model.bundle.Batch;
import org.example.model.internet.RespondToMobile;
import org.example.service.listerning.ReceiverService;

import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class ReceiverController {

    // final: can only assign onceFlowTrackServerReceiver
    // use Interface type: decouple, Controller does not care how to implement, only care about available method
    private final ReceiverService receiveService;

    public ReceiverController(ReceiverService service){
        this.receiveService = service;
    }

    @PostMapping("/FlowTrackServerListenerToMobile") // auto listen port:3000 with path FlowTrackServerReceiver
    // @RequestBody auto transfer HTTP body json to assigned data type: TrackBatch
    public ResponseEntity<RespondToMobile> receive(@Valid @RequestBody Batch batch) throws Exception {
        RespondToMobile respond = receiveService.receiveBatchFromMobile(batch);
        // ResponseEntity: state code, response head and body
        // response body transfer to json
        return ResponseEntity.ok(respond);
    }
}
