package org.async.async4j.samples;

import java.util.HashMap;
import java.util.Map;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.kohsuke.args4j.CmdLineParser;

public class Main {
	private static final Map<String, Cmd> COMMANDS = new HashMap<>();
	static{
		COMMANDS.put("parallelfor-bench", new ParallelForBenchCmd());
	}
	public static void main(String[] args) {
		if(args.length < 1){
			usage("Command name required !");
			System.exit(1);
		}
		
		String command = args[0];
		Cmd cmd = COMMANDS.get(command);
		if(cmd == null){
			usage("No such command: "+command);
			System.exit(1);
		}
		
		String[] arguments = new String[args.length - 1];
		System.arraycopy(args, 1, arguments, 0, arguments.length);
		
		try{
			CmdLineParser parser = new CmdLineParser(cmd);
			parser.parseArgument(arguments);
			Stopwatch stopwatch = SimonManager.getStopwatch(command);
			Split split = stopwatch.start();
			cmd.execute();
			split.stop();
			System.out.println("Result: " + stopwatch);
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}
	}

	private static void usage(String msg) {
		System.out.println(msg);
		System.out.println("Usage: java org.async.async4j.samples.Main <command> <parameters> ");
	}
}
