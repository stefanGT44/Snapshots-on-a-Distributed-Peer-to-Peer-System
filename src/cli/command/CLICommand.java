package cli.command;

/**
 * Defines a command on CLI. Each command has a name
 * and an execute, which takes and parses all the args.
 * @author stefanGT44
 *
 */
public interface CLICommand {

	/**
	 * Command name, as given by the user on the CLI.
	 */
	String commandName();
	
	/**
	 * All command logic goes here. <code>args</code> is the user's input, with command name taken out.
	 */
	void execute(String args);
}
