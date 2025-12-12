package dev.lvstrng.argon.module;

import dev.lvstrng.argon.Argon;
import dev.lvstrng.argon.event.events.ButtonListener;
import dev.lvstrng.argon.module.modules.client.ClickGUI;
import dev.lvstrng.argon.module.modules.combat.AimAssist;
import dev.lvstrng.argon.module.modules.combat.AnchorFill;
import dev.lvstrng.argon.module.modules.combat.AnchorMacro;
import dev.lvstrng.argon.module.modules.combat.AutoCrystal;
import dev.lvstrng.argon.module.modules.combat.AutoInventoryTotem;
import dev.lvstrng.argon.module.modules.combat.DoubleAnchor;
import dev.lvstrng.argon.module.modules.combat.HoverTotem;
import dev.lvstrng.argon.module.modules.combat.ShieldDisabler;
import dev.lvstrng.argon.module.modules.combat.TriggerBot;
import dev.lvstrng.argon.module.modules.misc.KeyPearl;
import dev.lvstrng.argon.module.modules.render.HUD;
import dev.lvstrng.argon.module.setting.KeybindSetting;
import dev.lvstrng.argon.utils.EncryptedString;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager implements ButtonListener {
	private final List<Module> modules = new ArrayList<>();

	public ModuleManager() {
		addModules();
		addKeybinds();
	}

	public void addModules() {
                add(new AimAssist());
                add(new AnchorFill());
                add(new AnchorMacro());
                add(new AutoCrystal());
                add(new AutoInventoryTotem());
                add(new DoubleAnchor());
                add(new HoverTotem());
                add(new ShieldDisabler());
                add(new TriggerBot());

                // Misc
                add(new KeyPearl());

                // Render / UI
                add(new HUD());
                add(new ClickGUI());
        }

	public List<Module> getEnabledModules() {
		return modules.stream()
				.filter(Module::isEnabled)
				.toList();
	}


	public List<Module> getModules() {
		return modules;
	}

	public void addKeybinds() {
		Argon.INSTANCE.getEventManager().add(ButtonListener.class, this);

		for (Module module : modules)
			module.addSetting(new KeybindSetting(EncryptedString.of("Keybind"), module.getKey(), true).setDescription(EncryptedString.of("Key to enabled the module")));
	}

	public List<Module> getModulesInCategory(Category category) {
		return modules.stream()
				.filter(module -> module.getCategory() == category)
				.toList();
	}

	@SuppressWarnings("unchecked")
	public <T extends Module> T getModule(Class<T> moduleClass) {
		return (T) modules.stream()
				.filter(moduleClass::isInstance)
				.findFirst()
				.orElse(null);
	}

	public void add(Module module) {
		modules.add(module);
	}

	@Override
	public void onButtonPress(ButtonEvent event) {
                modules.forEach(module -> {
                        if(module.getKey() == event.button && event.action == GLFW.GLFW_PRESS)
                                module.toggle();
                });
        }
}
