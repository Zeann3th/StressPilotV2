package dev.zeann3th.stresspilot.repository;

import dev.zeann3th.stresspilot.entity.FlowStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FlowStepRepository extends JpaRepository<FlowStepEntity, String> {
    @Transactional
    @Modifying
    @Query("DELETE FROM FlowStepEntity fse WHERE fse.flowId = :flowId")
    void deleteAllByFlowId(@Param("flowId") Long flowId);
}
