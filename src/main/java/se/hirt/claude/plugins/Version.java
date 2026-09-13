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

/**
 * Release version strings, as found in release tags ("v1.0.13") and manifests ("1.0.13").
 */
final class Version {
	private Version() {
	}

	/** "v1.2.3" becomes "1.2.3"; anything without the prefix is returned unchanged. */
	static String strip(String tag) {
		return tag.startsWith("v") ? tag.substring(1) : tag;
	}

	/**
	 * Compares numerically component by component when both versions are dotted integers, so
	 * "1.0.10" is newer than "1.0.9" and "1.0" equals "1.0.0". Anything else (pre-release suffixes,
	 * for example) falls back to plain string order, which still tells "same" from "different".
	 */
	static int compare(String a, String b) {
		int[] x = parts(a);
		int[] y = parts(b);
		if (x == null || y == null) {
			return strip(a).compareTo(strip(b));
		}
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			int p = i < x.length ? x[i] : 0;
			int q = i < y.length ? y[i] : 0;
			if (p != q) {
				return Integer.compare(p, q);
			}
		}
		return 0;
	}

	private static int[] parts(String version) {
		String[] pieces = strip(version).split("\\.", -1);
		int[] parts = new int[pieces.length];
		for (int i = 0; i < pieces.length; i++) {
			if (pieces[i].isEmpty() || !pieces[i].chars().allMatch(Character::isDigit)) {
				return null;
			}
			try {
				parts[i] = Integer.parseInt(pieces[i]);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return parts;
	}
}
