package de.kevin_stefan.infinitesavedhotbars;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomCheckboxWidget extends ClickableWidget {

    private boolean checked;
    private final Callback callback;

    public CustomCheckboxWidget(int x, int y, int width, int height, boolean checked, Callback callback) {
        super(x, y, width, height, Text.empty());
        this.checked = checked;
        this.callback = callback;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        Identifier identifier = this.checked ? CheckboxWidget.SELECTED_TEXTURE : CheckboxWidget.TEXTURE;
        context.drawGuiTexture(identifier, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.checked = !this.checked;
        this.callback.onValueChange(this, checked);
    }

    public interface Callback {
        void onValueChange(CustomCheckboxWidget checkbox, boolean checked);
    }

}
