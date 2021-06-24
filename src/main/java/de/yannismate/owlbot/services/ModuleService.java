package de.yannismate.owlbot.services;

import com.google.inject.Singleton;
import de.yannismate.owlbot.modules.LoggingModule;
import de.yannismate.owlbot.modules.Module;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
public class ModuleService {

  private final Map<Class<? extends Module>, Module> modules = new HashMap<>();
  private final Set<Class<? extends Module>> availableModules = Set.of(
      LoggingModule.class
  );

  public void addModule(Module module) {
    this.modules.put(module.getClass(), module);
  }

  public Map<Class<? extends Module>, Module> getModules() {
    return Collections.unmodifiableMap(this.modules);
  }

  public Module getModuleByClass(Class<? extends Module> clazz) {
    return this.modules.get(clazz);
  }

  public Set<Class<? extends Module>> getAvailableModules() {
    return this.availableModules;
  }

}
