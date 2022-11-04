package it.polimi.dummymanagedsystem.aservice.rest;

import it.polimi.dummymanagedsystem.aservice.domain.RandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@org.springframework.web.bind.annotation.RestController
@RequestMapping(path="/rest")
public class RestController {
    @Autowired
    private RandService randService;

    @GetMapping("/randomNumber")
    public int getRandomNumber() {
        return randService.getRandomNumber();
    }

}
