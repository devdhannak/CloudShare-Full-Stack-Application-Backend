package com.cloud.controller;

import com.cloud.dto.ProfileDTO;
import com.cloud.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/register")
    public ResponseEntity<ProfileDTO> createProfile(@RequestBody ProfileDTO profileDTO){

        HttpStatus status = profileService.existsByClerkId(profileDTO.getClerkId()) ? HttpStatus.OK
                : HttpStatus.CREATED;
        ProfileDTO savedProfile = profileService.createProfile(profileDTO);
       return ResponseEntity.status(status).body(savedProfile);
    }
}
