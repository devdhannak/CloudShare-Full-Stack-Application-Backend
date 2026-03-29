package com.cloud.service;

import com.cloud.document.UserCredits;
import com.cloud.repository.UserCreditsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCreditsService {

    private final UserCreditsRepository userCreditsRepository;
    private final ProfileService profileService;

    public UserCredits createInitialCredits(String clerkId){
        UserCredits userCredits = UserCredits.builder()
                .clerkId(clerkId)
                .credits(5)
                .plan("BASIC")
                .build();

        return userCreditsRepository.save(userCredits);
    }
    public UserCredits getUserCredits(String clerkId){
        UserCredits userCredits = userCreditsRepository.findByClerkId(clerkId);

        // No record at all → create one
        if(userCredits == null){
            return createInitialCredits(clerkId);
        }

        // Credits null → fix in place, don't create duplicate
//        if(userCredits.getCredits() == null){
//            userCredits.setCredits(5);
//            return userCreditsRepository.save(userCredits);
//        }

        return userCredits;
    }

    public UserCredits getUserCredits(){
        String clerkId = profileService.getCurrentProfile().getClerkId();
        return getUserCredits(clerkId);
    }

    public Boolean hasEnoughCredits(int requiredCredits){
        UserCredits userCredits = getUserCredits();
        return userCredits.getCredits() >= requiredCredits;
    }

    public UserCredits consumeCredit(){
        UserCredits userCredits = getUserCredits();
        if(userCredits.getCredits() <= 0){
            return null;
        }
        userCredits.setCredits(userCredits.getCredits()-1);
        return userCreditsRepository.save(userCredits);
    }

    public UserCredits addCredits(String clerkId, Integer creditsToAdd,String plan){
        UserCredits userCredits = userCreditsRepository.findByClerkId(clerkId);
        if(userCredits == null){
            createInitialCredits(clerkId);
        }
        userCredits.setCredits(userCredits.getCredits() + creditsToAdd);
        userCredits.setPlan(plan);
        return userCreditsRepository.save(userCredits);
    }

}
