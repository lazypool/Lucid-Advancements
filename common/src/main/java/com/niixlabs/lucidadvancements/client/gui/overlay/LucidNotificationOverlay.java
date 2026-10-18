package com.niixlabs.lucidadvancements.client.gui.overlay;

import com.niixlabs.lucidadvancements.Constants;
import com.niixlabs.lucidadvancements.client.gui.screen.LucidAdvancementsScreen;
import com.niixlabs.lucidadvancements.client.gui.util.GuiScale;
import com.niixlabs.lucidadvancements.config.LucidConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LucidNotificationOverlay {
    private static final int EFFECT_CACHE_INTERVAL_MS = 50;
    private static final int MAX_RECIPE_ICONS = 16;
    private static final int MIN_NOTIFICATION_HEIGHT = 30;
    private static final int MIN_EFFECT_ROW_HEIGHT = 22;

    private static final List<TimedNotification> NOTIFICATIONS = new ArrayList<>();
    private static long notificationExpiresAt = 0L;
    private static ClientLevel lastLevel = null;

    private static List<MobEffectInstance> effectCache = List.of();
    private static long effectCacheAt = 0L;
    private static ClientLevel effectCacheLevel = null;
    private static LocalPlayer effectCachePlayer = null;

    private LucidNotificationOverlay() {}

    public static void add(Notification notification) {
        if (notification == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            clear();
        }

        long now = Util.getMillis();
        if (now >= notificationExpiresAt) {
            clearNotifications();
        }

        if (notification.recipe) {
            for (TimedNotification timed : NOTIFICATIONS) {
                if (timed.notification.recipe) {
                    timed.notification.appendIcons(notification.itemIcons);
                    notificationExpiresAt = now + notificationDuration(mc);
                    return;
                }
            }
        }

        NOTIFICATIONS.add(new TimedNotification(notification, now));

        int maxNotifications = Math.max(1, LucidConfig.notificationMaxCount);
        while (NOTIFICATIONS.size() > maxNotifications) {
            NOTIFICATIONS.remove(0);
        }

        notificationExpiresAt = now + notificationDuration(mc);
        playToastIn();
    }

    public static void render(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            clear();
        }

        if (mc.player == null || mc.options.hideGui || NOTIFICATIONS.isEmpty()) {
            return;
        }

        long now = Util.getMillis();
        if (now >= notificationExpiresAt) {
            playToastOut();
            clearNotifications();
            return;
        }

        double targetScale = GuiScale.targetScale(mc);
        float scaleMod = GuiScale.scaleModifier(mc);
        int screenWidth = (int) Math.ceil(mc.getWindow().getScreenWidth() / targetScale);
        int screenHeight = (int) Math.ceil(mc.getWindow().getScreenHeight() / targetScale);
        int limit = screenHeight - LucidConfig.overlayBottomScreenMargin;

        int width = LucidConfig.overlayBoxWidth;
        int height = Math.max(MIN_NOTIFICATION_HEIGHT, LucidConfig.notificationHeight);
        int x = (screenWidth - width) / 2;
        int y = Math.max(0, LucidConfig.notificationTopMargin);

        Font font = mc.font;

        guiGraphics.pose().pushPose();

        if (scaleMod != 1.0f) {
            guiGraphics.pose().scale(scaleMod, scaleMod, 1.0f);
        }

        for (TimedNotification timed : NOTIFICATIONS) {
            if (y + height > limit) {
                break;
            }

            timed.advanceIcon(now);
            List<ItemStack> icons = timed.notification.itemIcons;
            ItemStack icon = icons.isEmpty() ? ItemStack.EMPTY : icons.get(timed.iconIndex);

            renderNotification(guiGraphics, font, timed.notification, icon, x, y, width, height);
            y += height + LucidConfig.overlayBoxSpacing;
        }

        guiGraphics.pose().popPose();
    }

    public static void renderEffects(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        List<MobEffectInstance> effects = visibleEffects(mc);
        if (effects.isEmpty()) {
            return;
        }

        double targetScale = GuiScale.targetScale(mc);
        float scaleMod = GuiScale.scaleModifier(mc);
        int screenWidth = (int) Math.ceil(mc.getWindow().getScreenWidth() / targetScale);
        int screenHeight = (int) Math.ceil(mc.getWindow().getScreenHeight() / targetScale);
        int limit = screenHeight - LucidConfig.overlayBottomScreenMargin;

        int width = LucidConfig.overlayBoxWidth;
        int rowHeight = Math.max(MIN_EFFECT_ROW_HEIGHT, LucidConfig.effectRowHeight);
        int spacing = Math.max(0, LucidConfig.effectRowSpacing);
        int startY = LucidConfig.overlayStartY;
        int availableRows = (limit - startY + spacing) / (rowHeight + spacing);
        int rows = Math.min(Math.max(1, LucidConfig.effectHudMaxRows), availableRows);
        if (rows <= 0) {
            return;
        }

        int maxRight = screenWidth - LucidConfig.overlayRightMargin;
        maxRight = Math.min(maxRight, screenWidth / 2 - spacing);
        if (!NOTIFICATIONS.isEmpty()) {
            maxRight = Math.min(maxRight, (screenWidth - LucidConfig.overlayBoxWidth) / 2 - spacing);
        }
        if (!LucidAdvancementsScreen.TRACKED_ADVANCEMENTS.isEmpty()) {
            int pinnedLeft = screenWidth - LucidConfig.overlayBoxWidth - LucidConfig.overlayRightMargin;
            maxRight = Math.min(maxRight, pinnedLeft - spacing);
        }

        Font font = mc.font;
        int rendered = 0;

        guiGraphics.pose().pushPose();

        if (scaleMod != 1.0f) {
            guiGraphics.pose().scale(scaleMod, scaleMod, 1.0f);
        }

        for (int i = 0; i < effects.size(); i++) {
            int column = i / rows;
            int row = i % rows;
            int x = Math.max(0, LucidConfig.overlayLeftMargin) + column * (width + spacing);
            if (x + width > maxRight) {
                break;
            }

            int y = startY + row * (rowHeight + spacing);
            renderEffect(guiGraphics, font, mc, effects.get(i), x, y, width, rowHeight);
            rendered++;
        }

        if (rendered < effects.size()) {
            int hidden = effects.size() - rendered;
            int infoY = rendered == 0 ? startY : startY + rows * (rowHeight + spacing);
            if (infoY + font.lineHeight > limit) {
                infoY = Math.max(startY, limit - font.lineHeight);
            }

            guiGraphics.drawString(font, Component.translatable(Constants.MOD_ID + ".overlay.hidden_count", hidden),
                    Math.max(0, LucidConfig.overlayLeftMargin), infoY, LucidConfig.overlayCriterionTextColor, true);
        }

        guiGraphics.pose().popPose();
    }

    public static void clear() {
        clearNotifications();
        effectCache = List.of();
        effectCacheAt = 0L;
        effectCacheLevel = null;
        effectCachePlayer = null;
    }

    private static void clearNotifications() {
        NOTIFICATIONS.clear();
        notificationExpiresAt = 0L;
    }

    private static long notificationDuration(Minecraft mc) {
        double multiplier = mc.options.notificationDisplayTime().get();
        return Math.max(1L, (long) (Math.max(1, LucidConfig.notificationDuration) * multiplier));
    }

    private static void playToastIn() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F, 1.0F)
        );
    }

    private static void playToastOut() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_OUT, 1.0F, 1.0F)
        );
    }

    private static void renderNotification(GuiGraphics guiGraphics, Font font, Notification notification, ItemStack icon, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, LucidConfig.overlayBgColor);
        guiGraphics.fill(x, y, x + width, y + LucidConfig.overlayAccentWidth, notification.accent);

        int textY = y + (height - 18) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 50);

        if (!icon.isEmpty()) {
            guiGraphics.renderItem(icon, x + LucidConfig.overlayIconXOffset, y + (height - 16) / 2);
        }

        String title = font.plainSubstrByWidth(notification.title.getString(), width - LucidConfig.overlayTitleMaxWidthPadding);
        String subtitle = font.plainSubstrByWidth(notification.subtitle.getString(), width - LucidConfig.overlayTitleMaxWidthPadding);

        guiGraphics.drawString(font, title, x + LucidConfig.overlayTitleXOffset, textY, notification.titleColor, true);
        guiGraphics.drawString(font, subtitle, x + LucidConfig.overlayTitleXOffset, textY + 11, LucidConfig.overlayCriterionTextColor, true);

        guiGraphics.pose().popPose();
    }

    private static void renderEffect(GuiGraphics guiGraphics, Font font, Minecraft mc, MobEffectInstance effect, int x, int y, int width, int rowHeight) {
        MobEffect value = effect.getEffect().value();
        int accent = value.isBeneficial() ? LucidConfig.overlayNormalAccent
                : value.getCategory() == MobEffectCategory.HARMFUL ? LucidConfig.effectHarmfulAccent : LucidConfig.effectNeutralAccent;

        guiGraphics.fill(x, y, x + width, y + rowHeight, LucidConfig.overlayBgColor);
        guiGraphics.fill(x + width - LucidConfig.overlayAccentWidth, y, x + width, y + rowHeight, accent);

        int textY = y + (rowHeight - 18) / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 50);
        guiGraphics.blit(x + LucidConfig.overlayIconXOffset, y + (rowHeight - 16) / 2, 0, 16, 16,
                mc.getMobEffectTextures().get(effect.getEffect()));

        String name = font.plainSubstrByWidth(effectName(effect).getString(), width - LucidConfig.overlayTitleMaxWidthPadding);
        String duration = MobEffectUtil.formatDuration(effect, 1.0F, tickRate(mc)).getString();

        guiGraphics.drawString(font, name, x + LucidConfig.overlayTitleXOffset, textY, LucidConfig.overlayTitleColor, true);
        guiGraphics.drawString(font, duration, x + LucidConfig.overlayTitleXOffset, textY + 11, LucidConfig.overlayCriterionTextColor, true);

        guiGraphics.pose().popPose();
    }

    private static Component effectName(MobEffectInstance effect) {
        MutableComponent name = effect.getEffect().value().getDisplayName().copy();
        int amplifier = effect.getAmplifier();

        if (amplifier >= 1 && amplifier <= 9) {
            name.append(CommonComponents.SPACE)
                    .append(Component.translatable("enchantment.level." + (amplifier + 1)));
        }

        return name;
    }

    private static float tickRate(Minecraft mc) {
        return mc.level == null ? 20.0F : mc.level.tickRateManager().tickrate();
    }

    private static List<MobEffectInstance> visibleEffects(Minecraft mc) {
        if (mc.player == null) {
            return List.of();
        }

        long now = Util.getMillis();
        if (mc.level != effectCacheLevel || mc.player != effectCachePlayer || now - effectCacheAt >= EFFECT_CACHE_INTERVAL_MS) {
            effectCacheLevel = mc.level;
            effectCachePlayer = mc.player;
            effectCacheAt = now;

            List<MobEffectInstance> effects = new ArrayList<>();
            for (MobEffectInstance effect : mc.player.getActiveEffects()) {
                if (effect.showIcon()) {
                    effects.add(effect);
                }
            }

            effects.sort(Collections.reverseOrder());
            effectCache = effects;
        }

        return effectCache;
    }

    public interface Provider {
        Notification lucid$notification();
    }

    public static final class Notification {
        private final List<ItemStack> itemIcons;
        private final Component title;
        private final Component subtitle;
        private final int accent;
        private final int titleColor;
        private final boolean recipe;

        private Notification(List<ItemStack> itemIcons, Component title, Component subtitle, int accent, int titleColor, boolean recipe) {
            this.itemIcons = itemIcons;
            this.title = title;
            this.subtitle = subtitle;
            this.accent = accent;
            this.titleColor = titleColor;
            this.recipe = recipe;
        }

        public static Notification item(ItemStack icon, Component title, Component subtitle, int accent, int titleColor) {
            return new Notification(new ArrayList<>(List.of(icon)), title, subtitle, accent, titleColor, false);
        }

        public static Notification recipe(ItemStack icon, Component title, Component subtitle, int accent, int titleColor) {
            return new Notification(new ArrayList<>(List.of(icon)), title, subtitle, accent, titleColor, true);
        }

        private void appendIcons(List<ItemStack> icons) {
            int remaining = MAX_RECIPE_ICONS - itemIcons.size();
            if (remaining <= 0) {
                return;
            }

            itemIcons.addAll(icons.subList(0, Math.min(remaining, icons.size())));
        }
    }

    private static final class TimedNotification {
        private final Notification notification;
        private long nextIconAt;
        private int iconIndex;

        private TimedNotification(Notification notification, long now) {
            this.notification = notification;
            this.nextIconAt = now + 1000L;
        }

        private void advanceIcon(long now) {
            int iconCount = notification.itemIcons.size();
            if (iconCount <= 1) {
                iconIndex = 0;
                return;
            }

            while (now >= nextIconAt) {
                iconIndex = (iconIndex + 1) % iconCount;
                nextIconAt += 1000L;
            }
        }
    }
}
