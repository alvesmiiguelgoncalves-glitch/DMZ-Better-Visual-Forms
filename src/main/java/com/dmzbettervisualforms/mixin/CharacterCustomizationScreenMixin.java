package com.dmzbettervisualforms.mixin;

import com.dmzbettervisualforms.client.PlayerVisuals;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.buttons.TexturedTextButton;
import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Tela de Customizacao do DMZ:
 * <ul>
 *   <li>aba Cabelo: com o SSJ4 (GT) na previa, uma segunda caixa de cor ao lado da do cabelo = tint do cabelo;</li>
 *   <li>aba Olhos: setas embaixo para alternar entre "Base" e "SSJ4 (GT)"; no SSJ4 (GT) as duas caixas de cor mudam
 *       a cor dos olhos dessa forma.</li>
 * </ul>
 * Tudo so do jogador local ({@link PlayerVisuals}); nao altera o JSON das formas nem os outros jogadores.
 */
@Mixin(value = CharacterCustomizationScreen.class, remap = false)
public abstract class CharacterCustomizationScreenMixin extends Screen {

    private static final int BVF$LEFT_X = 12;
    private static final int BVF$LEFT_W = 141;

    @Shadow private int previewFormIndex;
    @Shadow @Final private List<Object> previewFormOptions;
    @Shadow @Final private Map<String, TexturedTextButton> colorButtons;
    @Shadow private String currentColorField;
    @Shadow @Final private Character character;

    @Shadow protected abstract void refreshScreenWidgets();
    @Shadow private TexturedTextButton createColorButton(int x, int y, String fieldName) { return null; }
    @Shadow private CustomTextureButton createArrowButton(int x, int y, boolean isLeft, CustomTextureButton.OnPress onPress) { return null; }
    @Shadow private String getCurrentPreviewTransformationName() { return ""; }

    protected CharacterCustomizationScreenMixin(Component title) {
        super(title);
    }

    // ---- Cabelo ----------------------------------------------------------------------------------------------------

    @Inject(method = "initHairTab", at = @At("TAIL"), remap = false)
    private void bvf$hairTint(int top, CallbackInfo ci) {
        if (bvf$ssj4Index() < 0 || previewFormIndex != bvf$ssj4Index()) return;
        TexturedTextButton hair = colorButtons.get("hairColor");
        if (hair != null) hair.setX(BVF$LEFT_X + 46);
        addRenderableWidget(createColorButton(BVF$LEFT_X + 74, top + 10, "bvf:hairTint"));
    }

    // ---- Olhos -----------------------------------------------------------------------------------------------------

    @Inject(method = "initEyesTab", at = @At("TAIL"), remap = false)
    private void bvf$eyesPager(int top, CallbackInfo ci) {
        int ssj = bvf$ssj4Index();
        if (ssj < 0) return;
        if (previewFormIndex != ssj) previewFormIndex = 0;
        int y = top + 174;
        if (previewFormIndex == ssj) {
            addRenderableWidget(createArrowButton(BVF$LEFT_X + 18, y, true, btn -> {
                previewFormIndex = 0;
                refreshScreenWidgets();
            }));
            // o botao "copiar olho 1 para o olho 2" mexeria nos olhos base: nao serve nesta pagina
            for (GuiEventListener child : List.copyOf(children())) {
                if (child instanceof CustomTextureButton button && button.getX() == BVF$LEFT_X + 65 && button.getWidth() == 10) {
                    removeWidget(button);
                }
            }
            bvf$refreshEyeButton("eye1Color", PlayerVisuals.KEY_EYE1);
            bvf$refreshEyeButton("eye2Color", PlayerVisuals.KEY_EYE2);
        } else {
            addRenderableWidget(createArrowButton(BVF$LEFT_X + BVF$LEFT_W - 28, y, false, btn -> {
                previewFormIndex = ssj;
                refreshScreenWidgets();
            }));
        }
    }

    @Inject(method = "renderEyesText", at = @At("TAIL"), remap = false)
    private void bvf$eyesPagerText(GuiGraphics graphics, int centerX, int top, CallbackInfo ci) {
        if (bvf$ssj4Index() < 0) return;
        TextUtil.drawCenteredStringWithBorder(graphics, this.font, Component.literal(getCurrentPreviewTransformationName()), centerX, top + 178, 0xFFFFFF);
    }

    private void bvf$refreshEyeButton(String field, String key) {
        TexturedTextButton button = colorButtons.get(field);
        if (button != null) {
            String fallback = "eye2Color".equals(field) ? character.getEye2Color() : character.getEye1Color();
            button.setBackgroundColor(ColorUtils.hexToInt(PlayerVisuals.shown(key, character.getRace(), fallback)));
        }
    }

    // ---- cores -----------------------------------------------------------------------------------------------------

    @Inject(method = "getColorFromField", at = @At("HEAD"), cancellable = true, remap = false)
    private void bvf$getColor(String fieldName, CallbackInfoReturnable<String> cir) {
        String key = bvf$key(fieldName);
        if (key == null) return;
        String fallback = "eye2Color".equals(fieldName) ? character.getEye2Color() : character.getEye1Color();
        cir.setReturnValue(PlayerVisuals.shown(key, character.getRace(), fallback));
    }

    @Inject(method = "applyColor", at = @At("HEAD"), cancellable = true, remap = false)
    private void bvf$applyColor(String color, CallbackInfo ci) {
        String key = bvf$key(currentColorField);
        if (key == null) return;
        PlayerVisuals.set(key, color);
        TexturedTextButton button = colorButtons.get(currentColorField);
        if (button != null) button.setBackgroundColor(ColorUtils.hexToInt(color));
        ci.cancel(); // nao sincroniza com o servidor: e so visual do jogador local
    }

    /** Chave do {@link PlayerVisuals} para o campo de cor, ou null se for um campo normal do DMZ. */
    private String bvf$key(String field) {
        if ("bvf:hairTint".equals(field)) return PlayerVisuals.KEY_HAIR_TINT;
        int ssj = bvf$ssj4Index();
        if (ssj >= 0 && previewFormIndex == ssj) {
            if ("eye1Color".equals(field)) return PlayerVisuals.KEY_EYE1;
            if ("eye2Color".equals(field)) return PlayerVisuals.KEY_EYE2;
        }
        return null;
    }

    /** Posicao do SSJ4 (GT) na lista de formas da previa, ou -1. */
    private int bvf$ssj4Index() {
        try {
            for (int i = 1; i < previewFormOptions.size(); i++) {
                Object option = previewFormOptions.get(i);
                String group = bvf$text(option, "groupName");
                String form = bvf$text(option, "formName");
                if (group.isEmpty() || form.isEmpty()) continue;
                FormConfig.FormData data = ConfigManager.getForm(character.getRace(), group, form);
                if (PlayerVisuals.isSsj4Gt(data)) return i;
            }
        } catch (RuntimeException | ReflectiveOperationException ignored) {
        }
        return -1;
    }

    private static String bvf$text(Object option, String field) throws ReflectiveOperationException {
        Field f = option.getClass().getDeclaredField(field);
        f.setAccessible(true);
        Object value = f.get(option);
        return value == null ? "" : value.toString();
    }
}
