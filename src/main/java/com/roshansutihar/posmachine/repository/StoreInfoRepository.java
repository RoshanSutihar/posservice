package com.roshansutihar.posmachine.repository;

import com.roshansutihar.posmachine.entity.StoreInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreInfoRepository extends JpaRepository<StoreInfo, Long> {
    Optional<StoreInfo> findFirstByOrderByIdAsc();
}
