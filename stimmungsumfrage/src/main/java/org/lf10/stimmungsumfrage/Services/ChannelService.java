package org.lf10.stimmungsumfrage.Services;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.*;
import org.lf10.stimmungsumfrage.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelFeedbackRepository feedbackRepository;
    private final ChannelInviteRepository inviteRepository;

    @Transactional
    public Channel createChannel(String name, String description, ChannelType type, User creator) {
        Channel channel = new Channel();
        channel.setName(name);
        channel.setDescription(description);
        channel.setChannelType(type);
        channel.setCreator(creator);
        channel.getMembers().add(creator);
        return channelRepository.save(channel);
    }

    public List<Channel> getUserChannels(User user) {
        return channelRepository.findByMember(user);
    }

    public List<Channel> getAvailableChannels(User user) {
        return channelRepository.findAvailableChannels(ChannelType.OPEN, user);
    }

    @Transactional
    public void joinChannel(Channel channel, User user) {
        channel.getMembers().add(user);
        channelRepository.save(channel);
    }

    @Transactional
    public void leaveChannel(Channel channel, User user) {
        channel.getMembers().remove(user);
        channelRepository.save(channel);
    }

    public List<ChannelFeedback> getFeedback(Channel channel) {
        return feedbackRepository.findByChannelOrderByCreatedAtDesc(channel);
    }

    @Transactional
    public ChannelFeedback sendFeedback(Channel channel, User user, String emoji, String comment) {
        ChannelFeedback feedback = new ChannelFeedback();
        feedback.setChannel(channel);
        feedback.setUser(user);
        feedback.setEmoji(emoji);
        feedback.setComment(comment);
        return feedbackRepository.save(feedback);
    }

    public List<ChannelInvite> getPendingInvites(User user) {
        return inviteRepository.findByInvitedUserAndStatus(user, InviteStatus.PENDING);
    }

    public List<ChannelInvite> getChannelPendingInvites(Channel channel) {
        return inviteRepository.findByChannelAndStatus(channel, InviteStatus.PENDING);
    }

    @Transactional
    public ChannelInvite createInvite(Channel channel, User invitedUser, User invitedBy) {
        if (channel.getMembers().contains(invitedUser)) {
            throw new IllegalStateException("User is already a member");
        }
        if (inviteRepository.existsByChannelAndInvitedUserAndStatus(channel, invitedUser, InviteStatus.PENDING)) {
            throw new IllegalStateException("Invite already pending");
        }

        ChannelInvite invite = new ChannelInvite();
        invite.setChannel(channel);
        invite.setInvitedUser(invitedUser);
        invite.setInvitedBy(invitedBy);
        invite.setStatus(InviteStatus.PENDING);
        return inviteRepository.save(invite);
    }

    @Transactional
    public void acceptInvite(ChannelInvite invite) {
        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepository.save(invite);
        Channel channel = invite.getChannel();
        channel.getMembers().add(invite.getInvitedUser());
        channelRepository.save(channel);
    }

    @Transactional
    public void declineInvite(ChannelInvite invite) {
        invite.setStatus(InviteStatus.DECLINED);
        inviteRepository.save(invite);
    }
}
