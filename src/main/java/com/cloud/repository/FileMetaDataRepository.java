package com.cloud.repository;

import com.cloud.document.FileMetaDataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetaDataRepository extends MongoRepository<FileMetaDataDocument,String> {

    List<FileMetaDataDocument> findByClerkId(String clerkId);
    // this method returns the count of files upload by a single user:
    Long countByClerkId(String clerkId);

}
