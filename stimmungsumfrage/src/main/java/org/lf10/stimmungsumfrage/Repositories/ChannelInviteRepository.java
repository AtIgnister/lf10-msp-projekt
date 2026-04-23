package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.Channel;
import org.lf10.stimmungsumfrage.Models.ChannelInvite;
import org.lf10.stimmungsumfrage.Models.InviteStatus;
import org.lf10.stimmungsumfrage.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelInviteRepository extends JpaRepository<ChannelInvite, Long> {

    List<ChannelInvite> findByInvitedUserAndStatus(User user, InviteStatus status);

    List<ChannelInvite> findByChannelAndStatus(Channel channel, InviteStatus status);

    boolean existsByChannelAndInvitedUserAndStatus(Channel channel, User user, InviteStatus status);
}
