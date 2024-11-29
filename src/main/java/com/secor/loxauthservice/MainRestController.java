package com.secor.loxauthservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class MainRestController {

    private static final Logger log = LoggerFactory.getLogger(MainRestController.class);


    CredentialRepository credentialRepository;
   AuthtokenRepository authtokenRepository;

   MainRestController(CredentialRepository credentialRepository,
                      AuthtokenRepository authtokenRepository)
   {
       this.credentialRepository = credentialRepository;
       this.authtokenRepository = authtokenRepository;
   }

    @PostMapping("/signup")
    public ResponseEntity<Credential> signup(@RequestParam("username") String username,
                                             @RequestParam("password") String password)
    {
        Credential credential = new Credential();
        credential.setUsername(username);
        credential.setPassword(password);

        credentialRepository.save(credential);

        credential.setPassword("*********");

        return ResponseEntity.ok(credential);
    }

    @PostMapping("/login") // THE TOKEN IS ONLY GENERATED HERE AND NOT VALIDATED
    public ResponseEntity<LoxAuthentication> login(
                        @RequestParam("username") String username,
                        @RequestParam("password") String password
                       )
    {
        Credential credential = credentialRepository.findById(username).orElse(null);
        if (credential != null && credential.getPassword().equals(password))
        {
            LoxAuthentication loxAuthentication = new LoxAuthentication();

            Authtoken authtoken = new Authtoken();
            authtoken.setUsername(username);
            authtoken.setExpirytime(300);
            authtoken.setToken(String.valueOf(((int) (Math.random()*1000000))));
            authtoken.setCreationtime(Instant.now());
            authtokenRepository.save(authtoken);


            loxAuthentication.setAuthenticated(true);
            loxAuthentication.setAuthtoken(authtoken);
            loxAuthentication.setMessage("LOGIN SUCCESSFUL");

            return ResponseEntity.ok(loxAuthentication);
        }
        else
        {
            LoxAuthentication loxAuthentication = new LoxAuthentication();
            loxAuthentication.setAuthenticated(false);
            loxAuthentication.setMessage("INVALID CREDENTIALS");
            return ResponseEntity.status(HttpStatusCode.valueOf(401)).body(loxAuthentication);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Authtoken> validate(
            @RequestHeader("Sectoken") String token
    )
    {

        log.info("TOKEN VALIDATION STARTED...");

        Authtoken authtoken = authtokenRepository.findById(token).orElse(null);
        if (authtoken != null)
        {
            // CHECK THE EXPIRY
            Duration duration = Duration.between(authtoken.getCreationtime(), Instant.now());
            if(duration.getSeconds() > authtoken.getExpirytime())
            {
                authtokenRepository.deleteById(token);

                Authtoken badAuthToken = new Authtoken();
                badAuthToken.setCreationtime(Instant.MAX);
                badAuthToken.setUsername("INVALIDUSER");
                badAuthToken.setExpirytime(0);
                badAuthToken.setToken("TOKENEXPIRED");
                log.info("TOKEN EXPIRED");
                return ResponseEntity.status(HttpStatusCode.valueOf(401)).body(badAuthToken);

            }

            log.info("TOKEN VALIDATED SUCCESSFULY"+authtoken);
            return ResponseEntity.ok(authtoken);
        }
        else
        {
            Authtoken badAuthToken = new Authtoken();
            badAuthToken.setCreationtime(Instant.MAX);
            badAuthToken.setUsername("INVALIDUSER");
            badAuthToken.setExpirytime(0);
            badAuthToken.setToken("INVALIDTOKEN");
            log.info("TOKEN INVALID");
            return ResponseEntity.status(HttpStatusCode.valueOf(401)).body(badAuthToken);
        }
    }

}