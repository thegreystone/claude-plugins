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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BundleTest {
	private static Path zip(Path file, String entryName, String content) throws IOException {
		try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
			zip.putNextEntry(new ZipEntry(entryName));
			zip.write(content.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		return file;
	}

	@Test
	void readsTheVersionFromTheManifest(@TempDir
	Path dir) throws IOException {
		Path bundle = zip(dir.resolve("demo-server.mcpb"), "manifest.json",
				"{\"manifest_version\": \"0.3\", \"name\": \"demo\", \"version\": \"2.0.0\"}");
		assertEquals("2.0.0", Bundle.manifestVersion(bundle));
	}

	@Test
	void rejectsABundleWithoutAManifestOrVersion(@TempDir
	Path dir) throws IOException {
		Path noManifest = zip(dir.resolve("a.mcpb"), "server/binary", "");
		assertThrows(IOException.class, () -> Bundle.manifestVersion(noManifest));
		Path noVersion = zip(dir.resolve("b.mcpb"), "manifest.json", "{\"name\": \"demo\"}");
		assertThrows(IOException.class, () -> Bundle.manifestVersion(noVersion));
		Path notJson = zip(dir.resolve("c.mcpb"), "manifest.json", "nope");
		assertThrows(IOException.class, () -> Bundle.manifestVersion(notJson));
	}
}
