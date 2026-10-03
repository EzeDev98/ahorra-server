package com.ahorra.feature.accounting.service.impl;

import com.ahorra.feature.accounting.model.Category;
import com.ahorra.feature.accounting.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gives each user a starting set of categories the first time they are needed, so the
 * parse step and the client's category picker always have something to work with.
 * The user's own categories are never touched: nothing is added once any exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCategoryService {

    public static final List<String> DEFAULTS = List.of(
            "Food",
            "Transport",
            "Airtime and data",
            "Bills and utilities",
            "Rent",
            "Health",
            "Education",
            "Shopping",
            "Entertainment",
            "Family and gifts",
            "Other"
    );

    private final CategoryRepository categoryRepository;

    /** Creates the default categories for a user who has none. Safe to call again and from parallel requests. */
    public void ensureDefaults(Long userId) {
        if (!categoryRepository.findAllByUserIdAndActiveTrue(userId).isEmpty()) {
            return;
        }
        for (String name : DEFAULTS) {
            if (categoryRepository.findByUserIdAndName(userId, name).isPresent()) {
                continue;
            }
            Category category = new Category();
            category.setUserId(userId);
            category.setName(name);
            category.setActive(true);
            category.setSystem(true);
            try {
                categoryRepository.save(category);
            } catch (DataIntegrityViolationException e) {
                // A parallel request created the same category first; the unique (user, name) index kept it to one.
                log.debug("Default category '{}' already created for user {}", name, userId);
            }
        }
    }
}
