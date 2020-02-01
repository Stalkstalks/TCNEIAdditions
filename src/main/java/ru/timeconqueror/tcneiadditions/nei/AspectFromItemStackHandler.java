package ru.timeconqueror.tcneiadditions.nei;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import com.djgiannuzz.thaumcraftneiplugin.ModItems;
import com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.timeconqueror.tcneiadditions.TCNEIAdditions;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.gui.GuiResearchRecipe;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static codechicken.lib.gui.GuiDraw.changeTexture;
import static codechicken.lib.gui.GuiDraw.drawTexturedModalRect;

public class AspectFromItemStackHandler extends TemplateRecipeHandler {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(TCNEIAdditions.MODID, "textures/gui/itemstack_background.png");
    private static final ResourceLocation THAUM_OVERLAYS = new ResourceLocation(Thaumcraft.MODID.toLowerCase(), "textures/gui/gui_researchbook_overlay.png");
    private static final int GUI_WIDTH = 166;
    private static final int GUI_HEIGHT = 131;
    private static final int STACKS_OVERLAY_WIDTH = 163;
    private static final int STACKS_OVERLAY_HEIGHT = 74;
    private static final int STACKS_OVERLAY_START_X = GUI_WIDTH / 2 - STACKS_OVERLAY_WIDTH / 2;
    private static final int STACKS_OVERLAY_START_Y = GUI_HEIGHT - STACKS_OVERLAY_HEIGHT;
    private String playerName;

    public AspectFromItemStackHandler() {
        playerName = Minecraft.getMinecraft().getSession().getUsername();
    }

    @Override
    public String getRecipeName() {
        return I18n.format("tcneiadditions.aspect_from_itemstack.title");
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public void loadCraftingRecipes(ItemStack ingredient) {
        if (ingredient.getItem() instanceof ItemAspect) {
            Aspect aspect = ItemAspect.getAspects(ingredient).getAspects()[0];

            if (Thaumcraft.proxy.playerKnowledge.hasDiscoveredAspect(playerName, aspect)) {

                List<ItemStack> containingItemStacks = findContainingItemStacks(aspect);
                if (!containingItemStacks.isEmpty()) {
                    new AspectCachedRecipe(aspect, containingItemStacks);
                }
            }
        }
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        changeTexture(THAUM_OVERLAYS);
        {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            {
                int textureSize = 16;
                float scaleFactor = 1.75F;
                int x = GUI_WIDTH / 2;
                int y = 35;
                GL11.glTranslatef(x, y, 0);
                GL11.glScalef(scaleFactor, scaleFactor, 1.0F);
                drawTexturedModalRect(-textureSize / 2, -textureSize / 2, 20, 3, 16, 16);
                GL11.glScalef(1 / scaleFactor, 1 / scaleFactor, 1.0F);
                GL11.glTranslatef(-x, -y, 0);
            }
            GL11.glDisable(GL11.GL_BLEND);
        }

        changeTexture(BACKGROUND);
        drawTexturedModalRect(STACKS_OVERLAY_START_X, STACKS_OVERLAY_START_Y, 0, 0, STACKS_OVERLAY_WIDTH, STACKS_OVERLAY_HEIGHT);
    }

    @Override
    public void drawForeground(int recipe) {
    }

    @Override
    public String getGuiTexture() {
        return null;
    }


    private List<ItemStack> findContainingItemStacks(Aspect aspect) {
        ArrayList<ItemStack> stacks = new ArrayList<>();
        List<String> list = Thaumcraft.proxy.getScannedObjects().get(playerName);

        if (list != null) {
            for (String itemStackCache : list) {//every string represents cache of itemstack, like @12921929129
                try {
                    itemStackCache = itemStackCache.substring(1); // here we get rid of @

                    ItemStack is = GuiResearchRecipe.getFromCache(Integer.parseInt(itemStackCache));
                    if (is == null) continue;

                    AspectList tags = ThaumcraftCraftingManager.getObjectTags(is);
                    tags = ThaumcraftCraftingManager.getBonusTags(is, tags);

                    if (tags.size() <= 0)
                        continue;

                    ItemStack is2 = is.copy();
                    is2.stackSize = tags.getAmount(aspect);
                    if (is2.stackSize <= 0) continue;

                    stacks.add(is2);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        stacks.sort(Comparator.<ItemStack>comparingInt(itemStack -> itemStack.stackSize).reversed());

        return stacks;
    }

    private class AspectCachedRecipe extends CachedRecipe {
        private static final int STACKS_COUNT = 36;
        private ItemStack aspectStack;
        private int start;
        private ItemStack[] localPageStacks;
        private List<PositionedStack> ingredients = null;
        private PositionedStack result = null;

        public AspectCachedRecipe(Aspect aspect, List<ItemStack> fullItemStackList) {
            this(aspect, fullItemStackList, 0);
        }

        private AspectCachedRecipe(Aspect aspect, List<ItemStack> fullItemStackList, int start) {
            this.start = start;
            localPageStacks = getItemsInInterval(fullItemStackList);
            this.aspectStack = new ItemStack(ModItems.itemAspect);
            ItemAspect.setAspect(aspectStack, aspect);

            arecipes.add(this);

            if (start + STACKS_COUNT < fullItemStackList.size()) {//fixme
                new AspectCachedRecipe(aspect, fullItemStackList, start + STACKS_COUNT);
            }
        }

        @Override
        public PositionedStack getResult() {
            if (result == null) {
                result = new PositionedStack(aspectStack, GUI_WIDTH / 2 - 16 / 2, 27);
            }
            return result;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            if (ingredients == null) {
                ingredients = new ArrayList<>(localPageStacks.length);
                for (int i = 0; i < localPageStacks.length; i++) {
                    int x = STACKS_OVERLAY_START_X + i % 9 * (16 + 2);
                    int y = STACKS_OVERLAY_START_Y + i / 9 * (16 + 2);
                    ItemStack stack = localPageStacks[i];
                    ingredients.add(new PositionedStack(stack, x, y));
                }
            }

            return ingredients;
        }

        private ItemStack[] getItemsInInterval(List<ItemStack> stacksIn) {
            int count = start + STACKS_COUNT <= stacksIn.size() ? STACKS_COUNT : stacksIn.size() - start;
            ItemStack[] itemStacks = new ItemStack[count];

            for (int i = 0; i < itemStacks.length; i++) {
                itemStacks[i] = stacksIn.get(start + i);
            }

            return itemStacks;
        }
    }
}