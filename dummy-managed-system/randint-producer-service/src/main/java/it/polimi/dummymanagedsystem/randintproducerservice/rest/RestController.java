package it.polimi.dummymanagedsystem.randintproducerservice.rest;

import it.polimi.dummymanagedsystem.randintproducerservice.domain.RandintProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@org.springframework.web.bind.annotation.RestController
@RequestMapping(path="/rest")
public class RestController {
    @Autowired
    private RandintProducerService randintProducerService;

    @GetMapping("/generateRandomInt")
    public int generateRandomInt() {
        return randintProducerService.generateRandomInt();
    }

}
