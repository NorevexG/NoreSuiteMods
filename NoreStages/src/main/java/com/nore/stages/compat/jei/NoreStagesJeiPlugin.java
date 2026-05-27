package com.nore.stages.compat.jei;

import com.nore.stages.NoreStages;
import com.nore.stages.NoreStagesConfig;
import com.nore.stages.client.NoreStagesClientState;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@JeiPlugin
public final class NoreStagesJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    private static final Set<RecipeHolder<CraftingRecipe>> hiddenCraftingRecipes = new LinkedHashSet<>();
    private static final Set<ItemStack> hiddenItems = new LinkedHashSet<>();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(NoreStages.MODID, "jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        refreshHiddenContent();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        hiddenCraftingRecipes.clear();
        hiddenItems.clear();
    }

    public static void refreshHiddenContent() {
        if (runtime == null) {
            return;
        }

        IRecipeManager jeiRecipes = runtime.getRecipeManager();
        if (!hiddenCraftingRecipes.isEmpty()) {
            jeiRecipes.unhideRecipes(RecipeTypes.CRAFTING, hiddenCraftingRecipes);
            hiddenCraftingRecipes.clear();
        }
        if (!hiddenItems.isEmpty()) {
            runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hiddenItems);
            hiddenItems.clear();
        }

        if (!NoreStagesConfig.hideLockedJeiRecipes()) {
            return;
        }

        Set<ResourceLocation> lockedRecipeIds = NoreStagesClientState.lockedRecipes();
        List<RecipeHolder<CraftingRecipe>> toHide = jeiRecipes.createRecipeLookup(RecipeTypes.CRAFTING)
                .includeHidden()
                .get()
                .filter(recipe -> lockedRecipeIds.contains(recipe.id()))
                .toList();
        for (ResourceLocation recipeId : NoreStagesClientState.lockedRecipes()) {
            boolean present = toHide.stream().anyMatch(recipe -> recipe.id().equals(recipeId));
            if (!present) {
                NoreStages.LOGGER.debug("NoreStages could not find JEI crafting recipe {} to hide.", recipeId);
            }
        }

        if (!toHide.isEmpty()) {
            hiddenCraftingRecipes.addAll(toHide);
            jeiRecipes.hideRecipes(RecipeTypes.CRAFTING, toHide);
        }

        List<ItemStack> itemsToHide = new ArrayList<>();
        Set<ResourceLocation> lockedItems = NoreStagesClientState.lockedItems();
        for (ItemStack stack : runtime.getIngredientManager().getAllItemStacks()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (lockedItems.contains(itemId)) {
                itemsToHide.add(stack);
            }
        }
        for (ResourceLocation itemId : NoreStagesClientState.lockedItems()) {
            boolean present = itemsToHide.stream().anyMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId));
            if (!present) {
                NoreStages.LOGGER.debug("NoreStages could not find JEI item {} to hide.", itemId);
            }
        }
        if (!itemsToHide.isEmpty()) {
            hiddenItems.addAll(itemsToHide);
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, itemsToHide);
        }
    }
}
