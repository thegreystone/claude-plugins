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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Pulls one released universal bundle into the marketplace and bumps the plugin's version.
 * <p>
 * Claude Desktop refuses plugins whose mcpServers points at a URL ("MCPB URL references are not
 * allowed in plugins because the target can change after review"), so each bundle is committed here
 * and referenced by relative path.
 */
final class Installer {
	private final Marketplace marketplace;
	private final GitHub gitHub;
	private final PrintStream out;

	Installer(Marketplace marketplace, GitHub gitHub, PrintStream out) {
		this.marketplace = marketplace;
		this.gitHub = gitHub;
		this.out = out;
	}

	/**
	 * Downloads the bundle for the given release of the plugin, checks it, replaces the committed
	 * bundle with it, and records the version in both manifests. Nothing is changed unless every
	 * check passes.
	 *
	 * @param version
	 *            the release version without the "v" prefix
	 */
	void install(Marketplace.Plugin plugin, String version) throws IOException, InterruptedException {
		String tag = "v" + version;
		String assetName = plugin.assetName(version);
		out.println("downloading " + assetName + " from " + plugin.repo() + " " + tag + "...");
		GitHub.Release release = gitHub.release(plugin.repo(), tag);
		GitHub.Asset asset = release.asset(assetName)
				.orElseThrow(() -> new IOException(tag + " of " + plugin.repo() + " has no asset " + assetName));
		if (asset.size() > Bundle.SIZE_LIMIT) {
			throw new IOException(assetName + " is " + asset.size() + " bytes, over the " + Bundle.SIZE_LIMIT
					+ " byte limit for a committed bundle; not committing it");
		}

		Path target = plugin.bundle();
		Files.createDirectories(target.getParent());
		Path download = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".download");
		long size;
		try {
			gitHub.download(asset, download);
			size = Files.size(download);
			if (size != asset.size()) {
				throw new IOException("downloaded " + size + " bytes of " + assetName + ", expected " + asset.size());
			}
			String declared = Bundle.manifestVersion(download);
			if (!version.equals(declared)) {
				throw new IOException("bundle manifest declares version " + declared + ", not " + version);
			}
			Files.move(download, target, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(download);
		}

		marketplace.setVersion(plugin, version);
		out.println("installed " + marketplace.relativize(target) + " (" + size + " bytes); " + plugin.name()
				+ " manifests now at " + version);
	}
}
