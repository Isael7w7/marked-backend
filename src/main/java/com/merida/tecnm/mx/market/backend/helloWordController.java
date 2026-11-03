package com.merida.tecnm.mx.market.backend;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/saludar")
public class helloWordController {
    @GetMapping("/saludo")
    public String helloWord(){
        return "hello Word!";
    }
}
