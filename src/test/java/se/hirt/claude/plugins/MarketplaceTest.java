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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarketplaceTest {
	private static final String MARKETPLACE = """
			{
				"name": "test-plugins",
				"owner": {
					"name": "Tester"
				},
				"plugins": [
					{
						"name": "demo",
						"source": "./demo",
						"description": "A demo.",
						"version": "1.0.0",
						"repository": "https://github.com/owner/demo",
						"tags": [
							"a",
							"b"
						]
					}
				]
			}
			""";

	private static final String PLUGIN = """
			{
				"name": "demo",
				"version": "1.0.0",
				"mcpServers": "./demo-server.mcpb"
			}
			""";

	private static Path fixture(Path dir, String marketplace, String plugin) throws IOException {
		Files.createDirectories(dir.resolve(".claude-plugin"));
		Files.writeString(dir.resolve(Marketplace.MANIFEST), marketplace, StandardCharsets.UTF_8);
		Files.createDirectories(dir.resolve("demo/.claude-plugin"));
		Files.writeString(dir.resolve("demo/.claude-plugin/plugin.json"), plugin, StandardCharsets.UTF_8);
		return dir;
	}

	@Test
	void derivesEverythingFromTheManifests(@TempDir
	Path dir) throws IOException {
		Marketplace marketplace = Marketplace.load(fixture(dir, MARKETPLACE, PLUGIN));
		assertEquals(1, marketplace.plugins().size());
		Marketplace.Plugin demo = marketplace.plugin("demo");
		assertEquals("1.0.0", demo.version());
		assertEquals("owner/demo", demo.repo());
		assertEquals(dir.resolve("demo").normalize(), demo.dir());
		assertEquals(dir.resolve("demo/.claude-plugin/plugin.json").normalize(), demo.manifest());
		assertEquals(dir.resolve("demo/demo-server.mcpb").normalize(), demo.bundle());
		assertEquals("demo-server-1.0.1-universal.mcpb", demo.assetName("1.0.1"));
	}

	@Test
	void namesTheKnownPluginsWhenOneIsUnknown(@TempDir
	Path dir) throws IOException {
		Marketplace marketplace = Marketplace.load(fixture(dir, MARKETPLACE, PLUGIN));
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> marketplace.plugin("nope"));
		assertTrue(e.getMessage().contains("demo"), e.getMessage());
	}

	@Test
	void setVersionRewritesBothManifestsInPlace(@TempDir
	Path dir) throws IOException {
		Marketplace marketplace = Marketplace.load(fixture(dir, MARKETPLACE, PLUGIN));
		marketplace.setVersion(marketplace.plugin("demo"), "1.0.1");

		assertEquals(MARKETPLACE.replace("\"version\": \"1.0.0\"", "\"version\": \"1.0.1\""),
				Files.readString(dir.resolve(Marketplace.MANIFEST), StandardCharsets.UTF_8));
		assertEquals(PLUGIN.replace("\"version\": \"1.0.0\"", "\"version\": \"1.0.1\""),
				Files.readString(dir.resolve("demo/.claude-plugin/plugin.json"), StandardCharsets.UTF_8));
		assertEquals("1.0.1", Marketplace.load(dir).plugin("demo").version());
	}

	@Test
	void acceptsTheUsualRepositorySpellings() throws IOException {
		assertEquals("owner/demo", Marketplace.repo("https://github.com/owner/demo"));
		assertEquals("owner/demo", Marketplace.repo("https://github.com/owner/demo/"));
		assertEquals("owner/demo", Marketplace.repo("https://github.com/owner/demo.git"));
		assertEquals("owner/demo", Marketplace.repo("owner/demo"));
		assertThrows(IOException.class, () -> Marketplace.repo("https://gitlab.com/owner/demo"));
		assertThrows(IOException.class, () -> Marketplace.repo("demo"));
	}

	@Test
	void requiresABundleInThePluginManifest(@TempDir
	Path dir) throws IOException {
		fixture(dir, MARKETPLACE, PLUGIN.replace("./demo-server.mcpb", "./mcp.json"));
		IOException e = assertThrows(IOException.class, () -> Marketplace.load(dir));
		assertTrue(e.getMessage().contains("mcpServers"), e.getMessage());
	}
}
