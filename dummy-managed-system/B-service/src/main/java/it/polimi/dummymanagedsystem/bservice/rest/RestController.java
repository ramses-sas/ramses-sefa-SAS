package it.polimi.dummymanagedsystem.bservice.rest;

import it.polimi.dummymanagedsystem.bservice.domain.RandGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@org.springframework.web.bind.annotation.RestController
@RequestMapping(path="/rest")
public class RestController {
    @Autowired
    private RandGeneratorService randGeneratorService;

    @GetMapping("/generateRandomInt")
    public int generateRandomInt() {
        return randGeneratorService.generateRandomInt();
    }

}
