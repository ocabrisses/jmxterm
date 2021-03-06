package org.cyclopsgroup.jmxterm.cc;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.cyclopsgroup.jmxterm.Command;
import org.cyclopsgroup.jmxterm.CommandFactory;
import org.cyclopsgroup.jmxterm.utils.ConfigurationUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory class of commands which knows how to create Command class with given command name
 * 
 * @author <a href="mailto:jiaqi.guo@gmail.com">Jiaqi Guo</a>
 */
class PredefinedCommandFactory implements CommandFactory {
  private final CommandFactory delegate;

  /**
   * Default constructor
   * 
   * @throws IOException Thrown when Jar is corrupted
   */
  PredefinedCommandFactory() throws IOException {
    this("META-INF/cyclopsgroup/jmxterm.properties");
  }

  /**
   * Constructor which builds up command types
   * 
   * @param configPath Path of configuration file in classpath
   * @throws IOException Thrown when Jar is corrupted
   */
  @SuppressWarnings("unchecked")
  public PredefinedCommandFactory(String configPath) throws IOException {
    Validate.notNull(configPath, "configPath can't be NULL");
    ClassLoader classLoader = getClass().getClassLoader();
    Configuration props = ConfigurationUtils.loadFromOverlappingResources(configPath, classLoader);
    if (props == null) {
      throw new FileNotFoundException(
          "Couldn't load configuration from " + configPath + ", classpath has problem");
    }
    props = props.subset("jmxterm.commands");
    if (props == null) {
      throw new IOException("Expected configuration doesn't appear in " + configPath);
    }
    HashMap<String, Class<? extends Command>> commands =
        new HashMap<String, Class<? extends Command>>();
    for (String name : props.getStringArray("name")) {
      String type = props.getString(name + ".type");
      Class<? extends Command> commandType;
      try {
        commandType = (Class<? extends Command>) classLoader.loadClass(type);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Couldn't load type " + type, e);
      }
      commands.put(name, commandType);
      String[] aliases = props.getStringArray(name + ".alias");
      if (!ArrayUtils.isEmpty(aliases)) {
        for (String alias : aliases) {
          commands.put(alias, commandType);
        }
      }
    }
    commands.put("help", HelpCommand.class);
    delegate = new TypeMapCommandFactory(commands);
  }

  @Override
  public Command createCommand(String commandName) {
    return delegate.createCommand(commandName);
  }

  @Override
  public Map<String, Class<? extends Command>> getCommandTypes() {
    return delegate.getCommandTypes();
  }
}
