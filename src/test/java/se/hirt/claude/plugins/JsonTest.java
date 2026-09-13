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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class JsonTest {
	@Test
	void formatsWithTabsAndOneArrayElementPerLine() {
		JsonObject object = new JsonObject();
		object.addProperty("name", "Marcus Hirt's plugins <3 & more");
		JsonArray tags = new JsonArray();
		tags.add("rpg");
		tags.add("srd");
		object.add("tags", tags);
		object.add("empty", new JsonArray());
		JsonObject owner = new JsonObject();
		owner.addProperty("url", "https://www.hirt.se");
		object.add("owner", owner);

		assertEquals("""
				{
					"name": "Marcus Hirt's plugins <3 & more",
					"tags": [
						"rpg",
						"srd"
					],
					"empty": [],
					"owner": {
						"url": "https://www.hirt.se"
					}
				}
				""", Json.format(object));
	}

	@Test
	void roundTripKeepsKeyOrderAndLayout(@TempDir
	Path dir) throws IOException {
		String text = """
				{
					"z": "last",
					"a": "first",
					"list": [
						"x"
					]
				}
				""";
		Path file = dir.resolve("plugin.json");
		Files.writeString(file, text, StandardCharsets.UTF_8);
		Json.write(file, Json.readObject(file));
		assertEquals(text, Files.readString(file, StandardCharsets.UTF_8));
	}

	@Test
	void rejectsAnythingButAnObject(@TempDir
	Path dir) throws IOException {
		Path file = dir.resolve("list.json");
		Files.writeString(file, "[1, 2]", StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> Json.readObject(file));
		Files.writeString(file, "{not json", StandardCharsets.UTF_8);
		assertThrows(IOException.class, () -> Json.readObject(file));
	}
}
