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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The marketplace manifest and the plugin manifests it points at. Everything the tool needs to know
 * about a plugin is derived from those two files: the GitHub repository from the marketplace
 * entry's {@code repository}, and the bundle file (and so the release asset name) from the plugin
 * manifest's {@code mcpServers}.
 */
final class Marketplace {
	static final Path MANIFEST = Path.of(".claude-plugin", "marketplace.json");
	private static final String BUNDLE_SUFFIX = ".mcpb";

	/**
	 * @param repo
	 *            GitHub "owner/name"
	 * @param manifest
	 *            the plugin's own plugin.json
	 * @param bundle
	 *            the committed .mcpb file the plugin manifest points at
	 */
	record Plugin(String name, String version, String repo, Path dir, Path manifest, Path bundle) {
		/** The release asset carrying the given version of this plugin's universal bundle. */
		String assetName(String version) {
			String file = bundle.getFileName().toString();
			return file.substring(0, file.length() - BUNDLE_SUFFIX.length()) + "-" + version + "-universal"
					+ BUNDLE_SUFFIX;
		}
	}

	private final Path root;
	private final JsonObject json;
	private final List<Plugin> plugins;

	private Marketplace(Path root, JsonObject json, List<Plugin> plugins) {
		this.root = root;
		this.json = json;
		this.plugins = plugins;
	}

	/**
	 * The marketplace whose root is the working directory or the nearest parent holding a
	 * marketplace manifest.
	 */
	static Marketplace locate() throws IOException {
		Path start = Path.of("").toAbsolutePath();
		for (Path dir = start; dir != null; dir = dir.getParent()) {
			if (Files.isRegularFile(dir.resolve(MANIFEST))) {
				return load(dir);
			}
		}
		throw new IOException("no " + MANIFEST + " in " + start + " or any directory above it");
	}

	static Marketplace load(Path root) throws IOException {
		JsonObject json = Json.readObject(root.resolve(MANIFEST));
		List<Plugin> plugins = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray("plugins")) {
			JsonObject entry = element.getAsJsonObject();
			String name = string(entry, "name", "a plugin entry in " + MANIFEST);
			Path dir = root.resolve(string(entry, "source", name)).normalize();
			Path manifest = dir.resolve(".claude-plugin").resolve("plugin.json");
			String servers = string(Json.readObject(manifest), "mcpServers", manifest.toString());
			if (!servers.endsWith(BUNDLE_SUFFIX)) {
				throw new IOException(manifest + ": mcpServers must be the path of an " + BUNDLE_SUFFIX + " bundle");
			}
			plugins.add(new Plugin(name, string(entry, "version", name), repo(string(entry, "repository", name)), dir,
					manifest, dir.resolve(servers).normalize()));
		}
		return new Marketplace(root, json, List.copyOf(plugins));
	}

	private static String string(JsonObject object, String key, String owner) throws IOException {
		JsonElement value = object.get(key);
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			throw new IOException(owner + " has no string " + key);
		}
		return value.getAsString();
	}

	/**
	 * "https://github.com/owner/name", with or without a trailing slash or ".git", to "owner/name".
	 */
	static String repo(String repository) throws IOException {
		String repo = repository.strip();
		repo = repo.replaceFirst("^https?://github\\.com/", "");
		repo = repo.replaceFirst("/+$", "");
		repo = repo.replaceFirst("\\.git$", "");
		if (!repo.matches("[^/\\s]+/[^/\\s]+")) {
			throw new IOException("repository '" + repository + "' is not a GitHub repository URL");
		}
		return repo;
	}

	Path root() {
		return root;
	}

	Path relativize(Path path) {
		return root.relativize(path.toAbsolutePath().normalize());
	}

	List<Plugin> plugins() {
		return plugins;
	}

	Plugin plugin(String name) {
		return plugins.stream().filter(p -> p.name().equals(name)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("unknown plugin '" + name + "'; known: "
						+ plugins.stream().map(Plugin::name).collect(Collectors.joining(", "))));
	}

	/**
	 * Records a new version in the plugin's marketplace entry and in its own manifest, rewriting
	 * both files.
	 */
	void setVersion(Plugin plugin, String version) throws IOException {
		for (JsonElement element : json.getAsJsonArray("plugins")) {
			JsonObject entry = element.getAsJsonObject();
			if (plugin.name().equals(entry.get("name").getAsString())) {
				entry.addProperty("version", version);
			}
		}
		Json.write(root.resolve(MANIFEST), json);
		JsonObject manifest = Json.readObject(plugin.manifest());
		manifest.addProperty("version", version);
		Json.write(plugin.manifest(), manifest);
	}
}
