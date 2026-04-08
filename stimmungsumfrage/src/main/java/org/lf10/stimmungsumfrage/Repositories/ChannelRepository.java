package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.Channel;
import org.lf10.stimmungsumfrage.Models.ChannelType;
import org.lf10.stimmungsumfrage.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    @Query("SELECT c FROM Channel c JOIN c.members m WHERE m = :user")
    List<Channel> findByMember(@Param("user") User user);

    @Query("SELECT c FROM Channel c WHERE c.channelType = :type AND :user NOT MEMBER OF c.members")
    List<Channel> findAvailableChannels(@Param("type") ChannelType type, @Param("user") User user);
}
