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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionTest {
	@Test
	void stripsOnlyTheTagPrefix() {
		assertEquals("1.0.13", Version.strip("v1.0.13"));
		assertEquals("1.0.13", Version.strip("1.0.13"));
		assertEquals("", Version.strip("v"));
	}

	@Test
	void comparesNumerically() {
		assertTrue(Version.compare("1.0.10", "1.0.9") > 0);
		assertTrue(Version.compare("0.1.5", "0.2.0") < 0);
		assertEquals(0, Version.compare("1.0", "1.0.0"));
		assertEquals(0, Version.compare("v1.0.13", "1.0.13"));
	}

	@Test
	void fallsBackToStringOrderForAnythingElse() {
		assertEquals(0, Version.compare("1.0.0-rc1", "v1.0.0-rc1"));
		assertTrue(Version.compare("1.0.0-rc1", "1.0.0-rc2") < 0);
		assertTrue(Version.compare("1.0.0", "1.0.0-rc1") != 0);
	}
}
