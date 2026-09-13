/*
 * Copyright (C) 2026 Marcus Hirt
 *
 * This software is free:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package se.hirt.claude.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command line entry point. Run from anywhere inside the marketplace checkout:
 *
 * <pre>
 * java -jar target/claude-plugins-tools.jar update            # every plugin to its latest release
 * java -jar target/claude-plugins-tools.jar update --check    # only report what is behind
 * java -jar target/claude-plugins-tools.jar install rpg-mcp 0.1.5
 * </pre>
 */
@Command(name = "claude-plugins-tools", mixinStandardHelpOptions = true, description = "Keeps the MCP bundles committed in this plugin marketplace in step with the releases of their projects.", subcommands = {
		Main.Update.class, Main.Install.class})
public final class Main implements Callable<Integer> {

	public static void main(String[] args) {
		CommandLine commandLine = new CommandLine(new Main());
		commandLine.setExecutionExceptionHandler((exception, command, parseResult) -> {
			command.getErr().println(command.getColorScheme().errorText("error: " + exception.getMessage()));
			return 1;
		});
		System.exit(commandLine.execute(args));
	}

	@Override
	public Integer call() {
		CommandLine.usage(this, System.out);
		return 0;
	}

	@Command(name = "update", mixinStandardHelpOptions = true, description = "Bring every plugin, or only the named ones, up to the latest release of its project. Plugins already at the latest release are left alone.")
	static final class Update implements Callable<Integer> {
		@Option(names = {"-n", "--check"}, description = "Report what would change, touch nothing.")
		boolean check;

		@Parameters(arity = "0..*", paramLabel = "PLUGIN", description = "Plugins to update; all when none are given.")
		List<String> names = new ArrayList<>();

		@Override
		public Integer call() throws Exception {
			Marketplace marketplace = Marketplace.locate();
			GitHub gitHub = new GitHub();
			Installer installer = new Installer(marketplace, gitHub, System.out);
			List<Marketplace.Plugin> plugins = names.isEmpty() ? marketplace.plugins()
					: names.stream().map(marketplace::plugin).toList();

			int behind = 0;
			int updated = 0;
			int failed = 0;
			for (Marketplace.Plugin plugin : plugins) {
				String latest;
				try {
					latest = Version.strip(gitHub.latestRelease(plugin.repo()).tag());
				} catch (Exception e) {
					System.err.println(plugin.name() + ": could not look up the latest release of " + plugin.repo()
							+ ": " + e.getMessage());
					failed++;
					continue;
				}
				int order = Version.compare(latest, plugin.version());
				if (order == 0) {
					System.out.println(plugin.name() + ": " + plugin.version() + " is the latest release");
					continue;
				}
				if (order < 0) {
					System.out.println(plugin.name() + ": " + plugin.version() + " is ahead of the latest release "
							+ latest + "; leaving it");
					continue;
				}
				behind++;
				if (check) {
					System.out.println(plugin.name() + ": " + plugin.version() + " -> " + latest + " (would update)");
					continue;
				}
				System.out.println(plugin.name() + ": " + plugin.version() + " -> " + latest);
				try {
					installer.install(plugin, latest);
					updated++;
				} catch (Exception e) {
					System.err.println(plugin.name() + ": update to " + latest + " failed: " + e.getMessage());
					failed++;
				}
			}

			if (check) {
				System.out.println(behind + " plugin(s) behind the latest release");
			} else {
				System.out.println(updated + " plugin(s) updated, " + failed + " failed");
				if (updated > 0) {
					System.out.println("next: review 'git status', then commit and push");
				}
			}
			return failed == 0 ? 0 : 1;
		}
	}

	@Command(name = "install", mixinStandardHelpOptions = true, description = "Install a specific release of one plugin's bundle, for example to pin a version or to pick up a re-signed bundle.")
	static final class Install implements Callable<Integer> {
		@Parameters(index = "0", paramLabel = "PLUGIN", description = "Plugin name as in marketplace.json.")
		String name;

		@Parameters(index = "1", paramLabel = "VERSION", description = "Release version, with or without the v prefix.")
		String version;

		@Override
		public Integer call() throws Exception {
			Marketplace marketplace = Marketplace.locate();
			Installer installer = new Installer(marketplace, new GitHub(), System.out);
			installer.install(marketplace.plugin(name), Version.strip(version));
			System.out.println("next: review 'git status', then commit and push");
			return 0;
		}
	}
}
