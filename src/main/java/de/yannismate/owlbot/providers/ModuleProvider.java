package de.yannismate.owlbot.providers;

import com.google.inject.Singleton;
import de.yannismate.owlbot.modules.LoggingModule;
import de.yannismate.owlbot.modules.Module;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class ModuleProvider {

  private final Set<Module> modules = new HashSet<>();
  private final Set<Class<? extends Module>> availableModules = Set.of(
      LoggingModule.class
  );

  public void addModule(Module module) {
    this.modules.add(module);
  }

  public Set<Module> getModules() {
    return Collections.unmodifiableSet(modules);
  }

  public Set<Class<? extends Module>> getAvailableModules() {
    return this.availableModules;
  }

}
