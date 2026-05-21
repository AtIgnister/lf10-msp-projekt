package org.lf10.stimmungsumfrage;

import org.junit.jupiter.api.Test;
import org.lf10.stimmungsumfrage.Config.SecurityConfig;
import org.lf10.stimmungsumfrage.Controllers.ChannelController;
import org.lf10.stimmungsumfrage.Helpers.MockData;
import org.lf10.stimmungsumfrage.Models.*;
import org.lf10.stimmungsumfrage.Repositories.ChannelInviteRepository;
import org.lf10.stimmungsumfrage.Repositories.ChannelRepository;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.lf10.stimmungsumfrage.Services.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = ChannelController.class)
@Import(SecurityConfig.class)
class ChannelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChannelService channelService;

    @MockitoBean
    private ChannelRepository channelRepository;

    @MockitoBean
    private ChannelInviteRepository channelInviteRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void detail_RedirectsIfUserIsNotMember() throws Exception {
        User user = MockData.createMockUser();
        user.setId(1L);

        Channel channel = new Channel();
        channel.setId(10L);
        channel.setChannelType(ChannelType.OPEN);
        channel.setMembers(new java.util.HashSet<>());

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));

        mockMvc.perform(get("/channels/10")
                        .with(user(user))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/channels"));
    }

    @Test
    void detail_ShowsPageForMember() throws Exception {
        User user = MockData.createMockUser();
        user.setId(1L);

        Channel channel = new Channel();
        channel.setId(11L);
        channel.setName("IT Team");
        channel.setChannelType(ChannelType.OPEN);
        channel.setCreator(user);
        channel.getMembers().add(user);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(channelRepository.findById(11L)).thenReturn(Optional.of(channel));
        when(channelService.getFeedback(channel)).thenReturn(List.of());

        mockMvc.perform(get("/channels/11")
                        .with(user(user))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("channels/detail"));
    }

    @Test
    void create_ForwardsCustomEmojiSetToService() throws Exception {
        User user = MockData.createMockUser();
        user.setId(1L);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        mockMvc.perform(post("/channels")
                        .with(user(user.getEmail()))
                        .with(csrf())
                        .param("name", "IT Team")
                        .param("description", "Daily sync")
                        .param("channelType", "open")
                        .param("feedbackScaleSize", "2")
                        .param("emojiBad", "😬")
                        .param("emojiGood", "🙂"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/channels"));

        verify(channelService, times(1)).createChannel(
                eq("IT Team"),
                eq("Daily sync"),
                eq(ChannelType.OPEN),
                eq(user),
                eq(2),
                isNull(),
                eq("😬"),
                isNull(),
                eq("🙂"),
                isNull()
        );
    }

    @Test
    void sendFeedback_DoesNotPersistWhenEmojiAndCommentAreBlank() throws Exception {
        User user = MockData.createMockUser();
        user.setId(1L);

        Channel channel = new Channel();
        channel.setId(12L);
        channel.setChannelType(ChannelType.OPEN);
        channel.getMembers().add(user);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(channelRepository.findById(12L)).thenReturn(Optional.of(channel));

        mockMvc.perform(post("/channels/12/feedback")
                        .with(user(user))
                        .with(csrf())
                        .param("emoji", "")
                        .param("comment", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/channels/12"));

        verify(channelService, never()).sendFeedback(any(Channel.class), any(User.class), anyString(), anyString());
    }

    @Test
    void acceptInvite_RedirectsIfAuthenticatedUserIsNotInviteOwner() throws Exception {
        User loggedInUser = MockData.createMockUser();
        loggedInUser.setId(1L);

        User invitedUser = MockData.createMockUser();
        invitedUser.setId(2L);

        ChannelInvite invite = new ChannelInvite();
        invite.setId(5L);
        invite.setInvitedUser(invitedUser);
        invite.setStatus(InviteStatus.PENDING);

        when(userRepository.findByEmail(loggedInUser.getEmail())).thenReturn(Optional.of(loggedInUser));
        when(channelInviteRepository.findById(5L)).thenReturn(Optional.of(invite));

        mockMvc.perform(post("/channels/invites/5/accept")
                        .with(user(loggedInUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/channels"));

        verify(channelService, never()).acceptInvite(any(ChannelInvite.class));
    }

    @Test
    void declineInvite_RedirectsIfAuthenticatedUserIsNotInviteOwner() throws Exception {
        User loggedInUser = MockData.createMockUser();
        loggedInUser.setId(1L);

        User invitedUser = MockData.createMockUser();
        invitedUser.setId(2L);

        ChannelInvite invite = new ChannelInvite();
        invite.setId(6L);
        invite.setInvitedUser(invitedUser);
        invite.setStatus(InviteStatus.PENDING);

        when(userRepository.findByEmail(loggedInUser.getEmail())).thenReturn(Optional.of(loggedInUser));
        when(channelInviteRepository.findById(6L)).thenReturn(Optional.of(invite));

        mockMvc.perform(post("/channels/invites/6/decline")
                        .with(user(loggedInUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/channels"));

        verify(channelService, never()).declineInvite(any(ChannelInvite.class));
    }

    @Test
    void acceptInvite_CallsServiceForInviteOwner() throws Exception {
        User invitedUser = MockData.createMockUser();
        invitedUser.setId(7L);

        ChannelInvite invite = new ChannelInvite();
        invite.setId(7L);
        invite.setInvitedUser(invitedUser);
        invite.setStatus(InviteStatus.PENDING);

        when(userRepository.findByEmail(invitedUser.getEmail())).thenReturn(Optional.of(invitedUser));
        when(channelInviteRepository.findById(7L)).thenReturn(Optional.of(invite));

        mockMvc.perform(post("/channels/invites/7/accept")
                        .with(user(invitedUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/channels"));

        verify(channelService, times(1)).acceptInvite(invite);
    }

    @Test
    void declineInvite_CallsServiceForInviteOwner() throws Exception {
        User invitedUser = MockData.createMockUser();
        invitedUser.setId(8L);

        ChannelInvite invite = new ChannelInvite();
        invite.setId(8L);
        invite.setInvitedUser(invitedUser);
        invite.setStatus(InviteStatus.PENDING);

        when(userRepository.findByEmail(invitedUser.getEmail())).thenReturn(Optional.of(invitedUser));
        when(channelInviteRepository.findById(8L)).thenReturn(Optional.of(invite));

        mockMvc.perform(post("/channels/invites/8/decline")
                        .with(user(invitedUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/channels"));

        verify(channelService, times(1)).declineInvite(invite);
    }

    @Test
    void anonymousGetDetail_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/channels/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login"));
    }

    @Test
    void postFeedbackWithoutCsrf_IsForbidden() throws Exception {
        mockMvc.perform(post("/channels/12/feedback")
                        .with(user("john.doe@example.com"))
                        .param("emoji", "🙂")
                        .param("comment", "Test"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(channelService);
    }
}



