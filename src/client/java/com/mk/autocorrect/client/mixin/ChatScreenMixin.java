package com.mk.autocorrect.client.mixin;

import com.mk.autocorrect.client.AutocorrectEngine;
import com.mk.autocorrect.client.AutocorrectSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected EditBox input;

    private static final int KEY_TAB = 258;
    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;

    @Inject(method = "onEdited", at = @At("TAIL"))
    private void autocorrect$onEdited(String value, CallbackInfo ci) {
        if (value == null || value.startsWith("/")) {
            AutocorrectSuggestions.clear();
            return;
        }

        int lastSpace = value.lastIndexOf(' ');
        int wordStart = lastSpace + 1;
        String lastWord = value.substring(wordStart);

        List<String> suggestions = AutocorrectEngine.suggest(lastWord, 5);

        if (suggestions.isEmpty()) {
            AutocorrectSuggestions.clear();
        } else {
            AutocorrectSuggestions.set(suggestions, wordStart);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void autocorrect$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!AutocorrectSuggestions.hasSuggestions()) return;

        int key = event.key();

        if (key == KEY_TAB) {
            acceptSuggestion();
            cir.setReturnValue(true);
        } else if (key == KEY_UP) {
            AutocorrectSuggestions.moveUp();
            cir.setReturnValue(true);
        } else if (key == KEY_DOWN) {
            AutocorrectSuggestions.moveDown();
            cir.setReturnValue(true);
        }
    }

    private void acceptSuggestion() {
        String chosen = AutocorrectSuggestions.getSelected();
        int start = AutocorrectSuggestions.getWordStart();
        if (chosen == null || start < 0) return;

        String current = this.input.getValue();
        String newValue = current.substring(0, start) + chosen + " ";
        this.input.setValue(newValue);
        AutocorrectSuggestions.clear();
    }

    // Draws the suggestion box directly above the chat input bar, every frame.
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void autocorrect$renderSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!AutocorrectSuggestions.hasSuggestions()) return;

        List<String> options = AutocorrectSuggestions.get();
        int selected = AutocorrectSuggestions.getSelectedIndex();
        Font font = Minecraft.getInstance().font;

        int lineHeight = 10;
        int padding = 2;
        int boxWidth = 140;
        int boxHeight = options.size() * lineHeight + padding * 2;

        int guiHeight = graphics.guiHeight();
        int boxBottom = guiHeight - 14; // sits right above the input bar background
        int boxTop = boxBottom - boxHeight;
        int boxLeft = 4;
        int boxRight = boxLeft + boxWidth;

        graphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0xDD000000);

        for (int i = 0; i < options.size(); i++) {
            int y = boxTop + padding + i * lineHeight;
            boolean isSelected = (i == selected);
            int color = isSelected ? 0xFFFFFF55 : 0xFFAAAAAA;
            String line = (isSelected ? "> " : "  ") + options.get(i);
            graphics.text(font, line, boxLeft + 2, y, color);
        }
    }
}