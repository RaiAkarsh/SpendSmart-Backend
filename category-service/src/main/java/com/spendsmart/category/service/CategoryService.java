package com.spendsmart.category.service;

import com.spendsmart.category.entity.Category;

import java.util.List;


public interface CategoryService {

    
    Category createCategory(Category category);

    
    List<Category> getByUserId(int userId);

    
    Category getCategoryById(int categoryId);

    List<Category> getByUserAndType(int userId, String type);


    Category updateCategory(int categoryId, Category updatedCategory);


    void deleteCategory(int categoryId);


    void initDefaultCategories(int userId);

    void setCategoryBudget(int categoryId, double budgetLimit);

    int getCategoryCount(int userId);
}
