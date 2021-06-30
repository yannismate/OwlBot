package de.yannismate.owlbot.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.yannismate.owlbot.modules.LoggingModule;
import de.yannismate.owlbot.modules.Module;
import de.yannismate.owlbot.modules.ModuleCommand;
import de.yannismate.owlbot.modules.ModuleManagerModule;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.Channel;
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

  private final Logger logger = LoggerFactory.getLogger(ModuleService.class);
  private final Map<Class<? extends Module>, Module> modules = new HashMap<>();
  private final Set<Class<? extends Module>> availableModules = Set.of(
      LoggingModule.class,
      ModuleManagerModule.class
  );
  private final Map<String, ModuleCommandData> registeredCommands = new HashMap<>();

  @Inject
  public ModuleService(DiscordService discordService, DatabaseService databaseService) {
    discordService.getGateway().on(MessageCreateEvent.class).subscribe(event -> {
      if(event.getGuildId().isEmpty() || event.getMember().isEmpty()) return;
      databaseService.getGuildSettings(event.getGuildId().get()).thenAccept(guildSettings -> {
        String prefix = guildSettings.isPresent() ? guildSettings.get().getSettings().getPrefix() : "!";
        String msg = event.getMessage().getContent();
        if(msg.startsWith(prefix) && msg.length() > prefix.length()) {
          String command = event.getMessage().getContent().substring(1).split(" ")[0];
          if(registeredCommands.containsKey(command)) {
            ModuleCommandData cmd = registeredCommands.get(command);
            Module mod = this.getModuleByClass(cmd.getCmdClass());
            if(!mod.isAlwaysActive() && (guildSettings.isEmpty() ||
                !guildSettings.get().getEnabledModules().contains(cmd.getCmdClass().getSimpleName()))) {
              return;
            }
            if(cmd.getRequiredPermission().length() != 0) {
              if(guildSettings.isEmpty() ||
                  !guildSettings.get().getPermissions().hasPermission(event.getMember().get().getId(), event.getMember().get().getRoleIds(), cmd.getRequiredPermission())) {
                event.getMessage().getChannel().flatMap(c -> c.createMessage("<@" + event.getMember().get().getId().asString() + "> Missing required permissions.")).subscribe();
                return;
              }
            }
            try {
              cmd.getMethod().invoke(mod, event);
            } catch (IllegalAccessException | InvocationTargetException e) {
              logger.atError().addArgument(command).addArgument(e).log("Could not dispatch annotated command \"{}\"! {}");
              e.printStackTrace();
            }
          }
        }
      });
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
    private final Class<? extends Module> cmdClass;

    public ModuleCommandData(Method method, String requiredPermission, Class<? extends Module> cmdClass) {
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

    public Class<? extends Module> getCmdClass() {
      return cmdClass;
    }
  }

}
