package com.cloud.service;

import com.cloud.document.ProfileDocument;
import com.cloud.dto.ProfileDTO;
import com.cloud.repository.ProfileRepository;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileRepository profileRepository;
    private ModelMapper modelMapper;

    public ProfileDTO createProfile(ProfileDTO profileDTO){

        if(profileRepository.existsByClerkId(profileDTO.getClerkId())){
            return updateProfile(profileDTO);
        }

        ProfileDocument profile = ProfileDocument.builder()
                .clerkId(profileDTO.getClerkId())
                .email(profileDTO.getEmail())
                .firstName(profileDTO.getFirstName())
                .lastName(profileDTO.getLastName())
                .photoUrl(profileDTO.getPhotoUrl())
                .credits(5)
                .createdAt(Instant.now())
                .build();

        profile = profileRepository.save(profile);

        return ProfileDTO.builder()
                .id(profile.getId())
                .clerkId(profile.getClerkId())
                .firstName(profileDTO.getFirstName())
                .lastName(profileDTO.getLastName())
                .email(profileDTO.getEmail())
                .photoUrl(profileDTO.getPhotoUrl())
                .credits(profileDTO.getCredits())
                .build();
    }

    public ProfileDTO updateProfile(ProfileDTO profileDTO){
        ProfileDocument existingProfile = profileRepository.findByClerkId(profileDTO.getClerkId());
        if(existingProfile != null){
            // update the fields
            if(profileDTO.getEmail() != null && !profileDTO.getEmail().isEmpty()){
                existingProfile.setEmail(profileDTO.getEmail());
                existingProfile.setFirstName(profileDTO.getFirstName());
                existingProfile.setLastName(profileDTO.getLastName());
            }
            if(profileDTO.getFirstName() != null && !profileDTO.getFirstName().isEmpty()){
                existingProfile.setFirstName(profileDTO.getFirstName());
            }
            if(profileDTO.getLastName() != null && !profileDTO.getLastName().isEmpty()){
                existingProfile.setLastName(profileDTO.getLastName());
            }
            if(profileDTO.getPhotoUrl() != null && !profileDTO.getPhotoUrl().isEmpty()){
                existingProfile.setPhotoUrl(profileDTO.getPhotoUrl());
            }

            profileRepository.save(existingProfile);

            return ProfileDTO.builder()
                    .id(existingProfile.getId())
                    .clerkId(existingProfile.getClerkId())
                    .email(existingProfile.getEmail())
                    .firstName(existingProfile.getFirstName())
                    .lastName(existingProfile.getLastName())
                    .photoUrl(existingProfile.getPhotoUrl())
                    .credits(existingProfile.getCredits())
                    .createdAt(existingProfile.getCreatedAt())
                    .build();

        }
        return null;
    }

    public Boolean existsByClerkId(String clerkId){
        return profileRepository.existsByClerkId(clerkId);
    }

    public void deleteProfile(String clerkId){
        ProfileDocument existingProfile = profileRepository.findByClerkId(clerkId);
        if(existingProfile != null){
            profileRepository.delete(existingProfile);
        }
    }

    // AFTER
    public ProfileDocument getCurrentProfile(){
        if(SecurityContextHolder.getContext().getAuthentication() == null){
            throw new UsernameNotFoundException("User not authenticated");
        }
        String clerkId = SecurityContextHolder.getContext().getAuthentication().getName();
        ProfileDocument profile = profileRepository.findByClerkId(clerkId);

        if(profile == null){
            // Webhook missed — create profile on the fly
            profile = ProfileDocument.builder()
                    .clerkId(clerkId)
                    .credits(5)
                    .createdAt(Instant.now())
                    .build();
            profile = profileRepository.save(profile);
        }

        return profile;
    }

//    public ProfileDocument getCurrentProfile(){
//        if(SecurityContextHolder.getContext().getAuthentication() == null){
//            throw new UsernameNotFoundException("User not authenticated");
//        }
//        String clerkId = SecurityContextHolder.getContext().getAuthentication().getName();
//        return profileRepository.findByClerkId(clerkId);
//    }


}
