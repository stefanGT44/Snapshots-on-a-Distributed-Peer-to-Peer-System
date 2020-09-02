package cli.command;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;

public class BitcakeInfoCommand implements CLICommand {

	private SnapshotCollector collector;
	
	public BitcakeInfoCommand(SnapshotCollector collector) {
		this.collector = collector;
	}
	
	@Override
	public String commandName() {
		return "bitcake_info";
	}

	@Override
	public void execute(String args) {
		if (AppConfig.myServentInfo.isInit())
			collector.startCollecting();
		else
			AppConfig.timestampedErrorPrint("Node " + AppConfig.myServentInfo.getId() + " is not an initiator node.");
	}

}
