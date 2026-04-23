package org.lf10.stimmungsumfrage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lf10.stimmungsumfrage.Models.*;
import org.lf10.stimmungsumfrage.Repositories.ChannelFeedbackRepository;
import org.lf10.stimmungsumfrage.Repositories.ChannelInviteRepository;
import org.lf10.stimmungsumfrage.Repositories.ChannelRepository;
import org.lf10.stimmungsumfrage.Services.ChannelService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelFeedbackRepository feedbackRepository;

    @Mock
    private ChannelInviteRepository inviteRepository;

    @InjectMocks
    private ChannelService channelService;

    @Test
    void createChannel_SetsFieldsAndAddsCreatorAsMember() {
        User creator = new User();
        creator.setId(1L);

        when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> inv.getArgument(0));

        Channel channel = channelService.createChannel("IT Team", "Daily sync", ChannelType.OPEN, creator);

        assertEquals("IT Team", channel.getName());
        assertEquals("Daily sync", channel.getDescription());
        assertEquals(ChannelType.OPEN, channel.getChannelType());
        assertSame(creator, channel.getCreator());
        assertTrue(channel.getMembers().contains(creator));
        verify(channelRepository, times(1)).save(channel);
    }

    @Test
    void joinChannel_AddsUserAndSaves() {
        Channel channel = new Channel();
        User user = new User();

        channelService.joinChannel(channel, user);

        assertTrue(channel.getMembers().contains(user));
        verify(channelRepository, times(1)).save(channel);
    }

    @Test
    void leaveChannel_RemovesUserAndSaves() {
        Channel channel = new Channel();
        User user = new User();
        channel.getMembers().add(user);

        channelService.leaveChannel(channel, user);

        assertFalse(channel.getMembers().contains(user));
        verify(channelRepository, times(1)).save(channel);
    }

    @Test
    void sendFeedback_PersistsEmojiAndComment() {
        Channel channel = new Channel();
        User user = new User();

        when(feedbackRepository.save(any(ChannelFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelFeedback saved = channelService.sendFeedback(channel, user, "🙂", "Looks good");

        assertSame(channel, saved.getChannel());
        assertSame(user, saved.getUser());
        assertEquals("🙂", saved.getEmoji());
        assertEquals("Looks good", saved.getComment());
        verify(feedbackRepository, times(1)).save(any(ChannelFeedback.class));
    }

    @Test
    void getPendingInvites_UsesPendingStatus() {
        User user = new User();
        List<ChannelInvite> invites = List.of(new ChannelInvite());

        when(inviteRepository.findByInvitedUserAndStatus(user, InviteStatus.PENDING)).thenReturn(invites);

        List<ChannelInvite> result = channelService.getPendingInvites(user);

        assertEquals(invites, result);
        verify(inviteRepository, times(1)).findByInvitedUserAndStatus(user, InviteStatus.PENDING);
    }

    @Test
    void createInvite_ThrowsIfUserAlreadyMember() {
        User invited = new User();
        User invitedBy = new User();
        Channel channel = new Channel();
        channel.getMembers().add(invited);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> channelService.createInvite(channel, invited, invitedBy));

        assertEquals("User is already a member", ex.getMessage());
        verify(inviteRepository, never()).save(any(ChannelInvite.class));
    }

    @Test
    void createInvite_ThrowsIfPendingInviteExists() {
        User invited = new User();
        User invitedBy = new User();
        Channel channel = new Channel();

        when(inviteRepository.existsByChannelAndInvitedUserAndStatus(channel, invited, InviteStatus.PENDING))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> channelService.createInvite(channel, invited, invitedBy));

        assertEquals("Invite already pending", ex.getMessage());
        verify(inviteRepository, never()).save(any(ChannelInvite.class));
    }

    @Test
    void createInvite_SavesPendingInvite() {
        User invited = new User();
        User invitedBy = new User();
        Channel channel = new Channel();

        when(inviteRepository.existsByChannelAndInvitedUserAndStatus(channel, invited, InviteStatus.PENDING))
                .thenReturn(false);
        when(inviteRepository.save(any(ChannelInvite.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelInvite invite = channelService.createInvite(channel, invited, invitedBy);

        assertSame(channel, invite.getChannel());
        assertSame(invited, invite.getInvitedUser());
        assertSame(invitedBy, invite.getInvitedBy());
        assertEquals(InviteStatus.PENDING, invite.getStatus());
        verify(inviteRepository, times(1)).save(any(ChannelInvite.class));
    }

    @Test
    void acceptInvite_MarksAcceptedAndAddsMemberToChannel() {
        User invited = new User();
        Channel channel = new Channel();
        ChannelInvite invite = new ChannelInvite();
        invite.setChannel(channel);
        invite.setInvitedUser(invited);
        invite.setStatus(InviteStatus.PENDING);

        channelService.acceptInvite(invite);

        assertEquals(InviteStatus.ACCEPTED, invite.getStatus());
        assertTrue(channel.getMembers().contains(invited));
        verify(inviteRepository, times(1)).save(invite);
        verify(channelRepository, times(1)).save(channel);
    }

    @Test
    void declineInvite_MarksDeclinedAndSavesInvite() {
        ChannelInvite invite = new ChannelInvite();
        invite.setStatus(InviteStatus.PENDING);

        channelService.declineInvite(invite);

        assertEquals(InviteStatus.DECLINED, invite.getStatus());
        verify(inviteRepository, times(1)).save(invite);
        verify(channelRepository, never()).save(any(Channel.class));
    }
}

