package com.songlanyun.fileManager.dao;

import com.songlanyun.fileManager.domain.OssConfig;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OssConfigRepository extends ReactiveMongoRepository<OssConfig, String> {

};
