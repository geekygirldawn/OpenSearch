/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch;

import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.lucene.Lucene;
import org.opensearch.common.settings.Settings;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.test.VersionUtils;
import org.hamcrest.Matchers;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.opensearch.LegacyESVersion.V_6_3_0;
import static org.opensearch.LegacyESVersion.V_7_0_0;
import static org.opensearch.Version.MASK;
import static org.opensearch.test.VersionUtils.allVersions;
import static org.opensearch.test.VersionUtils.randomVersion;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.sameInstance;

public class VersionTests extends OpenSearchTestCase {

    public void testVersionComparison() throws Exception {
        assertThat(V_6_3_0.before(V_7_0_0), is(true));
        assertThat(V_6_3_0.before(V_6_3_0), is(false));
        assertThat(V_7_0_0.before(V_6_3_0), is(false));

        assertThat(V_6_3_0.onOrBefore(V_7_0_0), is(true));
        assertThat(V_6_3_0.onOrBefore(V_6_3_0), is(true));
        assertThat(V_7_0_0.onOrBefore(V_6_3_0), is(false));

        assertThat(V_6_3_0.after(V_7_0_0), is(false));
        assertThat(V_6_3_0.after(V_6_3_0), is(false));
        assertThat(V_7_0_0.after(V_6_3_0), is(true));

        assertThat(V_6_3_0.onOrAfter(V_7_0_0), is(false));
        assertThat(V_6_3_0.onOrAfter(V_6_3_0), is(true));
        assertThat(V_7_0_0.onOrAfter(V_6_3_0), is(true));

        assertTrue(Version.fromString("5.0.0-alpha2").onOrAfter(Version.fromString("5.0.0-alpha1")));
        assertTrue(Version.fromString("5.0.0").onOrAfter(Version.fromString("5.0.0-beta2")));
        assertTrue(Version.fromString("5.0.0-rc1").onOrAfter(Version.fromString("5.0.0-beta24")));
        assertTrue(Version.fromString("5.0.0-alpha24").before(Version.fromString("5.0.0-beta0")));

        assertThat(V_6_3_0, is(lessThan(V_7_0_0)));
        assertThat(V_6_3_0.compareTo(V_6_3_0), is(0));
        assertThat(V_7_0_0, is(greaterThan(V_6_3_0)));
    }

    public void testMin() {
        assertEquals(VersionUtils.getPreviousVersion(), Version.min(Version.CURRENT, VersionUtils.getPreviousVersion()));
        assertEquals(Version.fromString("1.0.1"), Version.min(Version.fromString("1.0.1"), Version.CURRENT));
        Version version = VersionUtils.randomVersion(random());
        Version version1 = VersionUtils.randomVersion(random());
        if (version.id <= version1.id) {
            assertEquals(version, Version.min(version1, version));
        } else {
            assertEquals(version1, Version.min(version1, version));
        }
    }

    public void testMax() {
        assertEquals(Version.CURRENT, Version.max(Version.CURRENT, VersionUtils.getPreviousVersion()));
        assertEquals(Version.CURRENT, Version.max(Version.fromString("1.0.1"), Version.CURRENT));
        Version version = VersionUtils.randomVersion(random());
        Version version1 = VersionUtils.randomVersion(random());
        if (version.id >= version1.id) {
            assertEquals(version, Version.max(version1, version));
        } else {
            assertEquals(version1, Version.max(version1, version));
        }
    }

    public void testMinimumIndexCompatibilityVersion() {
        assertEquals(Version.fromId(5000099), LegacyESVersion.V_6_0_0_beta1.minimumIndexCompatibilityVersion());
        assertEquals(Version.fromId(2000099), Version.fromId(5000099).minimumIndexCompatibilityVersion());
        assertEquals(Version.fromId(2000099),
                Version.fromId(5010000).minimumIndexCompatibilityVersion());
        assertEquals(Version.fromId(2000099),
                Version.fromId(5000001).minimumIndexCompatibilityVersion());
    }

    public void testVersionConstantPresent() {
        assertThat(Version.CURRENT, sameInstance(Version.fromId(Version.CURRENT.id)));
        assertThat(Version.CURRENT.luceneVersion, equalTo(org.apache.lucene.util.Version.LATEST));
        final int iters = scaledRandomIntBetween(20, 100);
        for (int i = 0; i < iters; i++) {
            Version version = randomVersion(random());
            assertThat(version, sameInstance(Version.fromId(version.id)));
            assertThat(version.luceneVersion, sameInstance(Version.fromId(version.id).luceneVersion));
        }
    }

    public void testCURRENTIsLatest() {
        final int iters = scaledRandomIntBetween(100, 1000);
        for (int i = 0; i < iters; i++) {
            Version version = randomVersion(random());
            if (version != Version.CURRENT) {
                assertThat("Version: " + version + " should be before: " + Version.CURRENT + " but wasn't",
                    version.before(Version.CURRENT), is(true));
            }
        }
    }

    public void testVersionFromString() {
        final int iters = scaledRandomIntBetween(100, 1000);
        for (int i = 0; i < iters; i++) {
            Version version = randomVersion(random());
            assertThat(Version.fromString(version.toString()), sameInstance(version));
        }
    }

    public void testTooLongVersionFromString() {
        Exception e = expectThrows(IllegalArgumentException.class, () -> Version.fromString("1.0.0.1.3"));
        assertThat(e.getMessage(), containsString("needs to contain major, minor, and revision"));
    }

    public void testTooShortVersionFromString() {
        Exception e = expectThrows(IllegalArgumentException.class, () -> Version.fromString("1.0"));
        assertThat(e.getMessage(), containsString("needs to contain major, minor, and revision"));
    }

    public void testWrongVersionFromString() {
        Exception e = expectThrows(IllegalArgumentException.class, () -> Version.fromString("WRONG.VERSION"));
        assertThat(e.getMessage(), containsString("needs to contain major, minor, and revision"));
    }

    public void testVersionNoPresentInSettings() {
        Exception e = expectThrows(IllegalStateException.class, () -> Version.indexCreated(Settings.builder().build()));
        assertThat(e.getMessage(), containsString("[index.version.created] is not present"));
    }

    public void testIndexCreatedVersion() {
        // an actual index has a IndexMetadata.SETTING_INDEX_UUID
        final LegacyESVersion version = LegacyESVersion.V_6_0_0_beta1;
        assertEquals(version, Version.indexCreated(
            Settings.builder()
                .put(IndexMetadata.SETTING_INDEX_UUID, "foo")
                .put(IndexMetadata.SETTING_VERSION_CREATED, version)
                .build()));
    }

    public void testMinCompatVersion() {
        Version major = Version.fromString("2.0.0");
        assertThat(Version.fromString("2.0.0").minimumCompatibilityVersion(), equalTo(major));
        assertThat(Version.fromString("2.2.0").minimumCompatibilityVersion(), equalTo(major));
        assertThat(Version.fromString("2.3.0").minimumCompatibilityVersion(), equalTo(major));

        Version major5x = Version.fromString("5.0.0");
        assertThat(Version.fromString("5.0.0").minimumCompatibilityVersion(), equalTo(major5x));
        assertThat(Version.fromString("5.2.0").minimumCompatibilityVersion(), equalTo(major5x));
        assertThat(Version.fromString("5.3.0").minimumCompatibilityVersion(), equalTo(major5x));

        Version major56x = Version.fromString("5.6.0");
        assertThat(LegacyESVersion.V_6_5_0.minimumCompatibilityVersion(), equalTo(major56x));
        assertThat(LegacyESVersion.V_6_3_1.minimumCompatibilityVersion(), equalTo(major56x));

        // from 7.0 on we are supporting the latest minor of the previous major... this might fail once we add a new version ie. 5.x is
        // released since we need to bump the supported minor in Version#minimumCompatibilityVersion()
        Version lastVersion = LegacyESVersion.V_6_8_0; // TODO: remove this once min compat version is a constant instead of method
        assertEquals(lastVersion.major, LegacyESVersion.V_7_0_0.minimumCompatibilityVersion().major);
        assertEquals("did you miss to bump the minor in Version#minimumCompatibilityVersion()",
                lastVersion.minor, LegacyESVersion.V_7_0_0.minimumCompatibilityVersion().minor);
        assertEquals(0, LegacyESVersion.V_7_0_0.minimumCompatibilityVersion().revision);
    }

    public void testToString() {
        // with 2.0.beta we lowercase
        assertEquals("2.0.0-beta1", Version.fromString("2.0.0-beta1").toString());
        assertEquals("5.0.0-alpha1", Version.fromId(5000001).toString());
        assertEquals("2.3.0", Version.fromString("2.3.0").toString());
        assertEquals("0.90.0.Beta1", Version.fromString("0.90.0.Beta1").toString());
        assertEquals("1.0.0.Beta1", Version.fromString("1.0.0.Beta1").toString());
        assertEquals("2.0.0-beta1", Version.fromString("2.0.0-beta1").toString());
        assertEquals("5.0.0-beta1", Version.fromString("5.0.0-beta1").toString());
        assertEquals("5.0.0-alpha1", Version.fromString("5.0.0-alpha1").toString());
    }

    public void testIsBeta() {
        assertTrue(Version.fromString("2.0.0-beta1").isBeta());
        assertTrue(Version.fromString("1.0.0.Beta1").isBeta());
        assertTrue(Version.fromString("0.90.0.Beta1").isBeta());
    }


    public void testIsAlpha() {
        assertTrue(new LegacyESVersion(5000001, org.apache.lucene.util.Version.LUCENE_7_0_0).isAlpha());
        assertFalse(new LegacyESVersion(4000002, org.apache.lucene.util.Version.LUCENE_7_0_0).isAlpha());
        assertTrue(new LegacyESVersion(4000002, org.apache.lucene.util.Version.LUCENE_7_0_0).isBeta());
        assertTrue(Version.fromString("5.0.0-alpha14").isAlpha());
        assertEquals(5000014, Version.fromString("5.0.0-alpha14").id);
        assertTrue(Version.fromId(5000015).isAlpha());

        for (int i = 0 ; i < 25; i++) {
            assertEquals(Version.fromString("5.0.0-alpha" + i).id, Version.fromId(5000000 + i).id);
            assertEquals("5.0.0-alpha" + i, Version.fromId(5000000 + i).toString());
        }

        for (int i = 0 ; i < 25; i++) {
            assertEquals(Version.fromString("5.0.0-beta" + i).id, Version.fromId(5000000 + i + 25).id);
            assertEquals("5.0.0-beta" + i, Version.fromId(5000000 + i + 25).toString());
        }
    }

    public void testParseVersion() {
        final int iters = scaledRandomIntBetween(100, 1000);
        for (int i = 0; i < iters; i++) {
            Version version = randomVersion(random());
            if (random().nextBoolean()) {
                version = version.id < MASK ?
                    new LegacyESVersion(version.id, version.luceneVersion) :
                    new Version(version.id ^ MASK, version.luceneVersion);
            }
            Version parsedVersion = Version.fromString(version.toString());
            assertEquals(version, parsedVersion);
        }

        expectThrows(IllegalArgumentException.class, () -> {
            Version.fromString("5.0.0-alph2");
        });
        assertSame(Version.CURRENT, Version.fromString(Version.CURRENT.toString()));

        assertEquals(Version.fromString("2.0.0-SNAPSHOT"), Version.fromId(2000099));

        expectThrows(IllegalArgumentException.class, () -> {
            Version.fromString("5.0.0-SNAPSHOT");
        });
    }

    public void testParseLenient() {
        // note this is just a silly sanity check, we test it in lucene
        for (Version version : VersionUtils.allReleasedVersions()) {
            org.apache.lucene.util.Version luceneVersion = version.luceneVersion;
            String string = luceneVersion.toString().toUpperCase(Locale.ROOT)
                    .replaceFirst("^LUCENE_(\\d+)_(\\d+)$", "$1.$2");
            assertThat(luceneVersion, Matchers.equalTo(Lucene.parseVersionLenient(string, null)));
        }
    }

    public void testAllVersionsMatchId() throws Exception {
        final Set<Version> releasedVersions = new HashSet<>(VersionUtils.allReleasedVersions());
        final Set<Version> unreleasedVersions = new HashSet<>(VersionUtils.allUnreleasedVersions());
        Map<String, Version> maxBranchVersions = new HashMap<>();
        for (java.lang.reflect.Field field : Version.class.getFields()) {
            if (field.getName().matches("_ID")) {
                assertTrue(field.getName() + " should be static", Modifier.isStatic(field.getModifiers()));
                assertTrue(field.getName() + " should be final", Modifier.isFinal(field.getModifiers()));
                int versionId = (Integer)field.get(Version.class);

                String constantName = field.getName().substring(0, field.getName().indexOf("_ID"));
                java.lang.reflect.Field versionConstant = Version.class.getField(constantName);
                assertTrue(constantName + " should be static", Modifier.isStatic(versionConstant.getModifiers()));
                assertTrue(constantName + " should be final", Modifier.isFinal(versionConstant.getModifiers()));

                Version v = (Version) versionConstant.get(null);
                logger.debug("Checking {}", v);
                if (field.getName().endsWith("_UNRELEASED")) {
                    assertTrue(unreleasedVersions.contains(v));
                } else {
                    assertTrue(releasedVersions.contains(v));
                }
                assertEquals("Version id " + field.getName() + " does not point to " + constantName, v, Version.fromId(versionId));
                assertEquals("Version " + constantName + " does not have correct id", versionId, v.id);
                if (v.major >= 2) {
                    String number = v.toString();
                    if (v.isBeta()) {
                        number = number.replace("-beta", "_beta");
                    } else if (v.isRC()) {
                        number = number.replace("-rc", "_rc");
                    } else if (v.isAlpha()) {
                        number = number.replace("-alpha", "_alpha");
                    }
                    assertEquals("V_" + number.replace('.', '_'), constantName);
                } else {
                    assertEquals("V_" + v.toString().replace('.', '_'), constantName);
                }

                // only the latest version for a branch should be a snapshot (ie unreleased)
                String branchName = "" + v.major + "." + v.minor;
                Version maxBranchVersion = maxBranchVersions.get(branchName);
                if (maxBranchVersion == null) {
                    maxBranchVersions.put(branchName, v);
                } else if (v.after(maxBranchVersion)) {
                    if (v == Version.CURRENT) {
                        // Current is weird - it counts as released even though it shouldn't.
                        continue;
                    }
                    assertFalse("Version " + maxBranchVersion + " cannot be a snapshot because version " + v + " exists",
                            VersionUtils.allUnreleasedVersions().contains(maxBranchVersion));
                    maxBranchVersions.put(branchName, v);
                }
            }
        }
    }

    // this test ensures we never bump the lucene version in a bugfix release
    public void testLuceneVersionIsSameOnMinorRelease() {
        for (Version version : VersionUtils.allReleasedVersions()) {
            for (Version other : VersionUtils.allReleasedVersions()) {
                if (other.onOrAfter(version)) {
                    assertTrue("lucene versions must be "  + other + " >= " + version,
                        other.luceneVersion.onOrAfter(version.luceneVersion));
                }
                if (other.isAlpha() == false && version.isAlpha() == false
                        && other.major == version.major && other.minor == version.minor) {
                    assertEquals(version + " vs. " + other, other.luceneVersion.major, version.luceneVersion.major);
                    assertEquals(version + " vs. " + other, other.luceneVersion.minor, version.luceneVersion.minor);
                    // should we also assert the lucene bugfix version?
                }
            }
        }
    }

    public static void assertUnknownVersion(Version version) {
        assertFalse("Version " + version + " has been releaed don't use a new instance of this version",
            VersionUtils.allReleasedVersions().contains(version));
    }

    public void testIsCompatible() {
        assertTrue(isCompatible(Version.CURRENT, Version.CURRENT.minimumCompatibilityVersion()));
        assertFalse(isCompatible(LegacyESVersion.V_6_6_0, LegacyESVersion.V_7_0_0));
        assertFalse(isCompatible(LegacyESVersion.V_6_7_0, LegacyESVersion.V_7_0_0));
        assertTrue(isCompatible(LegacyESVersion.V_6_8_0, LegacyESVersion.V_7_0_0));
        assertFalse(isCompatible(Version.fromId(2000099), LegacyESVersion.V_7_0_0));
        assertFalse(isCompatible(Version.fromId(2000099), LegacyESVersion.V_6_5_0));

        int currentMajorID = Version.computeID(Version.CURRENT.major, 0, 0, 99);
        final Version currentMajorVersion = Version.fromId(currentMajorID);
        final Version currentOrNextMajorVersion;
        if (Version.CURRENT.minor > 0) {
            currentOrNextMajorVersion = Version.fromId(currentMajorID);
        } else {
            currentOrNextMajorVersion = currentMajorVersion;
        }
        final Version lastMinorFromPreviousMajor =
                VersionUtils
                        .allVersions()
                        .stream()
                        .filter(v -> v.major == (currentOrNextMajorVersion.major == 1 ? 7 : currentOrNextMajorVersion.major - 1))
                        .max(Version::compareTo)
                        .orElseThrow(
                                () -> new IllegalStateException("expected previous minor version for [" + currentOrNextMajorVersion + "]"));
        final Version previousMinorVersion = VersionUtils.getPreviousMinorVersion();

        boolean isCompatible =
                previousMinorVersion.major == currentOrNextMajorVersion.major
                        || previousMinorVersion.minor == lastMinorFromPreviousMajor.minor;

        final String message = String.format(
                Locale.ROOT,
                "[%s] should %s be compatible with [%s]",
                previousMinorVersion,
                isCompatible ? "" : " not",
                currentOrNextMajorVersion);
        assertThat(
                message,
                isCompatible(VersionUtils.getPreviousMinorVersion(), currentOrNextMajorVersion),
                equalTo(isCompatible));

        assertFalse(isCompatible(Version.fromId(5000099), Version.fromString("6.0.0")));
        assertFalse(isCompatible(Version.fromId(5000099), Version.fromString("7.0.0")));

        Version a = randomVersion(random());
        Version b = randomVersion(random());
        assertThat(a.isCompatible(b), equalTo(b.isCompatible(a)));
    }

    /* tests that if a new version's minCompatVersion is always equal or higher to any older version */
    public void testMinCompatVersionOrderRespectsVersionOrder() {
        List<Version> versionsByMinCompat = new ArrayList<>(allVersions());
        versionsByMinCompat.sort(Comparator.comparing(Version::minimumCompatibilityVersion));
        assertThat(versionsByMinCompat, equalTo(allVersions()));

        versionsByMinCompat.sort(Comparator.comparing(Version::minimumIndexCompatibilityVersion));
        assertThat(versionsByMinCompat, equalTo(allVersions()));
    }


    public boolean isCompatible(Version left, Version right) {
        boolean result = left.isCompatible(right);
        assert result == right.isCompatible(left);
        return result;
    }

    // This exists because 5.1.0 was never released due to a mistake in the release process.
    // This verifies that we never declare the version as "released" accidentally.
    // It would never pass qa tests later on, but those come very far in the build and this is quick to check now.
    public void testUnreleasedVersion() {
        Version VERSION_5_1_0_UNRELEASED = Version.fromString("5.1.0");
        VersionTests.assertUnknownVersion(VERSION_5_1_0_UNRELEASED);
    }

}
