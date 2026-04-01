package com.cloud.service;
import com.cloud.document.FileMetaDataDocument;
import com.cloud.document.ProfileDocument;
import com.cloud.dto.FileMetaDataDTO;
import com.cloud.repository.FileMetaDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileMetaDataService {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final AwsService awsService;

    public List<FileMetaDataDTO> uploadFiles(MultipartFile files[]) throws IOException {
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        List<FileMetaDataDocument> savedFiles = new ArrayList<>();

        if(!userCreditsService.hasEnoughCredits(files.length)){
            throw new RuntimeException("Not enough credits to upload files. Please purchase more credits");
        }

        Path uploadPath = Paths.get("upload").toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        for(MultipartFile file:files){
            String fileUrl = awsService.uploadFile(file);
            log.info("AWS URL: {}", fileUrl);
//            logger.info("AWS URL: "+fileUrl);

            String fileName = UUID.randomUUID()+"."+ StringUtils.getFilenameExtension(file.getOriginalFilename());
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(),targetLocation, StandardCopyOption.REPLACE_EXISTING);
            FileMetaDataDocument fileMetaData = FileMetaDataDocument.builder()
                    .fileLocation(fileUrl)
                    .awsUrl(fileUrl)
                    .name(file.getOriginalFilename())
                    .size(file.getSize())
                    .type(file.getContentType())
                    .clerkId(currentProfile.getClerkId())
                    .isPublic(false)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            // TODO: Consume one credit for each file upload
            userCreditsService.consumeCredit();

            savedFiles.add(fileMetaDataRepository.save(fileMetaData));
        }
        return savedFiles.stream().map(this::mapToDTO).collect(Collectors.toList());

    }

    public List<FileMetaDataDTO> getFiles(){
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        List<FileMetaDataDocument> files = fileMetaDataRepository.findByClerkId(currentProfile.getClerkId());
        return files.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public FileMetaDataDTO getPublicFile(String id){
        Optional<FileMetaDataDocument> fileOptional = fileMetaDataRepository.findById(id);
        if(fileOptional.isEmpty() || !fileOptional.get().getIsPublic()){
            throw new RuntimeException("Unable to get the file.");
        }

        FileMetaDataDocument document = fileOptional.get();
        return mapToDTO(document);

    }

    public FileMetaDataDTO getDownloadableFile(String id){
        FileMetaDataDocument file = fileMetaDataRepository.findById(id).orElseThrow(()-> new RuntimeException("File not found."));
        return mapToDTO(file);

    }

    public void deleteFile(String id){

        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            FileMetaDataDocument file = fileMetaDataRepository.findById(id).orElseThrow(()-> new RuntimeException("File not found."));

            if(!file.getClerkId().equals(currentProfile.getClerkId())){
                throw new RuntimeException("File is not belong to current user.");
            }
            Path filePath = Paths.get(file.getFileLocation());
            Files.deleteIfExists(filePath);
            fileMetaDataRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Error while deleting the file.");
        }
    }


    public FileMetaDataDTO togglePublic(String id){
            FileMetaDataDocument file = fileMetaDataRepository.findById(id)
                    .orElseThrow(()-> new RuntimeException("File not found."));

            file.setIsPublic(!file.getIsPublic());
            fileMetaDataRepository.save(file);
            return mapToDTO(file);
    }



    private FileMetaDataDTO mapToDTO(FileMetaDataDocument fileMetaDataDocument){
        return FileMetaDataDTO.builder()
                .fileLocation(fileMetaDataDocument.getFileLocation())
                .awsUrl(fileMetaDataDocument.getAwsUrl())
                .name(fileMetaDataDocument.getName())
                .size(fileMetaDataDocument.getSize())
                .id(fileMetaDataDocument.getId())
                .clerkId(fileMetaDataDocument.getClerkId())
                .uploadedAt(fileMetaDataDocument.getUploadedAt())
                .type(fileMetaDataDocument.getType())
                .isPublic(fileMetaDataDocument.getIsPublic())
                .build();
    }

}
