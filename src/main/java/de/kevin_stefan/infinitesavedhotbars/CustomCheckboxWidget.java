package de.kevin_stefan.infinitesavedhotbars;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class CustomCheckboxWidget extends AbstractWidget {

    private boolean checked;
    private final Callback callback;

    public CustomCheckboxWidget(int x, int y, int width, int height, boolean checked, Callback callback) {
        super(x, y, width, height, Component.empty());
        this.checked = checked;
        this.callback = callback;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Identifier identifier = this.checked ? Checkbox.CHECKBOX_SELECTED_SPRITE : Checkbox.CHECKBOX_SPRITE;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        this.checked = !this.checked;
        this.callback.onValueChange(this, checked);
    }

    public interface Callback {
        void onValueChange(CustomCheckboxWidget checkbox, boolean checked);
    }

}
