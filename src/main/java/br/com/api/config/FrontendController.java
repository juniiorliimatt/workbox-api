package br.com.api.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping("/")
    public String redirectToFrontend() {
        return "forward:/index.html";
    }

}
