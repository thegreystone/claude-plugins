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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * The GitHub releases REST API, for public repositories. Unauthenticated calls are rate limited to
 * 60 per hour per address; a token in GITHUB_TOKEN or GH_TOKEN is used for the API calls when
 * present. Asset downloads never carry the token: they redirect to another host, and the assets are
 * public anyway.
 */
final class GitHub {
	record Asset(String name, long size, URI url) {
	}

	record Release(String tag, List<Asset> assets) {
		Optional<Asset> asset(String name) {
			return assets.stream().filter(a -> a.name().equals(name)).findFirst();
		}
	}

	private static final URI API = URI.create("https://api.github.com/");
	private static final String USER_AGENT = "claude-plugins-tools (https://github.com/thegreystone/claude-plugins)";

	private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(20)).build();
	private final String token = token();

	private static String token() {
		for (String name : new String[] {"GITHUB_TOKEN", "GH_TOKEN"}) {
			String value = System.getenv(name);
			if (value != null && !value.isBlank()) {
				return value.strip();
			}
		}
		return null;
	}

	/** The latest published release, as GitHub defines it: drafts and pre-releases do not count. */
	Release latestRelease(String repo) throws IOException, InterruptedException {
		return release(repo, "repos/" + repo + "/releases/latest", "has no published release");
	}

	/** The release with the given tag, for example "v1.0.13". */
	Release release(String repo, String tag) throws IOException, InterruptedException {
		return release(repo, "repos/" + repo + "/releases/tags/" + tag, "has no release " + tag);
	}

	private Release release(String repo, String path, String notFound) throws IOException, InterruptedException {
		HttpRequest.Builder request = HttpRequest.newBuilder(API.resolve(path)).timeout(Duration.ofSeconds(60))
				.header("Accept", "application/vnd.github+json").header("X-GitHub-Api-Version", "2022-11-28")
				.header("User-Agent", USER_AGENT);
		if (token != null) {
			request.header("Authorization", "Bearer " + token);
		}
		HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() == 404) {
			throw new IOException(repo + " " + notFound);
		}
		if (response.statusCode() / 100 != 2) {
			String hint = response.statusCode() == 403 || response.statusCode() == 429
					? " (rate limited? set GITHUB_TOKEN)" : "";
			throw new IOException("GitHub returned HTTP " + response.statusCode() + " for " + path + hint);
		}
		try {
			JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
			List<Asset> assets = new ArrayList<>();
			for (JsonElement element : release.getAsJsonArray("assets")) {
				JsonObject asset = element.getAsJsonObject();
				assets.add(new Asset(asset.get("name").getAsString(), asset.get("size").getAsLong(),
						URI.create(asset.get("browser_download_url").getAsString())));
			}
			return new Release(release.get("tag_name").getAsString(), List.copyOf(assets));
		} catch (JsonParseException | IllegalStateException | NullPointerException e) {
			throw new IOException("unexpected response from GitHub for " + path + ": " + e.getMessage(), e);
		}
	}

	/** Downloads a release asset to a file, replacing whatever is there. */
	void download(Asset asset, Path target) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(asset.url()).timeout(Duration.ofMinutes(10))
				.header("User-Agent", USER_AGENT).build();
		HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));
		if (response.statusCode() / 100 != 2) {
			throw new IOException("download of " + asset.name() + " failed with HTTP " + response.statusCode());
		}
	}
}
