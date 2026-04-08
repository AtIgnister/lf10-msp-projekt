package org.lf10.stimmungsumfrage.Repositories;

import org.lf10.stimmungsumfrage.Models.Channel;
import org.lf10.stimmungsumfrage.Models.ChannelFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelFeedbackRepository extends JpaRepository<ChannelFeedback, Long> {

    List<ChannelFeedback> findByChannelOrderByCreatedAtDesc(Channel channel);
}
