package com.webhook_wrapper.facebook;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class CommentTracker {
    private final Set<String> seenCommentIds = new HashSet<>();

    public boolean isNewComment(String commentId) {
        return seenCommentIds.add(commentId); // true if not seen before
    }
}
