package com.example.Reelify.repository;

import com.example.Reelify.model.VideoMetadata;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VideoMetadataRepository extends CassandraRepository<VideoMetadata, UUID> {
}
