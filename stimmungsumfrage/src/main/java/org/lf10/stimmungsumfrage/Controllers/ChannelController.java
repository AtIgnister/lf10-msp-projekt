package org.lf10.stimmungsumfrage.Controllers;

import lombok.RequiredArgsConstructor;
import org.lf10.stimmungsumfrage.Models.*;
import org.lf10.stimmungsumfrage.Repositories.ChannelInviteRepository;
import org.lf10.stimmungsumfrage.Repositories.ChannelRepository;
import org.lf10.stimmungsumfrage.Repositories.UserRepository;
import org.lf10.stimmungsumfrage.Services.ChannelService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final ChannelRepository channelRepository;
    private final ChannelInviteRepository inviteRepository;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    @GetMapping
    public String index(Authentication auth, Model model) {
        User user = getAuthenticatedUser(auth);
        model.addAttribute("myChannels", channelService.getUserChannels(user));
        model.addAttribute("availableChannels", channelService.getAvailableChannels(user));
        model.addAttribute("pendingInvites", channelService.getPendingInvites(user));
        model.addAttribute("isAdmin", user.getRole().getName().equals("ROLE_ADMIN"));
        return "channels/index";
    }

    @GetMapping("/new")
    public String createForm() {
        return "channels/create";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam String channelType,
                         Authentication auth) {
        User user = getAuthenticatedUser(auth);
        ChannelType type = channelType.equals("invite-only") ? ChannelType.INVITE_ONLY : ChannelType.OPEN;
        channelService.createChannel(name, description, type, user);
        return "redirect:/channels";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        User user = getAuthenticatedUser(auth);
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Channel not found"));

        if (!channel.getMembers().contains(user)) {
            return "redirect:/channels";
        }

        model.addAttribute("channel", channel);
        model.addAttribute("messages", channelService.getMessages(channel));
        model.addAttribute("feedbackList", channelService.getFeedback(channel));
        model.addAttribute("members", channel.getMembers());
        model.addAttribute("currentUser", user);

        if (channel.getChannelType() == ChannelType.INVITE_ONLY) {
            model.addAttribute("pendingInvites", channelService.getChannelPendingInvites(channel));
            List<User> allUsers = userRepository.findAll();
            List<User> invitableUsers = allUsers.stream()
                    .filter(u -> !channel.getMembers().contains(u))
                    .filter(u -> !inviteRepository.existsByChannelAndInvitedUserAndStatus(
                            channel, u, InviteStatus.PENDING))
                    .toList();
            model.addAttribute("invitableUsers", invitableUsers);
        }

        return "channels/detail";
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Channel not found"));
        channelService.joinChannel(channel, user);
        return "redirect:/channels";
    }

    @PostMapping("/{id}/leave")
    public String leave(@PathVariable Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Channel not found"));
        channelService.leaveChannel(channel, user);
        return "redirect:/channels";
    }

    @PostMapping("/{id}/messages")
    public String sendMessage(@PathVariable Long id,
                              @RequestParam String content,
                              Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Channel not found"));

        if (!channel.getMembers().contains(user)) {
            return "redirect:/channels";
        }

        channelService.sendMessage(channel, user, content);
        return "redirect:/channels/" + id;
    }

    @PostMapping("/{id}/feedback")
    public String sendFeedback(@PathVariable Long id,
                               @RequestParam(required = false) String emoji,
                               @RequestParam(required = false) String comment,
                               Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Channel not found"));

        if (!channel.getMembers().contains(user)) {
            return "redirect:/channels";
        }

        if ((emoji != null && !emoji.isBlank()) || (comment != null && !comment.isBlank())) {
            channelService.sendFeedback(channel, user, emoji, comment);
        }
        return "redirect:/channels/" + id;
    }

    @PostMapping("/{id}/invite")
    public String invite(@PathVariable Long id,
                         @RequestParam Long userId,
                         Authentication auth) {
        User user = getAuthenticatedUser(auth);
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Channel not found"));
        User invitedUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        channelService.createInvite(channel, invitedUser, user);
        return "redirect:/channels/" + id;
    }

    @PostMapping("/invites/{inviteId}/accept")
    public String acceptInvite(@PathVariable Long inviteId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        ChannelInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalStateException("Invite not found"));

        if (!invite.getInvitedUser().getId().equals(user.getId())) {
            return "redirect:/channels";
        }

        channelService.acceptInvite(invite);
        return "redirect:/channels";
    }

    @PostMapping("/invites/{inviteId}/decline")
    public String declineInvite(@PathVariable Long inviteId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        ChannelInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalStateException("Invite not found"));

        if (!invite.getInvitedUser().getId().equals(user.getId())) {
            return "redirect:/channels";
        }

        channelService.declineInvite(invite);
        return "redirect:/channels";
    }
}
