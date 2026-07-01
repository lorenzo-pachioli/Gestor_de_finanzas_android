package com.notificationcapture.app.services;

import com.notificationcapture.app.constants.CategoryConstants;
import com.notificationcapture.app.enums.CatColors;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.models.Category;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.CategoryRepository;

import java.util.ArrayList;
import java.util.List;

public class CategoryService {

    private final CategoryRepository repo;

    CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    public List<Category> getCategoriesForType(IngresoOEgreso type) {
        List<Category> filtered = new ArrayList<>(repo.getCategories(type));
        String otherId = type == IngresoOEgreso.INGRESO
                ? CategoryConstants.OTHER_INCOME_ID
                : CategoryConstants.OTHER_OUTCOME_ID;

        filtered.sort((a, b) -> {
            boolean aIsOther = otherId.equals(a.getId());
            boolean bIsOther = otherId.equals(b.getId());
            if (aIsOther && !bIsOther) return 1;
            if (!aIsOther && bIsOther) return -1;
            return 0;
        });
        return filtered;
    }

    public Category resolveCategory(String categoryId, IngresoOEgreso fallbackType) {
        if (categoryId != null) {
            Category found = repo.getCategoryById(categoryId);
            if (found != null) return found;
        }

        String otherId = fallbackType == IngresoOEgreso.INGRESO
                ? CategoryConstants.OTHER_INCOME_ID
                : CategoryConstants.OTHER_OUTCOME_ID;
        Category other = repo.getCategoryById(otherId);
        if (other != null) return other;
        return new Category(otherId, CategoryConstants.OTHER_DISPLAY_NAME, fallbackType);
    }

    public int getTransactionColor(Transaction transaction) {
        return CatColors.getOneIntColorByType(transaction.getType(), 0);
    }

    public int getColorForType(IngresoOEgreso type) {
        return CatColors.getOneIntColorByType(type, 0);
    }
}
