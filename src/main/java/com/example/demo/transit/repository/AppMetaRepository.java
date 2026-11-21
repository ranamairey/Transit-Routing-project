package com.example.demo.transit.repository;

import com.example.demo.transit.entity.AppMeta;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppMetaRepository extends MongoRepository<AppMeta, String> {
}
