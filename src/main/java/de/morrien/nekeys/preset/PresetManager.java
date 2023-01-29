package de.morrien.nekeys.preset;

import de.morrien.nekeys.NotEnoughKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.settings.KeyModifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Timor Morrien
 */
public class PresetManager {
    private static final int PRESET_COUNT = 10;

    private final Preset DEFAULT_PRESET;
    private final Path configFile;
    private List<Preset> presets;
    private Preset currentPreset;

    public PresetManager() {
        configFile = NotEnoughKeys.instance.configDirectory.resolve("preset.config");
        Preset tmpPreset = new Preset();
        for (KeyBinding keybinding : Minecraft.getMinecraft().gameSettings.keyBindings) {
            keybinding.setToDefault();
        }
        DEFAULT_PRESET = new Preset();
        tmpPreset.load();
        currentPreset = null;
    }

    public Preset getPreset(int id) {
        if (id >= 0 && id < presets.size()) {
            return presets.get(id);
        }
        return null;
    }

    public Preset getCurrentPreset() {
        return currentPreset;
    }

    public void saveToConfig() throws IOException {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= presets.size(); i++) {
            Preset preset = presets.get(i - 1);
            lines.add(preset == currentPreset ? i + " active" : String.valueOf(i));
            for (KeyBindingInformation keyBindingInformation : preset.keyBindingInformations) {
                lines.add("- " + keyBindingInformation.keyBindingId + "   " + keyBindingInformation.keyCode + "   " + keyBindingInformation.keyModifier.name());
            }
        }
        lines.add("End");
        Files.write(configFile, lines);
    }

    public void loadFromConfig() throws IOException {
        presets = new ArrayList<>();
        if (!Files.exists(configFile)) {
            for (int i = 1; i <= PRESET_COUNT; i++) {
                if (i == 1) {
                    Preset tmp = new Preset();
                    if (!DEFAULT_PRESET.equals(tmp)) {
                        presets.add(tmp);
                        continue;
                    }
                }
                presets.add(DEFAULT_PRESET.clone());
            }
            presets.get(0).load();
            saveToConfig();
        } else {
            List<String> lines = Files.readAllLines(configFile);
            List<KeyBindingInformation> kbis = null;
            boolean isCurrent = false;
            for (String line : lines) {
                if (line.startsWith("- ")) {
                    String[] split = line.split(" {3}");
                    kbis.add(new KeyBindingInformation(split[0].substring(2), Integer.parseInt(split[1].replace(" ", "")), KeyModifier.valueOf(split[2])));
                } else {
                    if (kbis != null) {
                        Preset p = new Preset(kbis);
                        p.repair();
                        presets.add(p);
                        if (isCurrent) {
                            p.load();
                            isCurrent = false;
                        }
                    }
                    if (line.contains("active")) {
                        isCurrent = true;
                    }
                    kbis = new ArrayList<>();
                }
            }
            if (currentPreset == null) presets.get(0).load();
        }
    }

    public static class KeyBindingInformation {
        public String keyBindingId;
        public int keyCode;
        public KeyModifier keyModifier;

        public KeyBindingInformation(KeyBinding keyBinding) {
            this.keyBindingId = keyBinding.getKeyDescription();
            this.keyCode = keyBinding.getKeyCode();
            this.keyModifier = keyBinding.getKeyModifier();
        }

        public KeyBindingInformation(String keyBindingId, int keyCode, KeyModifier keyModifier) {
            this.keyBindingId = keyBindingId;
            this.keyCode = keyCode;
            this.keyModifier = keyModifier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KeyBindingInformation that = (KeyBindingInformation) o;

            if (keyCode != that.keyCode) return false;
            if (keyBindingId != null ? !keyBindingId.equals(that.keyBindingId) : that.keyBindingId != null)
                return false;
            return keyModifier == that.keyModifier;
        }
    }

    public class Preset implements Cloneable {
        private List<KeyBindingInformation> keyBindingInformations;

        public Preset() {
            keyBindingInformations = new ArrayList<>();
            for (KeyBinding keyBinding : Minecraft.getMinecraft().gameSettings.keyBindings) {
                keyBindingInformations.add(new KeyBindingInformation(keyBinding));
            }
        }

        private Preset(List<KeyBindingInformation> keyBindingInformations) {
            this.keyBindingInformations = keyBindingInformations;
        }

        public void load() {
            if (this == currentPreset) {
                if (Minecraft.getMinecraft().player != null) {
                    Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentTranslation("nekeys.status.preset.alreadyLoaded", presets.indexOf(this) + 1), true);
                }
                return;
            }
            if (currentPreset != null) currentPreset.update();
            for (KeyBinding keyBinding : Minecraft.getMinecraft().gameSettings.keyBindings) {
                final String keyBindingId = keyBinding.getKeyDescription();
                for (KeyBindingInformation keyBindingInformation : keyBindingInformations) {
                    if (keyBindingId.equals(keyBindingInformation.keyBindingId)) {
                        keyBinding.setKeyModifierAndCode(keyBindingInformation.keyModifier, keyBindingInformation.keyCode);
                    }
                }
            }
            Minecraft.getMinecraft().gameSettings.saveOptions();
            KeyBinding.resetKeyBindingArrayAndHash();
            currentPreset = this;
            if (Minecraft.getMinecraft().player != null) {
                Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentTranslation("nekeys.status.preset.change", presets.indexOf(this) + 1), true);
            }
        }

        public void update() {
            if (currentPreset != this) return;
            keyBindingInformations = new ArrayList<>();
            for (KeyBinding keyBinding : Minecraft.getMinecraft().gameSettings.keyBindings) {
                keyBindingInformations.add(new KeyBindingInformation(keyBinding));
            }
            try {
                saveToConfig();
            } catch (IOException e) {
                NotEnoughKeys.logger.warn("Could not save preset changes to config file");
                NotEnoughKeys.logger.throwing(e);
            }
        }

        public boolean isComplete() {
            if (Minecraft.getMinecraft().gameSettings.keyBindings.length != keyBindingInformations.size()) return false;
            for (KeyBinding keyBinding : Minecraft.getMinecraft().gameSettings.keyBindings) {
                boolean found = false;
                for (KeyBindingInformation keyBindingInformation : keyBindingInformations) {
                    if (keyBinding.getKeyDescription().equals(keyBindingInformation)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }

        public void repair() {
            if (isComplete()) return;
            KeyBinding[] keyBindings = Minecraft.getMinecraft().gameSettings.keyBindings;
            for (int i = 0; i < keyBindings.length; i++) {
                KeyBinding keyBinding = keyBindings[i];
                boolean found = false;
                for (KeyBindingInformation keyBindingInformation : keyBindingInformations) {
                    if (keyBinding.getKeyDescription().equals(keyBindingInformation.keyBindingId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    keyBindingInformations.add(i, new KeyBindingInformation(keyBinding));
                }
            }
        }

        @Override
        public Preset clone() {
            List<KeyBindingInformation> kbis = new ArrayList<>();
            for (KeyBindingInformation kbi : keyBindingInformations) {
                kbis.add(new KeyBindingInformation(kbi.keyBindingId, kbi.keyCode, kbi.keyModifier));
            }
            return new Preset(kbis);
        }
    }
}
