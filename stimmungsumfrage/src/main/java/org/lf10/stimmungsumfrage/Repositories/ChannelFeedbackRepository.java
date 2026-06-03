package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.Channel;
import org.lf10.stimmungsumfrage.Models.ChannelFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChannelFeedbackRepository extends JpaRepository<ChannelFeedback, Long> {

    List<ChannelFeedback> findByChannelOrderByCreatedAtDesc(Channel channel);

    List<ChannelFeedback> findByChannelAndCreatedAtBetween(
            Channel channel,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("SELECT cf FROM ChannelFeedback cf " +
            "WHERE ((cf.comment IS NOT NULL AND TRIM(cf.comment) <> '') " +
            "OR (cf.emoji IS NOT NULL AND TRIM(cf.emoji) <> '')) " +
            "AND (:departmentId IS NULL OR cf.user.department.id = :departmentId) " +
            "AND (:channelId IS NULL OR cf.channel.id = :channelId) " +
            "AND (:startDate IS NULL OR cf.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR cf.createdAt < :endDate) " +
            "ORDER BY cf.createdAt DESC")
    List<ChannelFeedback> findFilteredForFeedbackList(
            @Param("departmentId") Long departmentId,
            @Param("channelId") Long channelId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
