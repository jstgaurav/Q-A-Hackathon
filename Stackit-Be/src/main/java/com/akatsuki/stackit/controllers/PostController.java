package com.akatsuki.stackit.controllers;

import com.akatsuki.stackit.domain.CreatePostRequest;
import com.akatsuki.stackit.domain.UpdatePostRequest;
import com.akatsuki.stackit.domain.dto.CreatePostRequestDto;
import com.akatsuki.stackit.domain.dto.PostDto;
import com.akatsuki.stackit.domain.dto.UpdatePostRequestDto;
import com.akatsuki.stackit.domain.entities.Post;
import com.akatsuki.stackit.domain.entities.User;
import com.akatsuki.stackit.mappers.PostMapper;
import com.akatsuki.stackit.service.PostService;
import com.akatsuki.stackit.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostMapper postMapper;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<PostDto>> getAllPosts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID tagId) {
        List<Post> posts = postService.getAllPosts(categoryId, tagId);
        List<PostDto> postDtos = posts.stream().map(postMapper::toDto).toList();
        return ResponseEntity.ok(postDtos);
    }

    @GetMapping(path = "/me")
    public ResponseEntity<List<PostDto>> getCurrentUserPosts(
            @RequestAttribute UUID userId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID tagId) {
        User loggedInUser = userService.getUserById(userId);
        List<Post> userPosts = postService.getUserPosts(loggedInUser, categoryId, tagId);
        List<PostDto> userPostDtos = userPosts.stream().map(postMapper::toDto).toList();
        return ResponseEntity.ok(userPostDtos);
    }

    @GetMapping(path = "/drafts")
    public ResponseEntity<List<PostDto>> getDrafts(@RequestAttribute UUID userId) {
        User loggedInUser = userService.getUserById(userId);
        List<Post> draftPost = postService.getDraftPosts(loggedInUser);
        List<PostDto> postDtos = draftPost.stream()
                .map(postMapper::toDto)
                .toList();
        return ResponseEntity.ok(postDtos);
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(
            @RequestAttribute UUID userId,
            @Valid @RequestBody CreatePostRequestDto createPostRequestDto) {
        User loggedInUser = userService.getUserById(userId);
        CreatePostRequest createPostRequest = postMapper.toCreatePostRequest(createPostRequestDto);

        Post createdPost = postService.createPost(loggedInUser, createPostRequest);
        PostDto createdPostDto = postMapper.toDto(createdPost);
        return new ResponseEntity<>(createdPostDto, HttpStatus.CREATED);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<PostDto> updatePost(
            @RequestAttribute UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequestDto updatePostRequestDto) {
        Post existingPost = postService.getPostById(id);

        if(!existingPost.getAuthor().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UpdatePostRequest updatePostRequest = postMapper.toUpdatePostRequest(updatePostRequestDto);
        Post updatePost = postService.updatePost(id, updatePostRequest);
        PostDto updatePostDto = postMapper.toDto(updatePost);

        return ResponseEntity.ok(updatePostDto);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<PostDto> getPost(
            @RequestAttribute UUID userId,
            @PathVariable UUID id) {
        Post existingPost = postService.getPostById(id);

        if (!existingPost.getAuthor().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PostDto postDto = postMapper.toDto(existingPost);
        return ResponseEntity.ok(postDto);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deletePost(
            @RequestAttribute UUID userId,
            @PathVariable UUID id) {
        Post existingPost = postService.getPostById(id);

        if (!existingPost.getAuthor().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
