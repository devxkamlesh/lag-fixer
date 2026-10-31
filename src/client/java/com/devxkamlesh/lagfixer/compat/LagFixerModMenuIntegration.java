package com.devxkamlesh.lagfixer.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers the Lag Fixer config screen with Mod Menu.
 *
 * Opens the dependency-free native dashboard. Detailed Cloth Config
 * settings remain available from the dashboard when installed.
 *
 * Declared as a "modmenu" entrypoint in fabric.mod.json.
 */
public final class LagFixerModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return com.devxkamlesh.lagfixer.gui.LagFixerDashboardScreen::new;
    }
}
