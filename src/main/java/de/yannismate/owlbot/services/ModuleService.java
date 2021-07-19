package de.yannismate.owlbot.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.yannismate.owlbot.model.Module;
import de.yannismate.owlbot.model.ModuleCommand;
import de.yannismate.owlbot.model.events.CommandExecutionEvent;
import de.yannismate.owlbot.modules.LoggingModule;
import de.yannismate.owlbot.modules.ModuleManagerModule;
import discord4j.core.event.domain.message.MessageCreateEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

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
  public ModuleService(DiscordService discordService, DatabaseService databaseService,
      BotEventService eventService) {
    discordService.getGateway().on(MessageCreateEvent.class).subscribe(event -> {
      //Check if message was sent on a guild
      if(event.getGuildId().isEmpty() || event.getMember().isEmpty()) return;

      databaseService.getGuildSettings(event.getGuildId().get()).thenAccept(guildSettings -> {
        String prefix = guildSettings.isPresent() ? guildSettings.get().getSettings().getPrefix() : "!";
        String msg = event.getMessage().getContent();

        //Check if message length is long enough to be a command
        if(!msg.startsWith(prefix) || msg.length() <= prefix.length()) return;

        String command = event.getMessage().getContent().substring(1).split(" ")[0];

        //Check if command exists
        if(!registeredCommands.containsKey(command)) return;

        ModuleCommandData cmd = registeredCommands.get(command);
        Module mod = this.getModuleByClass(cmd.getCmdClass());

        //Check if command module is enabled on the guild
        if(!mod.isAlwaysActive() && (guildSettings.isEmpty() ||
            !guildSettings.get().getEnabledModules().contains(cmd.getCmdClass().getSimpleName()))) {
          return;
        }

        //Check permissions
        if(cmd.getRequiredPermission().length() != 0) {
          if(guildSettings.isEmpty() ||
              !guildSettings.get().getPermissions().hasPermission(event.getMember().get().getId(), event.getMember().get().getRoleIds(), cmd.getRequiredPermission())) {
            discordService.createMessageInChannel(event.getGuildId().get(), event.getMessage().getChannelId(), "<@" + event.getMember().get().getId().asString() + "> Missing required permissions.").subscribe();
            return;
          }
        }


        try {
          Object result = cmd.getMethod().invoke(mod, event);
          //Publish CommandExecutionEvent to Bot EventSub
          if(result instanceof Mono) {
            Mono<Void> r = (Mono<Void>) result;
            r.doOnSuccess(then -> {
              String[] args = event.getMessage().getContent().split(" ");
              args = Arrays.copyOfRange(args, 1, args.length);

              CommandExecutionEvent commandExecutionEvent = new CommandExecutionEvent(
                  event.getGuildId().get(), event.getMessage().getChannelId(),
                  event.getMember().get(), command, args);

              eventService.publishEvent(commandExecutionEvent);
            }).subscribe();
          }
        } catch (IllegalAccessException | InvocationTargetException e) {
          logger.atError().addArgument(command).addArgument(e).log("Could not dispatch annotated command \"{}\"! {}");
        }
      });
    });

  }


  public void addModule(Module module) {
    this.modules.put(module.getClass(), module);

    //Register annotated commands
    for(Method m : module.getClass().getMethods()) {
      if(!m.isAnnotationPresent(ModuleCommand.class)) continue;

      ModuleCommand mc = m.getAnnotation(ModuleCommand.class);
      registeredCommands.put(mc.command(), new ModuleCommandData(m, mc.requiredPermission(),
          module.getClass()));
      logger.atInfo().addArgument(mc.command()).addArgument(module.getClass().getCanonicalName()).log("Registered command \"{}\" in {}");
    }
  }

  public Map<Class<? extends Module>, Module> getModules() {
    return Collections.unmodifiableMap(this.modules);
  }

  public Module getModuleByClass(Class<? extends Module> clazz) {
    return this.modules.get(clazz);
  }

  public Optional<Module> getModuleByName(String name) {
    return this.modules.values().stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
  }

  public Optional<Module> getModuleByClassName(String className) {
    return this.modules.values().stream().filter(m -> m.getClass().getSimpleName().equalsIgnoreCase(className)).findFirst();
  }

  public Map<String, ModuleCommandData> getCommandsByModuleClass(Class<? extends Module> moduleClass) {
    return this.registeredCommands.entrySet().stream()
        .filter(e -> e.getValue().cmdClass == moduleClass)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
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
