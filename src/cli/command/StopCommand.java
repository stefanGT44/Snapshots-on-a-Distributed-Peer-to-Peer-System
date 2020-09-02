package cli.command;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import cli.CLIParser;
import servent.SimpleServentListener;

public class StopCommand implements CLICommand {

	private CLIParser parser;
	private SimpleServentListener listener;
	private SnapshotCollector snapshotCollector;
	
	public StopCommand(CLIParser parser, SimpleServentListener listener,
			SnapshotCollector snapshotCollector) {
		this.parser = parser;
		this.listener = listener;
		this.snapshotCollector = snapshotCollector;
	}
	
	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("Stopping...");
		parser.stop();
		listener.stop();
		snapshotCollector.stop();
	}

}
