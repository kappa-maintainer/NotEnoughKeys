package de.morrien.nekeys;

import de.morrien.nekeys.api.NekeysAPI;
import de.morrien.nekeys.command.CommandOpenVoiceCommandSettings;
import de.morrien.nekeys.preset.PresetManager;
import de.morrien.nekeys.url.CustomStreamHandlerFactory;
import de.morrien.nekeys.url.ResourceHandler;
import de.morrien.nekeys.voice.VoiceHandler;
import de.morrien.nekeys.voice.command.*;
import de.morrien.nekeys.voice.command.psi.SelectPsiSlotVoiceCommand;
import de.morrien.nekeys.voice.command.thaumcraft.SelectFocusVoiceCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by Timor Morrien
 */
@Mod(
        name = Reference.MODNAME,
        version = Reference.VERSION,
        modid = Reference.MODID,
        updateJSON = Reference.UPDATE_URL,
        acceptedMinecraftVersions = Reference.MINECRAFT_VERSION,
        clientSideOnly = true
)
public class NotEnoughKeys {

    @Mod.Instance
    public static NotEnoughKeys instance;

    public static Logger logger;
    private static GuiControls currentGui;
    private static GuiButton[] guiButtons = new GuiButton[10];
    private static boolean needsInit = true;
    public Path configDirectory;
    public VoiceHandler voiceHandler;
    public PresetManager presetManager;

    public NotEnoughKeys() {
        // This is necessary to enable Sphinx4 to load the custom minecraft-dictionary
        CustomStreamHandlerFactory factory = new CustomStreamHandlerFactory("resource", new ResourceHandler());
        URL.setURLStreamHandlerFactory(factory);
    }

    public void sendLocalizedStatusMessage(String langCode) {
        sendStatusMessage(new TextComponentTranslation(langCode));
    }

    public void sendStatusMessage(String message) {
        sendStatusMessage(new TextComponentString(message));
    }

    public void sendStatusMessage(ITextComponent message) {
        if (Minecraft.getMinecraft().player != null)
            Minecraft.getMinecraft().player.sendStatusMessage(message, true);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        logger = event.getModLog();

        configDirectory = event.getModConfigurationDirectory().toPath().resolve(Reference.MODID);
        try {
            if (!Files.exists(configDirectory)) {
                Files.createDirectory(configDirectory);
            }
        } catch (IOException e) {
            logger.warn("Unable to create config directory");
            logger.warn(e.toString());
        }

        voiceHandler = new VoiceHandler();
        voiceHandler.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Keybindings.register();

        NekeysAPI.addVoiceCommand(EmptyVoiceCommand.class, new EmptyVoiceCommand.Factory());
        NekeysAPI.addVoiceCommand(SimpleVoiceKeybind.class, new SimpleVoiceKeybind.Factory());
        NekeysAPI.addVoiceCommand(ToggleVoiceKeybind.class, new ToggleVoiceKeybind.Factory());
        NekeysAPI.addVoiceCommand(ChatVoiceCommand.class, new ChatVoiceCommand.Factory());
        NekeysAPI.addVoiceCommand(OpenGuiVoiceCommand.class, new OpenGuiVoiceCommand.Factory());
        NekeysAPI.addVoiceCommand(SelectPresetVoiceCommand.class, new SelectPresetVoiceCommand.Factory());
        //voiceHandler.bindPopup(VoicePressKey.class, PressKeyPopup.class);

        // Add VoiceCommands for mods
        if (Loader.isModLoaded("psi")) {
            NekeysAPI.addVoiceCommand(SelectPsiSlotVoiceCommand.class, new SelectPsiSlotVoiceCommand.Factory());
        }
        if (Loader.isModLoaded("thaumcraft")) {
            NekeysAPI.addVoiceCommand(SelectFocusVoiceCommand.class, new SelectFocusVoiceCommand.Factory());
        }
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandOpenVoiceCommandSettings());
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Keybindings.onKeyInput(event);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        voiceHandler.tickUpdate();
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            if (Keybindings.PUSH_TO_TALK.isKeyDown()) {
                Minecraft mc = Minecraft.getMinecraft();
                int posX = event.getResolution().getScaledWidth() - 15;
                int posY = event.getResolution().getScaledHeight() - 23;
                ResourceLocation microphone = new ResourceLocation(Reference.MODID, "textures/gui/microphone.png");

                mc.getTextureManager().bindTexture(microphone);
                GlStateManager.color(1, 1, 1, 0.8f);
                mc.ingameGUI.drawTexturedModalRect(posX, posY, 0, 0, 10, 18);
            }
        }
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu) {
            if (needsInit) {
                voiceHandler.init();
                presetManager = new PresetManager();
                try {
                    presetManager.loadFromConfig();
                } catch (IOException e) {
                    logger.error("Could not load preset config file");
                    logger.error(e.toString());
                }
                needsInit = false;
            }
        }
        if (event.getGui() instanceof GuiControls) {
            if (presetManager == null) presetManager = new PresetManager();
            currentGui = (GuiControls) event.getGui();

            GuiButton extenderButton = new GuiButton(1010100, currentGui.width / 2 - 5, 18 + 24, 10, 20, "+");
            extenderButton.packedFGColour = 0x55FF55;
            event.getButtonList().add(extenderButton);
            for (int i = 1; i <= 10; i++) {
                GuiButton button = new GuiButton(1010100 + i, currentGui.width / 2 - 155 + 65 + (i - 1) * 25, 66, 20, 20, String.valueOf(i));
                guiButtons[i - 1] = button;
                button.visible = false;
                if (presetManager.getPreset(i - 1) == presetManager.getCurrentPreset()) {
                    button.packedFGColour = 0x00FF00;
                }
                event.getButtonList().add(button);
            }
        } else if (currentGui != null) {
            if (presetManager != null)
                presetManager.getCurrentPreset().update();
            currentGui = null;
        }
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent event) {
        if (event.getGui() instanceof GuiControls) {
            if (guiButtons[0] != null && guiButtons[0].visible)
                event.getGui().drawString(Minecraft.getMinecraft().fontRenderer, I18n.format("gui.nekeys.presets"), event.getGui().width / 2 - 150, 72, 0xFFFFFF);
        }
    }

    @SubscribeEvent
    public void onGuiAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getGui() instanceof GuiControls) {
            int buttonID = event.getButton().id;
            if (buttonID == 1010100) {
                for (GuiButton guiButton : guiButtons) {
                    guiButton.visible = !guiButton.visible;
                }
                boolean enable = "+".equals(event.getButton().displayString);
                event.getButton().displayString = enable ? "-" : "+";
                GuiKeyBindingList keyBindingList = ObfuscationReflectionHelper.getPrivateValue(GuiControls.class, (GuiControls) event.getGui(), "keyBindingList", "t", "field_146494_r");
                keyBindingList.top += enable ? 26 : -26;
            } else if (buttonID >= 1010101 && buttonID <= 1010110) {
                int presetID = buttonID - 1010101;
                PresetManager.Preset preset = presetManager.getPreset(presetID);
                preset.load();
                for (GuiButton guiButton : guiButtons) {
                    if (presetManager.getPreset(guiButton.id - 1010101) == presetManager.getCurrentPreset()) {
                        guiButton.packedFGColour = 0x00FF00;
                    } else {
                        guiButton.packedFGColour = 0xFFFFFFFF;
                    }
                }
            }
        }
    }
}
