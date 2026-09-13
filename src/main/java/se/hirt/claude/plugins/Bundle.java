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
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * The MCP Bundle (.mcpb) files: a zip with a manifest.json and the server binaries.
 */
final class Bundle {
	/**
	 * GitHub warns about files above this size and refuses them at twice this size, and every
	 * version of a bundle stays in the history forever, so a bundle above it is not committed.
	 */
	static final long SIZE_LIMIT = 50_000_000L;

	private Bundle() {
	}

	/** The version the bundle's own manifest declares. */
	static String manifestVersion(Path bundle) throws IOException {
		try (ZipFile zip = new ZipFile(bundle.toFile())) {
			ZipEntry entry = zip.getEntry("manifest.json");
			if (entry == null) {
				throw new IOException(bundle.getFileName() + " has no manifest.json");
			}
			try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
				JsonElement version = JsonParser.parseReader(reader).getAsJsonObject().get("version");
				if (version == null || !version.isJsonPrimitive()) {
					throw new IOException("manifest.json in " + bundle.getFileName() + " declares no version");
				}
				return version.getAsString();
			} catch (JsonParseException | IllegalStateException e) {
				throw new IOException("manifest.json in " + bundle.getFileName() + " is not valid: " + e.getMessage(),
						e);
			}
		}
	}
}
