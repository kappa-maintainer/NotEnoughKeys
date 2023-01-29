package de.morrien.nekeys.gui.voice;

import de.morrien.nekeys.NotEnoughKeys;
import de.morrien.nekeys.api.command.IVoiceCommand;
import de.morrien.nekeys.api.popup.AbstractPopup;
import de.morrien.nekeys.gui.DropDownList;
import de.morrien.nekeys.voice.command.EmptyVoiceCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.io.IOException;

/**
 * Created by Timor Morrien
 */
@SideOnly(Side.CLIENT)
public class GuiVoiceCommand extends GuiScreen {

    /**
     * A reference to the screen object that created this. Used for navigating between screens.
     */
    private final GuiScreen parentScreen;
    protected String screenTitle = "Voice Commands";
    private GuiVoiceCommandList voiceCommandList;
    private GuiVoiceCommandSettings activeSettings;

    public GuiVoiceCommand() {
        this(null);
    }

    public GuiVoiceCommand(GuiScreen screen) {
        this.parentScreen = screen;
    }

    public void openSettings(GuiVoiceCommandList.CommandEntry entry) {
        activeSettings = new GuiVoiceCommandSettings(entry);
        if (activeSettings.voiceCommandPopup == null) {
            activeSettings = null;
            NotEnoughKeys.logger.warn("Could not open popup for entry \"" + entry.getClass().getName() + "\"");
        }
    }

    /**
     * Init the GUI components
     */
    @Override
    public void initGui() {
        this.voiceCommandList = new GuiVoiceCommandList(this, this.mc);
        this.screenTitle = I18n.format("gui.nekeys.voice_commands.title");
        this.buttonList.add(new GuiButton(100, this.width / 2 - 175 + 181, this.height - 29, 150, 20, I18n.format("gui.done")));
        this.buttonList.add(new GuiButton(101, this.width / 2 - 175, this.height - 29, 150, 20, I18n.format("gui.nekeys.reload")));
        this.buttonList.add(new GuiButton(102, this.width / 2 - 175, this.height - 54, 150, 20, I18n.format("gui.nekeys.createNew")));
        this.buttonList.add(new GuiButton(103, this.width / 2 - 175 + 181, this.height - 54, 150, 20, I18n.format("gui.nekeys.clone")));
    }

    @Override
    public void onGuiClosed() {
        voiceCommandList.save();
    }

    /**
     * Handles mouse input.
     */
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (activeSettings != null) {
            activeSettings.dropDownList.handleMouseInput();
            activeSettings.voiceCommandPopup.handleMouseInput();
        } else {
            this.voiceCommandList.handleMouseInput();
        }
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 100) {
            this.mc.displayGuiScreen(this.parentScreen);
        } else if (button.id == 101) {
            NotEnoughKeys.instance.voiceHandler.reloadConfig();
            voiceCommandList.loadCommands();
        } else if (button.id == 102) {
            voiceCommandList.listEntries.add(voiceCommandList.newEntry(new EmptyVoiceCommand("empty", "")));
        } else if (button.id == 103) {
            voiceCommandList.activeAction = commandEntry -> {
                int index = voiceCommandList.listEntries.indexOf(commandEntry);
                IVoiceCommand command = NotEnoughKeys.instance.voiceHandler.factoryMap.newVoiceCommand(commandEntry.getCommand());
                GuiVoiceCommandList.CommandEntry entry = voiceCommandList.newEntry(command);
                voiceCommandList.listEntries.add(index, entry);
                voiceCommandList.activeAction = null;
                return true;
            };
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (activeSettings != null) {
            activeSettings.onClick(mouseX, mouseY);
            return;
        }
        if (mouseButton != 0 || !this.voiceCommandList.mouseClicked(mouseX, mouseY, mouseButton)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    /**
     * Called when a mouse button is released.
     */
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state != 0 || !this.voiceCommandList.mouseReleased(mouseX, mouseY, state)) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (activeSettings != null) {
            activeSettings.voiceCommandPopup.keyTyped(typedChar, keyCode);
        } else {
            if (keyCode == 1 || !this.voiceCommandList.keyTyped(typedChar, keyCode)) {
                //if (keyCode == 1) voiceCommandList.save();
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    /**
     * Called from the main game loop to update the screen.
     */
    @Override
    public void updateScreen() {
        super.updateScreen();
        if (voiceCommandList == null) {
            this.initGui();
        }
        voiceCommandList.update();
    }

    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int mX = mouseX;
        int mY = mouseY;
        if (activeSettings != null) {
            mX = -1;
            mY = -1;
        }
        if (voiceCommandList == null) return;
        this.voiceCommandList.drawScreen(mX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 8, 16777215);
        double x = voiceCommandList.left + width / 2d - voiceCommandList.getListWidth() / 2d + 2;
        drawCenteredString(fontRenderer, I18n.format("gui.nekeys.voice_commands.name"), (int) x + 20, 25, Color.ORANGE.getRGB());
        drawString(fontRenderer, I18n.format("gui.nekeys.voice_commands.rule"), (int) x + 60, 25, Color.ORANGE.getRGB());
        super.drawScreen(mX, mouseY, partialTicks);

        //drawHorizontalLine(this.width / 2 - 175, this.width / 2 - 175 + 330, this.height - 30, 0xFFFFFFFF);
        if (activeSettings != null) {
            activeSettings.draw(mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void drawVerticalLine(int x, int startY, int endY, int color) {
        super.drawVerticalLine(x, startY, endY, color);
    }

    @Override
    protected void drawHorizontalLine(int startX, int endX, int y, int color) {
        super.drawHorizontalLine(startX, endX, y, color);
    }

    public class GuiVoiceCommandSettings {

        private GuiVoiceCommandList.CommandEntry commandEntry;
        private AbstractPopup voiceCommandPopup;
        //private Map<String, Class<? extends IVoiceCommand>> nameMap;
        private DropDownList<Class<? extends IVoiceCommand>> dropDownList;
        private Class<? extends IVoiceCommand> lastSelection;
        private GuiButton saveButton;

        public GuiVoiceCommandSettings(GuiVoiceCommandList.CommandEntry entry) {
            this.commandEntry = entry;
            //this.nameMap = new HashMap<>();
            this.dropDownList = new DropDownList<>(0, 0, 0, 18, 6);
            this.dropDownList.stringifier = clazz -> I18n.format("voiceCommand." + clazz.getSimpleName() + ".name");
            NotEnoughKeys.instance.voiceHandler.factoryMap.forEach((voiceCommandClass, factory) -> {
                //String name = I18n.format("voiceCommand." + voiceCommandClass.getSimpleName() + ".name");
                //nameMap.put(name, voiceCommandClass);
                dropDownList.optionsList.add(voiceCommandClass);
                if (voiceCommandClass.equals(commandEntry.getCommand().getClass())) {
                    dropDownList.selection = voiceCommandClass;
                    voiceCommandPopup = NotEnoughKeys.instance.voiceHandler.factoryMap.newPopup(commandEntry.getCommand());
                    lastSelection = dropDownList.selection;
                }
            });
            dropDownList.optionsList.sort((o1, o2) -> dropDownList.stringifier.toString(o1).compareToIgnoreCase(dropDownList.stringifier.toString(o2)));
            this.saveButton = new GuiButton(0, width / 2 + 100 - 45, height / 2 + 60 - 23, 40, 20, "Save");
        }

        public void draw(int mouseX, int mouseY, float partialTicks) {
            drawRect(0, 0, width, height, 0x77000000);
            drawRect(width / 2 - 101, height / 2 - 61, width / 2 + 102, height / 2 + 61, 0xFFFFFFFF);
            //drawRect(width/2 - 100, height/2 - 60, width/2 + 100, height/2 + 60, 0xFF000000);

            double dialogX = width / 2D - 100;
            double dialogY = height / 2D - 60;
            double dialogWidth = 200;
            double dialogHeight = 120;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            mc.getTextureManager().bindTexture(new ResourceLocation("textures/blocks/planks_oak.png"));
            GlStateManager.color(1F, 1F, 1F, 1F);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos(dialogX, dialogY + dialogHeight, 0).tex(0, height / 64D).color(150, 150, 150, 255).endVertex();
            bufferbuilder.pos(dialogX + dialogWidth, dialogY + dialogHeight, 0).tex(width / 64D, height / 64D).color(150, 150, 150, 255).endVertex();
            bufferbuilder.pos(dialogX + dialogWidth, dialogY, 0).tex(width / 64D, 0).color(150, 150, 150, 255).endVertex();
            bufferbuilder.pos(dialogX, dialogY, 0).tex(0, 0).color(150, 150, 150, 255).endVertex();
            tessellator.draw();

            //this.saveButton.x = width / 2 + 100 - 45;
            //this.saveButton.y = height / 2 + 60 - 23;
            saveButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);

            if (lastSelection != dropDownList.selection) {
                commandEntry.setCommand(NotEnoughKeys.instance.voiceHandler.factoryMap.get(dropDownList.selection).newCommand(commandEntry.getCommand().getName(), commandEntry.getCommand().getRuleContent()));
                voiceCommandPopup = NotEnoughKeys.instance.voiceHandler.factoryMap.newPopup(commandEntry.getCommand());
                lastSelection = dropDownList.selection;
            }

            voiceCommandPopup.draw((int) dialogX, (int) dialogY + 22, (int) dialogWidth, (int) dialogHeight - 22, mouseX, mouseY, partialTicks);

            dropDownList.x = (int) (dialogX + 5);
            dropDownList.y = (int) (dialogY + 3);
            dropDownList.width = (int) (dialogWidth - 10);
            dropDownList.draw();
        }

        public void onClick(int mouseX, int mouseY) {
            if (mouseX <= width / 2 - 102 ||
                    mouseX >= width / 2 + 102 ||
                    mouseY <= height / 2 - 62 ||
                    mouseY >= height / 2 + 62) {
                activeSettings = null;
                return;
            }
            if (!dropDownList.onClick(mouseX, mouseY) && !voiceCommandPopup.onClick(mouseX, mouseY)) {
                if (saveButton.mousePressed(Minecraft.getMinecraft(), mouseX, mouseY)) {
                    saveButton.playPressSound(Minecraft.getMinecraft().getSoundHandler());
                    commandEntry.setCommand(voiceCommandPopup.getCommand());
                    activeSettings = null;
                }
            }
        }
    }
}
