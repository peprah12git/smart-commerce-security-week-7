package com.smartcommerce.service.imp;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcommerce.exception.BusinessException;
import com.smartcommerce.exception.DuplicateResourceException;
import com.smartcommerce.exception.ResourceNotFoundException;
import com.smartcommerce.model.Category;
import com.smartcommerce.model.Product;
import com.smartcommerce.repositories.CategoryRepository;
import com.smartcommerce.repositories.ProductRepository;
import com.smartcommerce.service.serviceInterface.CategoryServiceInterface;

/**
 * Service layer for Category entity
 * Handles business logic, validation, and orchestration of category operations
 */
@Service
@Transactional
public class CategoryService implements CategoryServiceInterface {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Creates a new category
     *
     * @param category Category object to create
     * @return Created category
     * @throws DuplicateResourceException if category name already exists
     * @throws BusinessException          if category creation fails
     */
    public Category createCategory(Category category) {
        // Validate input
        validateCategory(category);

        // Check for duplicate category name
        List<Category> existingCategories = categoryRepository.findAll();
        boolean nameExists = existingCategories.stream()
                .anyMatch(c -> c.getCategoryName().equalsIgnoreCase(category.getCategoryName()));

        if (nameExists) {
            throw new DuplicateResourceException("Category", "name", category.getCategoryName());
        }

        // Save and return category
        return categoryRepository.save(category);
    }

    /**
     * Retrieves all categories
     *
     * @return List of all categories
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Retrieves a category by ID
     *
     * @param categoryId Category ID
     * @return Category object
     * @throws ResourceNotFoundException if category not found
     */
    @Transactional(readOnly = true)
    public Category getCategoryById(int categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
    }

    /**
     * Retrieves a category by name
     *
     * @param categoryName Category name
     * @return Category object
     * @throws ResourceNotFoundException if category not found
     */
    @Transactional(readOnly = true)
    public Category getCategoryByName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new BusinessException("Category name cannot be empty");
        }

        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .filter(c -> c.getCategoryName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Category", "name", categoryName));
    }

    /**
     * Updates an existing category
     *
     * @param categoryId      Category ID to update
     * @param categoryDetails Updated category details
     * @return Updated category
     * @throws ResourceNotFoundException  if category not found
     * @throws DuplicateResourceException if category name already exists
     * @throws BusinessException          if update fails
     */
    public Category updateCategory(int categoryId, Category categoryDetails) {
        // Check if category exists
        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        // Validate updated details
        validateCategory(categoryDetails);

        // Check for duplicate name (if name is being changed)
        if (!existingCategory.getCategoryName().equalsIgnoreCase(categoryDetails.getCategoryName())) {
            List<Category> categories = categoryRepository.findAll();
            boolean nameExists = categories.stream()
                    .anyMatch(c -> c.getCategoryId() != categoryId
                            && c.getCategoryName().equalsIgnoreCase(categoryDetails.getCategoryName()));

            if (nameExists) {
                throw new DuplicateResourceException("Category", "name", categoryDetails.getCategoryName());
            }
        }

        // Update category details
        existingCategory.setCategoryName(categoryDetails.getCategoryName());
        existingCategory.setDescription(categoryDetails.getDescription());

        return categoryRepository.save(existingCategory);
    }

    /**
     * Deletes a category
     *
     * @param categoryId Category ID to delete
     * @throws ResourceNotFoundException if category not found
     * @throws BusinessException         if category has products or deletion fails
     */
    public void deleteCategory(int categoryId) {
        // Check if category exists
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        // Check if category has products
        List<Product> productsInCategory = productRepository.findByCategoryName(category.getCategoryName());
        if (productsInCategory != null && !productsInCategory.isEmpty()) {
            throw new BusinessException(
                    String.format("Cannot delete category '%s' because it has %d product(s) associated with it",
                            category.getCategoryName(), productsInCategory.size()));
        }

        categoryRepository.deleteById(categoryId);
    }

    /**
     * Validates category data
     *
     * @param category Category to validate
     * @throws BusinessException if validation fails
     */
    private void validateCategory(Category category) {
        if (category == null) {
            throw new BusinessException("Category cannot be null");
        }

        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new BusinessException("Category name is required");
        }

        if (category.getCategoryName().length() > 100) {
            throw new BusinessException("Category name cannot exceed 100 characters");
        }

        // Description is optional, but if provided, validate length
        if (category.getDescription() != null && category.getDescription().length() > 500) {
            throw new BusinessException("Category description cannot exceed 500 characters");
        }
    }
}