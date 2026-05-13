package com.spendsmart.category.serviceimpl;

import com.spendsmart.category.client.BudgetClient;
import com.spendsmart.category.dto.BudgetSyncRequest;
import com.spendsmart.category.entity.Category;
import com.spendsmart.category.repository.CategoryRepository;
import com.spendsmart.category.service.CategoryService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import feign.FeignException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetClient budgetClient;

    @Value("${jwt.secret}")
    private String secret;

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
        Category saved = categoryRepository.save(category);
        syncBudgetForCategory(saved);
        return saved;
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

        Category saved = categoryRepository.save(existing);
        syncBudgetForCategory(saved);
        return saved;
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
        Category saved = categoryRepository.save(category);
        syncBudgetForCategory(saved);
    }

    //getCategoryCount
    @Override
    @Cacheable(cacheNames = "categories", key = "'count:' + #userId")
    public int getCategoryCount(int userId) {
        return categoryRepository.countByUserId(userId);
    }

    private void syncBudgetForCategory(Category category) {
        try {
            if (!"EXPENSE".equalsIgnoreCase(category.getType())) {
                return;
            }

            String authorization = buildAuthorizationHeader(category.getUserId());
            Integer existingBudgetId = findActiveBudgetId(category.getUserId(), category.getCategoryId(), authorization);

            if (category.getBudgetLimit() <= 0) {
                if (existingBudgetId != null) {
                    budgetClient.deactivateBudget(existingBudgetId, authorization);
                }
                return;
            }

            BudgetSyncRequest request = new BudgetSyncRequest();
            request.setUserId(category.getUserId());
            request.setCategoryId(category.getCategoryId());
            request.setName(category.getName() + " Budget");
            request.setLimitAmount(category.getBudgetLimit());
            request.setCurrency("INR");
            request.setPeriod("MONTHLY");
            request.setStartDate(LocalDate.now().withDayOfMonth(1));
            request.setEndDate(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
            request.setAlertThreshold(80);

            if (existingBudgetId == null) {
                budgetClient.createBudget(request, authorization);
            } else {
                budgetClient.updateBudget(existingBudgetId, request, authorization);
            }
        } catch (Exception e) {
            System.err.println("Budget sync skipped for categoryId=" + category.getCategoryId()
                    + ": " + e.getMessage());
        }
    }

    private Integer findActiveBudgetId(int userId, int categoryId, String authorization) {
        try {
            Map<String, Object> budget = budgetClient.getActiveBudgetByCategory(userId, categoryId, authorization);
            Object rawBudgetId = budget.get("budgetId");
            if (rawBudgetId instanceof Number number) {
                return number.intValue();
            }
            return null;
        } catch (FeignException.NotFound e) {
            return null;
        }
    }

    private String buildAuthorizationHeader(int userId) {
        return "Bearer " + generateServiceToken(userId);
    }

    private String generateServiceToken(int userId) {
        return Jwts.builder()
                .subject("category-service")
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
