package com.spendsmart.category;

import com.spendsmart.category.client.BudgetClient;
import com.spendsmart.category.entity.Category;
import com.spendsmart.category.repository.CategoryRepository;
import com.spendsmart.category.serviceimpl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Unit Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BudgetClient budgetClient;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category foodCategory;
    private Category gymCategory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(categoryService, "secret", "SpendSmartSecretKey2026_Standard32Bytes!");
        lenient().when(budgetClient.getActiveBudgetByCategory(anyInt(), anyInt(), anyString()))
                .thenReturn(Map.of("budgetId", 101));

        foodCategory = new Category();
        foodCategory.setCategoryId(1);
        foodCategory.setUserId(1);
        foodCategory.setName("Food");
        foodCategory.setType("EXPENSE");
        foodCategory.setIcon("");
        foodCategory.setColorCode("#FF6B6B");
        foodCategory.setDefault(true);   // default  - cannot be deleted

        gymCategory = new Category();
        gymCategory.setCategoryId(13);
        gymCategory.setUserId(1);
        gymCategory.setName("Gym");
        gymCategory.setType("EXPENSE");
        gymCategory.setIcon("");
        gymCategory.setColorCode("#FF8C00");
        gymCategory.setDefault(false);   // custom  - can be deleted
    }


    @Test
    @DisplayName("createCategory: should save and mark isDefault=false")
    void createCategory_shouldSaveWithIsDefaultFalse() {
        when(categoryRepository.findByUserIdAndName(1, "Gym")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(gymCategory);

        Category input = new Category();
        input.setUserId(1);
        input.setName("Gym");
        input.setType("EXPENSE");

        Category result = categoryService.createCategory(input);

        assertNotNull(result);
        // Critical: user-created categories must never be marked as default
        verify(categoryRepository).save(argThat(c -> !c.isDefault()));
    }

    @Test
    @DisplayName("createCategory: should throw when duplicate name exists for same user")
    void createCategory_shouldThrow_whenDuplicateName() {
        when(categoryRepository.findByUserIdAndName(1, "Gym")).thenReturn(Optional.of(gymCategory));

        Category input = new Category();
        input.setUserId(1);
        input.setName("Gym");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> categoryService.createCategory(input));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(categoryRepository, never()).save(any());
    }


    @Test
    @DisplayName("deleteCategory: should delete custom (non-default) category")
    void deleteCategory_shouldDelete_whenCategoryIsCustom() {
        when(categoryRepository.findByCategoryId(13)).thenReturn(Optional.of(gymCategory));

        assertDoesNotThrow(() -> categoryService.deleteCategory(13));
        verify(categoryRepository, times(1)).deleteByCategoryId(13);
    }

    @Test
    @DisplayName("deleteCategory: should throw when trying to delete a default category")
    void deleteCategory_shouldThrow_whenCategoryIsDefault() {
        when(categoryRepository.findByCategoryId(1)).thenReturn(Optional.of(foodCategory));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> categoryService.deleteCategory(1));
        assertTrue(ex.getMessage().contains("Cannot delete default category"));
        verify(categoryRepository, never()).deleteByCategoryId(anyInt());
    }

    @Test
    @DisplayName("deleteCategory: should throw when category not found")
    void deleteCategory_shouldThrow_whenNotFound() {
        when(categoryRepository.findByCategoryId(999)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> categoryService.deleteCategory(999));
    }


    @Test
    @DisplayName("initDefaultCategories: should save exactly 12 categories for new user")
    void initDefaultCategories_shouldSave12Categories_forNewUser() {
        when(categoryRepository.findByUserIdAndIsDefault(1, true)).thenReturn(Collections.emptyList());
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        categoryService.initDefaultCategories(1);

        // 12 categories: 8 EXPENSE + 4 INCOME
        verify(categoryRepository, times(12)).save(any(Category.class));
    }

    @Test
    @DisplayName("initDefaultCategories: should throw when already initialized")
    void initDefaultCategories_shouldThrow_whenAlreadyInitialized() {
        when(categoryRepository.findByUserIdAndIsDefault(1, true))
                .thenReturn(Arrays.asList(foodCategory));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> categoryService.initDefaultCategories(1));
        assertTrue(ex.getMessage().contains("already initialized"));
        verify(categoryRepository, never()).save(any());
    }


    @Test
    @DisplayName("getByUserId: should return all categories for user")
    void getByUserId_shouldReturnAllCategories() {
        when(categoryRepository.findByUserId(1)).thenReturn(Arrays.asList(foodCategory, gymCategory));

        List<Category> result = categoryService.getByUserId(1);

        assertEquals(2, result.size());
        verify(categoryRepository, times(1)).findByUserId(1);
    }

    @Test
    @DisplayName("getByUserAndType: should return only EXPENSE categories")
    void getByUserAndType_shouldFilterByType() {
        when(categoryRepository.findByUserIdAndType(1, "EXPENSE"))
                .thenReturn(Arrays.asList(foodCategory, gymCategory));

        List<Category> result = categoryService.getByUserAndType(1, "EXPENSE");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> "EXPENSE".equals(c.getType())));
    }

    @Test
    @DisplayName("getCategoryById: should return category when found")
    void getCategoryById_shouldReturn_whenFound() {
        when(categoryRepository.findByCategoryId(1)).thenReturn(Optional.of(foodCategory));

        Category result = categoryService.getCategoryById(1);

        assertEquals("Food", result.getName());
    }

    @Test
    @DisplayName("getCategoryById: should throw when not found")
    void getCategoryById_shouldThrow_whenNotFound() {
        when(categoryRepository.findByCategoryId(404)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> categoryService.getCategoryById(404));
    }

    @Test
    @DisplayName("updateCategory: should allow default category budget and style updates")
    void updateCategory_shouldAllowDefaultCategoryNonNameFields() {
        when(categoryRepository.findByCategoryId(1)).thenReturn(Optional.of(foodCategory));
        when(categoryRepository.save(any())).thenReturn(foodCategory);

        Category updates = new Category();
        updates.setName("Food");
        updates.setType("expense");
        updates.setColorCode("#123456");
        updates.setIcon("restaurant");
        updates.setBudgetLimit(2500.0);

        categoryService.updateCategory(1, updates);

        verify(categoryRepository).save(argThat(c ->
                "EXPENSE".equals(c.getType())
                        && "#123456".equals(c.getColorCode())
                        && "restaurant".equals(c.getIcon())
                        && c.getBudgetLimit() == 2500.0));
    }

    @Test
    @DisplayName("updateCategory: should throw when renaming default category")
    void updateCategory_shouldThrow_whenRenamingDefaultCategory() {
        when(categoryRepository.findByCategoryId(1)).thenReturn(Optional.of(foodCategory));

        Category updates = new Category();
        updates.setName("New Food");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> categoryService.updateCategory(1, updates));

        assertTrue(ex.getMessage().contains("Cannot rename default category"));
    }

    @Test
    @DisplayName("getCategoryCount: should return repository count")
    void getCategoryCount_shouldReturnCount() {
        when(categoryRepository.countByUserId(1)).thenReturn(12);
        assertEquals(12, categoryService.getCategoryCount(1));
    }


    @Test
    @DisplayName("setCategoryBudget: should update budgetLimit")
    void setCategoryBudget_shouldUpdateBudgetLimit() {
        when(categoryRepository.findByCategoryId(1)).thenReturn(Optional.of(foodCategory));
        when(categoryRepository.save(any())).thenReturn(foodCategory);

        categoryService.setCategoryBudget(1, 5000.0);

        verify(categoryRepository).save(argThat(c -> c.getBudgetLimit() == 5000.0));
    }
}
