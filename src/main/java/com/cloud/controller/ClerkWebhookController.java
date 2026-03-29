package com.cloud.controller;

import com.cloud.dto.ProfileDTO;
import com.cloud.service.ProfileService;
import com.cloud.service.UserCreditsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class ClerkWebhookController {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
//    private Logger logger = LoggerFactory.getLogger(ClerkWebhookController.class);

    @Value("${clerk.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/clerk")
    public ResponseEntity<?> handleClerkWebhook(
            @RequestHeader("svix-id") String svixId,
            @RequestHeader("svix-timestamp") String svixTimestamp,
            @RequestHeader("svix-signature") String svixSignature,
            @RequestBody String payload
    ){

        System.out.println(">>> Webhook hit! eventType incoming...");
        System.out.println(">>> Payload: " + payload);

        try {
           boolean isValid =  verifyWebhookSignature(svixId,svixTimestamp,svixSignature,payload);
           if(!isValid){
               return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid webhook signature");
           }

            ObjectMapper mapper = new ObjectMapper();
           JsonNode rootNode = mapper.readTree(payload);

//           logger.info("Root Node Data: {}",rootNode);
//           logger.info("Data: {}",rootNode.path("data"));

           String eventType = rootNode.path("type").asText();

           switch (eventType){
               case "user.created":
                   handleUserCreated(rootNode.path("data"));
                   break;
               case "user.updated":
                   handleUserUpdated(rootNode.path("data"));
                   break;
               case "user.deleted":
                   handleUserDeleted(rootNode.path("data"));
                   break;
           }
           return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,e.getMessage());
        }

    }

    private void handleUserDeleted(JsonNode data) {
        String clerkId = data.path("id").asText();
        profileService.deleteProfile(clerkId);
    }

    private void handleUserUpdated(JsonNode data) {
        String clerkId = data.path("id").asText();
        String email="";
        JsonNode email_addresses = data.path("email_addresses");
        if(email_addresses.isArray() && !email_addresses.isEmpty()){
            email = email_addresses.get(0).path("email_address").asText();
        }

        String firstName = data.path("first_name").asText("");
        String lastName = data.path("last_name").asText("");
        String photoUrl = data.path("image_url").asText("");

        ProfileDTO updateProfile =  ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

        updateProfile = profileService.updateProfile(updateProfile);
        if(updateProfile == null){
            handleUserCreated(data);
        }

    }


    private void handleUserCreated(JsonNode data) {
       String clerkId = data.path("id").asText();

       String email = "";
       JsonNode emailAddresses = data.path("email_addresses");
       if(emailAddresses.isArray() && !emailAddresses.isEmpty()){
           email = emailAddresses.get(0).path("email_address").asText();
       }

       String firstName = data.path("first_name").asText("");
       String lastName = data.path("last_name").asText("");
       String photoUrl = data.path("image_url").asText("");

       ProfileDTO newProfile =  ProfileDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .photoUrl(photoUrl)
                .build();

       profileService.createProfile(newProfile);
       userCreditsService.createInitialCredits(clerkId);

    }

    private boolean verifyWebhookSignature(String svixId, String svixTimestamp, String svixSignature, String payload) {
        //validate signature
        return true;
    }

}
