package de.yannismate.owlbot.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.yannismate.owlbot.modules.LoggingModule;
import de.yannismate.owlbot.modules.Module;
import de.yannismate.owlbot.modules.ModuleCommand;
import de.yannismate.owlbot.modules.ModuleManagerModule;
import discord4j.core.event.domain.message.MessageCreateEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ModuleService {

  private DiscordService discordService;

  private final Logger logger = LoggerFactory.getLogger(ModuleService.class);
  private final Map<Class<? extends Module>, Module> modules = new HashMap<>();
  private final Set<Class<? extends Module>> availableModules = Set.of(
      LoggingModule.class,
      ModuleManagerModule.class
  );
  private final Map<String, ModuleCommandData> registeredCommands = new HashMap<>();

  @Inject
  public ModuleService(DiscordService discordService) {
    this.discordService = discordService;
    discordService.getGateway().on(MessageCreateEvent.class).subscribe(event -> {
      //TODO: Get prefix for server
      String prefix = "!";
      String msg = event.getMessage().getContent();
      if(msg.startsWith(prefix) && msg.length() > prefix.length()) {
        String command = event.getMessage().getContent().substring(1).split(" ")[0];
        if(registeredCommands.containsKey(command)) {
          ModuleCommandData cmd = registeredCommands.get(command);
          //TODO: Check permissions
          try {
            cmd.getMethod().invoke(this.getModuleByClass(cmd.getCmdClass()), event);
          } catch (IllegalAccessException | InvocationTargetException e) {
            logger.atError().addArgument(command).addArgument(e).log("Could not dispatch annotated command \"{}\"! {}");
            e.printStackTrace();
          }
        }
      }
    });
  }


  public void addModule(Module module) {
    this.modules.put(module.getClass(), module);
    for(Method m : module.getClass().getMethods()) {
      if(m.isAnnotationPresent(ModuleCommand.class)) {
        ModuleCommand mc = m.getAnnotation(ModuleCommand.class);
        registeredCommands.put(mc.command(), new ModuleCommandData(m, mc.requiredPermission(),
            module.getClass()));
        logger.atInfo().addArgument(mc.command()).addArgument(module.getClass().getCanonicalName()).log("Registered command \"{}\" in {}");
      }
    }
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

  static class ModuleCommandData {
    private final Method method;
    private final String requiredPermission;
    private final Class cmdClass;

    public ModuleCommandData(Method method, String requiredPermission, Class cmdClass) {
      this.method = method;
      this.requiredPermission = requiredPermission;
      this.cmdClass = cmdClass;
    }

    public Method getMethod() {
      return method;
    }

    public String getRequiredPermission() {
      return requiredPermission;
    }

    public Class getCmdClass() {
      return cmdClass;
    }
  }

}
