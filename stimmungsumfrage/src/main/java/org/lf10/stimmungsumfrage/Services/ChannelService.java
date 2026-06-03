package org.lf10.stimmungsumfrage.Services;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.*;
import org.lf10.stimmungsumfrage.Repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelFeedbackRepository feedbackRepository;
    private final ChannelInviteRepository inviteRepository;
    private final UserChannelFeedbackStatusRepository statusRepository;

    @Transactional
    public Channel createChannel(String name, String description, ChannelType type, User creator,
                                 int feedbackScaleSize,
                                 String emojiVeryBad, String emojiBad, String emojiNeutral,
                                 String emojiGood, String emojiVeryGood) {
        if (feedbackScaleSize < Channel.MIN_FEEDBACK_EMOJI_COUNT || feedbackScaleSize > Channel.MAX_FEEDBACK_EMOJI_COUNT) {
            throw new IllegalArgumentException("Feedback scale must be between 2 and 5 emojis");
        }

        Channel channel = new Channel();
        channel.setName(name);
        channel.setDescription(description);
        channel.setChannelType(type);
        channel.setCreator(creator);
        channel.setFeedbackScaleSize(feedbackScaleSize);

        applyFeedbackEmojis(channel, feedbackScaleSize, emojiVeryBad, emojiBad, emojiNeutral, emojiGood, emojiVeryGood);
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

    private void applyFeedbackEmojis(Channel channel, int feedbackScaleSize,
                                     String emojiVeryBad, String emojiBad, String emojiNeutral,
                                     String emojiGood, String emojiVeryGood) {
        switch (feedbackScaleSize) {
            case 2 -> {
                channel.setEmojiBad(normalizeEmoji(emojiBad, Channel.DEFAULT_EMOJI_BAD));
                channel.setEmojiGood(normalizeEmoji(emojiGood, Channel.DEFAULT_EMOJI_GOOD));
            }
            case 3 -> {
                channel.setEmojiBad(normalizeEmoji(emojiBad, Channel.DEFAULT_EMOJI_BAD));
                channel.setEmojiNeutral(normalizeEmoji(emojiNeutral, Channel.DEFAULT_EMOJI_NEUTRAL));
                channel.setEmojiGood(normalizeEmoji(emojiGood, Channel.DEFAULT_EMOJI_GOOD));
            }
            case 4 -> {
                channel.setEmojiVeryBad(normalizeEmoji(emojiVeryBad, Channel.DEFAULT_EMOJI_VERY_BAD));
                channel.setEmojiBad(normalizeEmoji(emojiBad, Channel.DEFAULT_EMOJI_BAD));
                channel.setEmojiGood(normalizeEmoji(emojiGood, Channel.DEFAULT_EMOJI_GOOD));
                channel.setEmojiVeryGood(normalizeEmoji(emojiVeryGood, Channel.DEFAULT_EMOJI_VERY_GOOD));
            }
            default -> {
                channel.setEmojiVeryBad(normalizeEmoji(emojiVeryBad, Channel.DEFAULT_EMOJI_VERY_BAD));
                channel.setEmojiBad(normalizeEmoji(emojiBad, Channel.DEFAULT_EMOJI_BAD));
                channel.setEmojiNeutral(normalizeEmoji(emojiNeutral, Channel.DEFAULT_EMOJI_NEUTRAL));
                channel.setEmojiGood(normalizeEmoji(emojiGood, Channel.DEFAULT_EMOJI_GOOD));
                channel.setEmojiVeryGood(normalizeEmoji(emojiVeryGood, Channel.DEFAULT_EMOJI_VERY_GOOD));
            }
        }
    }

    private String normalizeEmoji(String emoji, String defaultEmoji) {
        String result = (emoji == null || emoji.isBlank()) ? defaultEmoji : emoji;
        if (result != null && result.length() > 32) {
            return result.substring(0, 32);
        }
        return result;
    }

    public boolean canSubmitFeedback(User user, Channel channel) {
        return statusRepository
                .findByUserAndChannel(user, channel)
                .map(status ->
                        status.getLastSubmission() == null
                                || status.getLastSubmission()
                                .isBefore(LocalDateTime.now().minusDays(1)))
                .orElse(true);
    }

    public void updateLastSubmitted(User user, Channel channel) {
        UserChannelFeedbackStatus status =
                statusRepository
                        .findByUserAndChannel(user, channel)
                        .orElseGet(() -> {
                            UserChannelFeedbackStatus s =
                                    new UserChannelFeedbackStatus();
                            s.setUser(user);
                            s.setChannel(channel);
                            return s;
                        });

        status.setLastSubmission(LocalDateTime.now());

        statusRepository.save(status);
    }
}
