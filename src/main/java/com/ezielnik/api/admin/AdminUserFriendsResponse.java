package com.ezielnik.api.admin;

import com.ezielnik.api.friend.FriendResponse;

import java.util.List;

public record AdminUserFriendsResponse(
        List<FriendResponse> accepted,
        List<FriendResponse> incoming,
        List<FriendResponse> sent
) {}
