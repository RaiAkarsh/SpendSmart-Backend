package com.spendsmart.category.serviceimpl;

import com.spendsmart.category.entity.Category;
import com.spendsmart.category.repository.CategoryRepository;
import com.spendsmart.category.service.CategoryService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    //createCategory

    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public Category createCategory(Category category) {
        boolean exists = categoryRepository
                .findByUserIdAndName(category.getUserId(), category.getName())
                .isPresent();

        if (exists) {
            throw new RuntimeException(
                "Category '" + category.getName() + "' already exists for this user"
            );
        }
  
        category.setDefault(false);   
        return categoryRepository.save(category);
    }

    //getByUserId
    @Override
    @Cacheable(cacheNames = "categories", key = "'user:' + #userId")
    public List<Category> getByUserId(int userId) {
        return categoryRepository.findByUserId(userId);
    }

    //getCategoryById
    @Override
    @Cacheable(cacheNames = "categories", key = "'id:' + #categoryId")
    public Category getCategoryById(int categoryId) {
        return categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new RuntimeException(
                    "Category not found with id: " + categoryId
                ));
    }

    //getByUserAndType
    @Override
    @Cacheable(cacheNames = "categories", key = "'user:' + #userId + ':type:' + #type.toUpperCase()")
    public List<Category> getByUserAndType(int userId, String type) {
        return categoryRepository.findByUserIdAndType(userId, type.toUpperCase());
    }

    //updateCategory
    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public Category updateCategory(int categoryId, Category updatedCategory) {
        Category existing = getCategoryById(categoryId);

        // Default categories: cannot rename, but CAN edit budget, type, icon, color
        if (existing.isDefault() && updatedCategory.getName() != null
                && !updatedCategory.getName().equals(existing.getName())) {
            throw new RuntimeException(
                "Cannot rename default category: " + existing.getName()
            );
        }

        // Update name only for non-default categories
        if (!existing.isDefault() && updatedCategory.getName() != null) {
            existing.setName(updatedCategory.getName());
        }

        // These fields are editable for ALL categories (default + custom)
        if (updatedCategory.getType() != null) {
            existing.setType(updatedCategory.getType().toUpperCase());
        }
        if (updatedCategory.getColorCode() != null) {
            existing.setColorCode(updatedCategory.getColorCode());
        }
        if (updatedCategory.getIcon() != null) {
            existing.setIcon(updatedCategory.getIcon());
        }
        if (updatedCategory.getBudgetLimit() >= 0) {
            existing.setBudgetLimit(updatedCategory.getBudgetLimit());
        }

        return categoryRepository.save(existing);
    }

    //deleteCategory
    @Override
    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void deleteCategory(int categoryId) {
        Category category = getCategoryById(categoryId);

        if (category.isDefault()) {
            throw new RuntimeException(
                "Cannot delete default category: " + category.getName()
            );
        }

        categoryRepository.deleteByCategoryId(categoryId);
    }

    // initDefaultCategories
    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void initDefaultCategories(int userId) {
        List<Category> existing = categoryRepository.findByUserIdAndIsDefault(userId, true);
        if (!existing.isEmpty()) {
            throw new RuntimeException("Default categories already initialized for userId: " + userId);
        }

        List<String[]> defaults = Arrays.asList(
                // EXPENSE categories
                new String[]{"Food",           "EXPENSE", "", "#FF6B6B"},
                new String[]{"Transport",      "EXPENSE", "", "#4ECDC4"},
                new String[]{"Shopping",       "EXPENSE", "", "#45B7D1"},
                new String[]{"Bills",          "EXPENSE", "", "#96CEB4"},
                new String[]{"Health",         "EXPENSE", "", "#FFEAA7"},
                new String[]{"Entertainment",  "EXPENSE", "", "#DDA0DD"},
                new String[]{"Education",      "EXPENSE", "", "#98D8C8"},
                new String[]{"Other Expense",  "EXPENSE", "", "#B0BEC5"},

                // INCOME categories
                new String[]{"Salary",         "INCOME",  "", "#66BB6A"},
                new String[]{"Freelance",      "INCOME",  "", "#42A5F5"},
                new String[]{"Investment",     "INCOME",  "", "#FFA726"},
                new String[]{"Other Income",   "INCOME",  "", "#AB47BC"}
        );

        for (String[] d : defaults) {
            Category cat = new Category();
            cat.setUserId(userId);
            cat.setName(d[0]);
            cat.setType(d[1]);
            cat.setIcon(d[2]);
            cat.setColorCode(d[3]);
            cat.setDefault(true);       
            cat.setBudgetLimit(0.0);    
            categoryRepository.save(cat);
        }
    }

    //setCategoryBudget
    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public void setCategoryBudget(int categoryId, double budgetLimit) {
        Category category = getCategoryById(categoryId);
        category.setBudgetLimit(budgetLimit);
        categoryRepository.save(category);
    }

    //getCategoryCount
    @Override
    @Cacheable(cacheNames = "categories", key = "'count:' + #userId")
    public int getCategoryCount(int userId) {
        return categoryRepository.countByUserId(userId);
    }
}
