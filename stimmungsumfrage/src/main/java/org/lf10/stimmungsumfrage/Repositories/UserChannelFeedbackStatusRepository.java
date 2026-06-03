package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.Channel;
import org.lf10.stimmungsumfrage.Models.User;
import org.lf10.stimmungsumfrage.Models.UserChannelFeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserChannelFeedbackStatusRepository
        extends JpaRepository<UserChannelFeedbackStatus, Long> {

    Optional<UserChannelFeedbackStatus>
    findByUserAndChannel(User user, Channel channel);
}
