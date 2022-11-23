package it.polimi.dummymanagedsystem.randintvendorservice.rest;

import it.polimi.dummymanagedsystem.randintvendorservice.domain.RandintVendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@org.springframework.web.bind.annotation.RestController
@RequestMapping(path="/rest")
public class RestController {
    @Autowired
    private RandintVendorService randintVendorService;

    @GetMapping("/randomNumber")
    public int getRandomNumber() {
        return randintVendorService.getRandomNumber();
    }

}
