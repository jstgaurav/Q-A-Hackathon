package com.akatsuki.stackit.mappers;

import com.akatsuki.stackit.domain.PostStatus;
import com.akatsuki.stackit.domain.dto.CategoryDto;
import com.akatsuki.stackit.domain.dto.CreateCategoryRequest;
import com.akatsuki.stackit.domain.entities.Category;
import com.akatsuki.stackit.domain.entities.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "postCount", source = "posts", qualifiedByName = "calculatePostCount")
    CategoryDto toDto(Category category);

    Category toEntity(CreateCategoryRequest createCategoryRequest);

    @Named("calculatePostCount")
    default long calculatePostCount(List<Post> posts) {
        if(null == posts) return 0;
        return posts.stream()
                .filter(post -> PostStatus.PUBLISHED.equals(post.getStatus()))
                .count();
    }
}
