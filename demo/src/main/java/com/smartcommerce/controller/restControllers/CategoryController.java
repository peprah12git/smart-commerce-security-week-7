package com.smartcommerce.controller.restControllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcommerce.dtos.request.CreateCategoryDTO;
import com.smartcommerce.dtos.request.UpdateCategoryDTO;
import com.smartcommerce.exception.ErrorResponse;
import com.smartcommerce.exception.ValidationErrorResponse;
import com.smartcommerce.model.Category;
import com.smartcommerce.service.imp.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for Category management
 * Handles HTTP requests for category CRUD operations
 * Base URL: /api/categories
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Category management API — CRUD operations for product categories")
public class CategoryController {

    private final CategoryService categoryService;

    // Manual constructor for compatibility
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Create a new category
     * POST /api/categories
     */
    @Operation(summary = "Create a new category", description = "Creates a new product category with optional parent category")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Category name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<Category> createCategory(
            @Valid @RequestBody CreateCategoryDTO createCategoryDTO) {

        Category category = new Category(
                createCategoryDTO.categoryName(),
                createCategoryDTO.description()
        );


        Category createdCategory = categoryService.createCategory(category);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCategory);
    }

    /**
     * Update category
     * PUT /api/categories/{id}
     */
    @Operation(summary = "Update a category", description = "Fully updates an existing category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Category name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @Parameter(description = "Category ID to update", required = true, example = "1")
            @PathVariable int id,
            @Valid @RequestBody UpdateCategoryDTO updateCategoryDTO) {

        Category category = new Category();
        category.setCategoryName(updateCategoryDTO.categoryName());
        category.setDescription(updateCategoryDTO.description());

        Category updatedCategory = categoryService.updateCategory(id, category);

        return ResponseEntity.ok(updatedCategory);
    }

    /**
     * Delete category
     * DELETE /api/categories/{id}
     */
    @Operation(summary = "Delete a category", description = "Permanently deletes a category by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Category ID to delete", required = true, example = "1")
            @PathVariable int id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}