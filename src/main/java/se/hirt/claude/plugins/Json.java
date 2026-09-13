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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Reads and writes the JSON manifests. Files are written in the shape Spotless formats them to (see
 * the pom): one tab per level, arrays one element per line, no escaping beyond what JSON requires,
 * a single trailing newline.
 */
final class Json {
	private static final Gson GSON = new GsonBuilder()
			.setFormattingStyle(FormattingStyle.PRETTY.withIndent("\t").withNewline("\n")).disableHtmlEscaping()
			.serializeNulls().create();

	private Json() {
	}

	static JsonObject readObject(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonElement element = JsonParser.parseReader(reader);
			if (!element.isJsonObject()) {
				throw new IOException(file + " does not contain a JSON object");
			}
			return element.getAsJsonObject();
		} catch (JsonParseException e) {
			throw new IOException(file + " is not valid JSON: " + e.getMessage(), e);
		}
	}

	static String format(JsonElement element) {
		return GSON.toJson(element) + "\n";
	}

	static void write(Path file, JsonElement element) throws IOException {
		Files.writeString(file, format(element), StandardCharsets.UTF_8);
	}
}
