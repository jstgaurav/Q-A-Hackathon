package com.akatsuki.stackit.service.impl;

import com.akatsuki.stackit.domain.CreatePostRequest;
import com.akatsuki.stackit.domain.PostStatus;
import com.akatsuki.stackit.domain.UpdatePostRequest;
import com.akatsuki.stackit.domain.entities.Category;
import com.akatsuki.stackit.domain.entities.Post;
import com.akatsuki.stackit.domain.entities.Tag;
import com.akatsuki.stackit.domain.entities.User;
import com.akatsuki.stackit.repositories.PostRepository;
import com.akatsuki.stackit.service.CategoryService;
import com.akatsuki.stackit.service.PostService;
import com.akatsuki.stackit.service.TagService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CategoryService categoryService;
    private final TagService tagService;

    private static final int WORDS_PER_MINUTE = 200;

    @Override
    public Post getPostById(UUID id) {
        return postRepository.findById(id).orElseThrow(() -> {
            return new EntityNotFoundException("Post not found with id " + id);
        });
    }

    @Transactional(readOnly = true)
    @Override
    public List<Post> getAllPosts(UUID categoryId, UUID tagId) {
        if (categoryId != null && tagId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            Tag tag = tagService.getTagById(tagId);
            return postRepository.findAllByStatusAndCategoryAndTagsContaining(
                    PostStatus.PUBLISHED,
                    category,
                    tag
            );
        }

        if (categoryId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            return postRepository.findAllByStatusAndCategory(
                    PostStatus.PUBLISHED,
                    category
            );
        }

        if (tagId != null) {
            Tag tag = tagService.getTagById(tagId);
            return postRepository.findAllByStatusAndTagsContaining(
                    PostStatus.PUBLISHED,
                    tag
            );
        }

        return postRepository.findAllByStatus(PostStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Post> getUserPosts(User loggedInUser, UUID categoryId, UUID tagId) {
        if(categoryId != null && tagId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            Tag tag = tagService.getTagById(tagId);

            return postRepository.findAllByAuthorAndStatusAndCategoryAndTagsContaining(
                    loggedInUser,
                    PostStatus.PUBLISHED,
                    category,
                    tag
            );
        }

        if (categoryId != null) {
            Category category = categoryService.getCategoryById(categoryId);
            return postRepository.findAllByAuthorAndStatusAndCategory(
                    loggedInUser,
                    PostStatus.PUBLISHED,
                    category
            );
        }

        if (tagId != null) {
            Tag tag = tagService.getTagById(tagId);
            return postRepository.findAllByAuthorAndStatusAndTagsContaining(
                    loggedInUser,
                    PostStatus.PUBLISHED,
                    tag
            );
        }
        return postRepository.findAllByAuthorAndStatus(loggedInUser, PostStatus.PUBLISHED);
    }

    @Override
    public List<Post> getDraftPosts(User loggedInUser) {
        return postRepository.findAllByAuthorAndStatus(loggedInUser, PostStatus.DRAFT);
    }

    @Override
    @Transactional
    public Post createPost(User loggedInUser, CreatePostRequest createPostRequest) {
        Post newPost = new Post();
        newPost.setTitle(createPostRequest.getTitle());
        String postContent = createPostRequest.getContent();
        newPost.setContent(postContent);
        newPost.setStatus(createPostRequest.getStatus());
        newPost.setAuthor(loggedInUser);
        newPost.setReadingTime(calculateReadingTime(postContent));

        Category category = categoryService.getCategoryById(createPostRequest.getCategoryId());
        newPost.setCategory(category);

        Set<UUID> tagIds = createPostRequest.getTagIds();
        List<Tag> tags = tagService.getTagByIds(tagIds);
        newPost.setTags(new HashSet<>(tags));

        return postRepository.save(newPost);
    }

    @Override
    public Post updatePost(UUID id, UpdatePostRequest updatePostRequest) {
        Post existingPost = getPostById(id);

        existingPost.setTitle(updatePostRequest.getTitle());
        String postContent = updatePostRequest.getContent();
        existingPost.setContent(postContent);
        existingPost.setStatus(updatePostRequest.getStatus());
        existingPost.setReadingTime(calculateReadingTime(postContent));

        UUID updatedPostRequestCategoryId = updatePostRequest.getCategoryId();
        if(!existingPost.getCategory().getId().equals(updatedPostRequestCategoryId)) {
            Category newCategory = categoryService.getCategoryById(updatedPostRequestCategoryId);
            existingPost.setCategory(newCategory);
        }

        Set<UUID> existingTagsId = existingPost.getTags().stream().map(tag -> tag.getId()).collect(Collectors.toSet());
        Set<UUID> updatePostRequestTagsId = updatePostRequest.getTagIds();
        if(existingTagsId.equals(updatePostRequestTagsId)) {
            List<Tag> tags = tagService.getTagByIds(new HashSet<>(updatePostRequestTagsId));
            existingPost.setTags(new HashSet<>(tags));
        }

        return postRepository.save(existingPost);
    }

    @Override
    public void deletePost(UUID id) {
        Post post = getPostById(id);
        postRepository.delete(post);
    }

    private Integer calculateReadingTime(String content) {
        if(null == content || content.isEmpty()) return 0;
        int wordCount = content.trim().split("\\s+").length;
        return (int) Math.ceil((double) wordCount / WORDS_PER_MINUTE);
    }
}
