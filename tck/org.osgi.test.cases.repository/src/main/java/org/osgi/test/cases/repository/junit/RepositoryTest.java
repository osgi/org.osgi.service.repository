/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
package org.osgi.test.cases.repository.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;

/**
 * @author David Bosschaert
 */
public class RepositoryTest extends RepositoryTestBase {
    @Test
    public void testQueryByBundleID() throws Exception {
        Requirement requirement = new RequirementImpl("osgi.wiring.bundle",
                "(&(osgi.wiring.bundle=org.osgi.test.cases.repository.tb1)(bundle-version=1.0.0.test))");

        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(requirement);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.keySet().iterator().next()).isEqualTo(requirement);

        assertThat(result.values().size()).isEqualTo(1);
        Collection<Capability> matchingCapabilities = result.values().iterator().next();
        assertThat(matchingCapabilities.size()).isEqualTo(1);
        Capability capability = matchingCapabilities.iterator().next();

        assertThat(capability.getNamespace()).isEqualTo(requirement.getNamespace());
        assertThat(capability.getAttributes().get("osgi.wiring.bundle")).isEqualTo("org.osgi.test.cases.repository.tb1");
        assertThat(capability.getAttributes().get("bundle-version")).isEqualTo(Version.parseVersion("1.0.0.test"));
    }

    @Test
    public void testQueryNoMatch() throws Exception {
        Requirement requirement = new RequirementImpl("osgi.wiring.bundle",
                "(&(osgi.wiring.bundle=org.osgi.test.cases.repository.tb1)(foo=bar))");

        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(requirement);
        assertThat(result).hasSize(1);
        assertThat(result.keySet().iterator().next()).isEqualTo(requirement);

        Collection<Capability> matchingCapabilities = result.get(requirement);
        assertThat(matchingCapabilities).isEmpty();
    }

    @Test
    public void testQueryNoFilter() throws Exception {
        Requirement requirement = new RequirementImpl("osgi.wiring.bundle");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(requirement);
        assertThat(result).hasSize(1);
        Collection<Capability> matches = result.get(requirement);
        assertThat(matches).hasSize(4);

        boolean foundtb1 = false, foundtb2 = false;
        for (Capability cap : matches) {
            if (cap.getAttributes().get("osgi.wiring.bundle").equals("org.osgi.test.cases.repository.tb1")) {
                foundtb1 = true;
            } else if (cap.getAttributes().get("osgi.wiring.bundle").equals("org.osgi.test.cases.repository.tb2")) {
                foundtb2 = true;
            }
        }

        assertThat(foundtb1).isTrue();
        assertThat(foundtb2).isTrue();
    }

    @Test
    public void testQueryOnNonMainAttribute() throws Exception {
        Requirement requirement = new RequirementImpl("osgi.identity",
                "(license=http://www.opensource.org/licenses/Apache-2.0)");

        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(requirement);
        assertThat(result.size()).isEqualTo(1);
        Collection<Capability> matches = result.get(requirement);
        assertThat(matches.size()).isEqualTo(1);
        Capability capability = matches.iterator().next();
        assertThat(capability.getNamespace()).isEqualTo("osgi.identity");
        assertThat(capability.getAttributes().get("osgi.identity")).isEqualTo("org.osgi.test.cases.repository.tb2");
    }

    @Test
    public void testDisconnectedQueries() throws Exception {
        Requirement req1 = new RequirementImpl("osgi.wiring.bundle",
                "(osgi.wiring.bundle=org.osgi.test.cases.repository.tb1)");
        Requirement req2 = new RequirementImpl("osgi.wiring.bundle",
                "(osgi.wiring.bundle=org.osgi.test.cases.repository.tb2)");

        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req1, req2);
        assertThat(result.size()).isEqualTo(2);

        Collection<Capability> match1 = result.get(req1);
        assertThat(match1.size()).isEqualTo(1);
        Capability cap1 = match1.iterator().next();
        assertThat(cap1.getAttributes().get("osgi.wiring.bundle")).isEqualTo("org.osgi.test.cases.repository.tb1");

        Collection<Capability> match2 = result.get(req2);
        assertThat(match2.size()).isEqualTo(1);
        Capability cap2 = match2.iterator().next();
        assertThat(cap2.getAttributes().get("osgi.wiring.bundle")).isEqualTo("org.osgi.test.cases.repository.tb2");
    }

    @Test
    public void testComplexQuery() throws Exception {
        Requirement req = new RequirementImpl("osgi.wiring.package",
                "(|(osgi.wiring.package=org.osgi.test.cases.repository.tb1.pkg1)(osgi.wiring.package=org.osgi.test.cases.repository.tb2))");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);

        assertThat(result).hasSize(1);
        Collection<Capability> matches = result.get(req);
        assertThat(matches).hasSize(2);

        boolean foundtb1 = false, foundtb2 = false;
        for (Capability cap : matches) {
            if (cap.getAttributes().get("bundle-symbolic-name").equals("org.osgi.test.cases.repository.tb1")) {
                foundtb1 = true;
            } else if (cap.getAttributes().get("bundle-symbolic-name").equals("org.osgi.test.cases.repository.tb2")) {
                foundtb2 = true;
            }
        }

        assertThat(foundtb1).isTrue();
        assertThat(foundtb2).isTrue();
    }

    @Test
    public void testComplexQueryWithCustomAttributeSpecificValue() throws Exception {
        Requirement req = new RequirementImpl("osgi.wiring.package",
                "(&(|(osgi.wiring.package=org.osgi.test.cases.repository.tb1.pkg1)(osgi.wiring.package=org.osgi.test.cases.repository.tb2))(approved=yes))");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);
        assertThat(result.size()).isEqualTo(1);
        Collection<Capability> matches = result.get(req);
        assertThat(matches.size()).isEqualTo(1);
        Capability capability = matches.iterator().next();
        assertThat(capability.getAttributes().get("bundle-symbolic-name")).isEqualTo("org.osgi.test.cases.repository.tb2");
    }

    @Test
    public void testComplexQueryWithCustomAttributeDefined() throws Exception {
        Requirement req = new RequirementImpl("osgi.wiring.package",
                "(&(|(osgi.wiring.package=org.osgi.test.cases.repository.tb1.pkg1)(osgi.wiring.package=org.osgi.test.cases.repository.tb2))(approved=*))");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);

        assertThat(result).hasSize(1);
        Collection<Capability> matches = result.get(req);
        assertThat(matches).hasSize(2);

        boolean foundtb1 = false, foundtb2 = false;
        for (Capability cap : matches) {
            if (cap.getAttributes().get("bundle-symbolic-name").equals("org.osgi.test.cases.repository.tb1")) {
                foundtb1 = true;
            } else if (cap.getAttributes().get("bundle-symbolic-name").equals("org.osgi.test.cases.repository.tb2")) {
                foundtb2 = true;
            }
        }

        assertThat(foundtb1).isTrue();
        assertThat(foundtb2).isTrue();
    }

    @Test
    public void testQueryCustomNamespace() throws Exception {
        Requirement req = new RequirementImpl("osgi.foo.bar", "(myattr=myval)");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);
        assertThat(result.size()).isEqualTo(1);
        Collection<Capability> matches = result.get(req);
        assertThat(matches.size()).isEqualTo(1);
        Capability capability = matches.iterator().next();
        assertThat(capability.getAttributes().get("myattr")).isEqualTo("myval");
    }

    @Test
    public void testRepositoryContent() throws Exception {
        Requirement req = new RequirementImpl("osgi.identity", "(osgi.identity=org.osgi.test.cases.repository.tb1)");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);
        assertThat(result.size()).isEqualTo(1);
        Collection<Capability> matches = result.get(req);
        assertThat(matches.size()).isEqualTo(1);

        Capability capability = matches.iterator().next();
        assertThat(capability.getNamespace()).isEqualTo("osgi.identity");
        assertThat(capability.getAttributes().get("osgi.identity")).isEqualTo("org.osgi.test.cases.repository.tb1");
        assertThat(capability.getAttributes().get("version")).isEqualTo(new Version("1.0.0.test"));
        assertThat(capability.getAttributes().get("type")).isEqualTo("osgi.bundle");
        assertThat(capability.getDirectives().size()).isEqualTo(0);

        Resource resource = capability.getResource();
        assertThat(resource.getRequirements(null).size()).as("This resource has no requirements").isEqualTo(0);

        List<Capability> bundleCaps = resource.getCapabilities("osgi.wiring.bundle");
        assertThat(bundleCaps.size()).isEqualTo(1);
        Capability bundleCap = bundleCaps.get(0);
        assertThat(bundleCap.getNamespace()).isEqualTo("osgi.wiring.bundle");
        assertThat(bundleCap.getAttributes().get("osgi.wiring.bundle")).isEqualTo("org.osgi.test.cases.repository.tb1");
        assertThat(bundleCap.getAttributes().get("bundle-version")).isEqualTo(new Version("1.0.0.test"));
        assertThat(bundleCap.getDirectives().size()).isEqualTo(0);

        boolean foundPkg1 = false;
        boolean foundPkg2 = false;
        List<Capability> packageCaps = resource.getCapabilities("osgi.wiring.package");
        assertThat(packageCaps.size()).isEqualTo(2);
        for (Capability packageCap : packageCaps) {
            assertThat(packageCap.getNamespace()).isEqualTo("osgi.wiring.package");
            assertThat(packageCap.getDirectives().size()).isEqualTo(0);
            Map<String, Object> attrs = packageCap.getAttributes();
            assertThat(attrs.get("bundle-symbolic-name")).isEqualTo("org.osgi.test.cases.repository.tb1");
            assertThat(attrs.get("bundle-version")).isEqualTo(new Version("1.0.0.test"));
            assertThat(attrs.get("approved")).isEqualTo("no");

            if ("org.osgi.test.cases.repository.tb1.pkg1".equals(attrs.get("osgi.wiring.package"))) {
                assertThat(attrs.get("version")).isEqualTo(new Version("0.9"));
                foundPkg1 = true;
            } else if ("org.osgi.test.cases.repository.tb1.pkg2".equals(attrs.get("osgi.wiring.package"))) {
                assertThat(attrs.get("version")).isEqualTo(new Version("0.8"));
                foundPkg2 = true;
            }
        }
        assertThat(foundPkg1).isTrue();
        assertThat(foundPkg2).isTrue();

        List<Capability> fooBarCaps = resource.getCapabilities("osgi.foo.bar");
        assertThat(fooBarCaps.size()).isEqualTo(1);
        Capability fooBarCap = fooBarCaps.iterator().next();
        assertThat(fooBarCap.getNamespace()).isEqualTo("osgi.foo.bar");
        assertThat(fooBarCap.getDirectives().size()).isEqualTo(0);
        assertThat(fooBarCap.getAttributes().get("myattr")).isEqualTo("myval");

        // test osgi.content
        List<Capability> contentCaps = resource.getCapabilities("osgi.content");
        assertThat(contentCaps.size()).isEqualTo(1);
        Capability contentCap = contentCaps.iterator().next();
        assertThat(contentCap.getNamespace()).isEqualTo("osgi.content");
        assertThat(contentCap.getDirectives().size()).isEqualTo(0);
        assertThat(contentCap.getAttributes().get("mime")).isEqualTo("application/vnd.osgi.bundle");
        String url = (String) contentCap.getAttributes().get("url");
        byte[] contentBytes = readFully(new URL(url).openStream());
        assertThat(contentBytes.length > 0).isTrue();
        assertThat(contentCap.getAttributes().get("size")).isEqualTo(Long.valueOf(contentBytes.length));
        assertThat(contentCap.getAttributes().get("osgi.content")).isEqualTo(getSHA256(contentBytes));
    }

    @Test
    public void testRepositoryContent2() throws Exception {
        Requirement req = new RequirementImpl("osgi.identity", "(osgi.identity=org.osgi.test.cases.repository.tb2)");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);
        assertThat(result.size()).isEqualTo(1);
        Collection<Capability> matches = result.get(req);
        assertThat(matches.size()).isEqualTo(1);
        Capability capability = matches.iterator().next();
        assertThat(capability.getAttributes().get("osgi.identity")).isEqualTo("org.osgi.test.cases.repository.tb2");

        Resource resource = capability.getResource();

        // test getCapabilities();
        List<Capability> identityCaps = resource.getCapabilities("osgi.identity");
        assertThat(identityCaps.size()).isEqualTo(1);
        Capability identityCap = identityCaps.iterator().next();
        assertThat(identityCap.getAttributes().get("osgi.identity")).isEqualTo("org.osgi.test.cases.repository.tb2");
        assertThat(identityCap.getAttributes().get("version")).isEqualTo(Version.parseVersion("1"));
        assertThat(identityCap.getAttributes().get("type")).isEqualTo("osgi.bundle");
        assertThat(identityCap.getAttributes().get("license")).isEqualTo("http://www.opensource.org/licenses/Apache-2.0");

        List<Capability> contentCaps = resource.getCapabilities("osgi.content");
        assertThat(contentCaps.size()).isEqualTo(1);
        Capability contentCap = contentCaps.iterator().next();
        // content and SHA is checked below

        assertThat(resource.getCapabilities("osgi.wiring.bundle").size()).isEqualTo(1);

        List<Capability> wiringCaps = resource.getCapabilities("osgi.wiring.package");
        assertThat(wiringCaps.size()).isEqualTo(1);
        Capability wiringCap = wiringCaps.iterator().next();
        assertThat(wiringCap.getAttributes().get("osgi.wiring.package")).isEqualTo("org.osgi.test.cases.repository.tb2");
        assertThat(wiringCap.getAttributes().get("version")).isEqualTo(Version.parseVersion("1.2.3.qualified"));
        assertThat(wiringCap.getAttributes().get("bundle-version")).isEqualTo(Version.parseVersion("1"));
        assertThat(wiringCap.getAttributes().get("bundle-symbolic-name")).isEqualTo("org.osgi.test.cases.repository.tb2");
        assertThat(wiringCap.getAttributes().get("approved")).isEqualTo("yes");
        assertThat(wiringCap.getDirectives().get("uses")).isEqualTo("org.osgi.test.cases.repository.tb1.pkg1");
        assertThat(resource.getCapabilities("osgi.foo.bar").size()).isEqualTo(1);

        // Read the requirements
        assertThat(resource.getRequirements("org.osgi.nonexistent").size()).isEqualTo(0);
        List<Requirement> wiringReqs = resource.getRequirements("osgi.wiring.package");
        assertThat(wiringReqs.size()).isEqualTo(1);
        Requirement wiringReq = wiringReqs.iterator().next();
        assertThat(wiringReq.getDirectives().size()).isEqualTo(2);
        assertThat(wiringReq.getDirectives().get("custom")).isEqualTo("custom directive");
        assertThat(wiringReq.getDirectives().get("filter")).isEqualTo("(&(osgi.wiring.package=org.osgi.test.cases.repository.tb1.pkg1)(version>=0.9)(!(version>=1)))");
        assertThat(wiringReq.getAttributes().size()).isEqualTo(1);
        assertThat(wiringReq.getAttributes().get("custom")).isEqualTo(Long.valueOf(42));

        List<Requirement> metaReqs = resource.getRequirements("osgi.identity");
        assertThat(metaReqs.size()).isEqualTo(2);

        boolean foundSources = false;
        boolean foundJavaDoc = false;
        for (Requirement metaReq : metaReqs) {
            assertThat(metaReq.getNamespace()).isEqualTo("osgi.identity");
            assertThat(metaReq.getAttributes().size()).isEqualTo(0);
            Map<String, String> dirs = metaReq.getDirectives();
            assertThat(dirs.get("effective")).isEqualTo("meta");
            assertThat(dirs.get("resolution")).isEqualTo("optional");
            if ("sources".equals(dirs.get("classifier"))) {
                assertThat(dirs.get("filter")).isEqualTo("(&(version=1.0)(osgi.identity=org.osgi.test.cases.repository.tb2-src))");
                foundSources = true;
            } else if ("javadoc".equals(dirs.get("classifier"))) {
                assertThat(dirs.get("filter")).isEqualTo("(&(version=1.0)(osgi.identity=org.osgi.test.cases.repository.tb2-javadoc))");
                foundJavaDoc = true;
            }
        }

        assertThat(foundSources).isTrue();
        assertThat(foundJavaDoc).isTrue();

        Set<Requirement> allReqs = new HashSet<Requirement>();
        allReqs.addAll(wiringReqs);
        allReqs.addAll(metaReqs);
        assertThat(new HashSet<Requirement>(resource.getRequirements(null))).isEqualTo(allReqs);

        // Check content and SHA
        URL tb1JarURL = context.getBundle().getResource("tb2.jar");
        byte[] expectedBytes = readFully(tb1JarURL.openStream());

        RepositoryContent repositoryContent = (RepositoryContent) resource;
        byte[] contentBytes = readFully(repositoryContent.getContent());
        assertThat(contentBytes.length > 0).isTrue();
        assertThat(Arrays.equals(expectedBytes, contentBytes)).isTrue();
        assertThat(contentCap.getAttributes().get("size")).isEqualTo(Long.valueOf(contentBytes.length));
        assertThat(contentCap.getAttributes().get("osgi.content")).isEqualTo(getSHA256(contentBytes));
    }

	@Test
	public void testAttributeDataTypes() throws Exception {
        Requirement req = new RequirementImpl("osgi.test.namespace", "(osgi.test.namespace=a testing namespace)");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(req);
        assertThat(result.size()).isEqualTo(1);
        Collection<Capability> matches = result.get(req);
        assertThat(matches.size()).isEqualTo(1);
        Capability cap = matches.iterator().next();

        assertThat(cap.getNamespace()).isEqualTo(req.getNamespace());
        assertThat(cap.getAttributes().get("osgi.test.namespace")).isEqualTo("a testing namespace");
        assertThat(cap.getAttributes().get("testString")).isEqualTo("");
        assertThat(cap.getAttributes().get("testString2")).isEqualTo(",");
        assertThat(cap.getAttributes().get("testString3")).isEqualTo("a,b");
        assertThat(cap.getAttributes().get("testVersion")).isEqualTo(Version.parseVersion("1.2.3.qualifier"));
        assertThat(cap.getAttributes().get("testLong")).isEqualTo(Long.valueOf(Long.MAX_VALUE));
        assertThat(cap.getAttributes().get("testDouble")).isEqualTo(Double.valueOf(Math.PI));
        assertThat(cap.getAttributes().get("testStringList")).isEqualTo(Arrays.asList("a", "b and c", "d"));
        assertThat(cap.getAttributes().get("testStringList2")).isEqualTo(Arrays.asList(",", "\\,\\"));
        assertThat(cap.getAttributes().get("testStringList3")).isEqualTo(Arrays.asList(","));
        assertThat(cap.getAttributes().get("testVersionList")).isEqualTo(Arrays.asList(new Version("1.2.3"), new Version("4.5.6")));
        assertThat(cap.getAttributes().get("testLongList")).isEqualTo(Arrays.asList(Long.MIN_VALUE, 0l, Long.MAX_VALUE));
        assertThat(cap.getAttributes().get("testDoubleList")).isEqualTo(Arrays.asList(Math.E, Math.E));
    }

    @Test
    public void testQueryExpressions() throws Exception {
        Requirement emptyReq = new RequirementImpl("osgi.test.namespace");
        Map<Requirement, Collection<Capability>> result = findProvidersAllRepos(emptyReq);
        assertThat(result).hasSize(1);
        Collection<Capability> match = result.get(emptyReq);
        assertThat(match).hasSize(2);

        Capability expected = null;
        for (Capability cap : match) {
            if ("a testing namespace".equals(cap.getAttributes().get("osgi.test.namespace"))) {
                expected = cap;
                break;
            }
        }

        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testString=*)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testString=foo)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testString2=,)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testString2=a,b)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testString3=a,b)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testString3=,)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testVersion=1.2.3.qualifier)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testVersion=2.0)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testVersion>=1.0)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testVersion>=2.0)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testVersion<=1.2.3.qualifier)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(|(testVersion<=1.0)(testVersion>=2.0))")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(&(testVersion>=1.0)(!(testVersion>=2.0)))")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(&(testVersion>=1.0)(!(testVersion>=1.1)))")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList=d)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList=e)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList=b*c)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList=b*d)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList~=D)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList~=E)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList2=,)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList2=\\\\,\\\\)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList2=\\\\)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList3=,)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testStringList3=\\\\)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testLong=" + Long.MAX_VALUE + ")")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testLong=" + Long.MIN_VALUE + ")")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testDouble>=3)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testDouble>=3.2)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testVersionList=*)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testVersionList=1.2.3)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testVersionList=4.5.6)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testVersionList=1.0)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testLongList<=0)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testLongList>=0)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testLongList=0)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testLongList=1)")).isNull();
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testDoubleList=2.718281828459045)")).isEqualTo(expected);
        assertThat(findSingleCapSingleReq("osgi.test.namespace", "(testDoubleList=0)")).isNull();
    }

    @Test
    public void testMultiContent() throws Exception {
        Resource res = findSingleCapSingleReq("osgi.identity", "(osgi.identity=org.osgi.test.cases.repository.testresource)").getResource();
        List<Capability> caps = res.getCapabilities("osgi.content");
        assertThat(caps).hasSize(2);

        boolean foundZip = false;
        boolean foundTgz = false;
        for (Capability cap : caps) {
            Map<String, Object> attrs = cap.getAttributes();

            URL url = null;
            if ("application/vnd.osgi.test.zip".equals(attrs.get("mime"))) {
                url = context.getBundle().getResource("/testresource.zip");
                foundZip = true;
            } else if ("application/vnd.osgi.test.tar.gz".equals(attrs.get("mime"))) {
                url = context.getBundle().getResource("/testresource.tar.gz");
                foundTgz = true;
            }

            byte[] expectedBytes = readFully(url.openStream());

            byte[] actualBytes = readFully(new URL((String) attrs.get("url")).openStream());
            assertThat(Arrays.equals(expectedBytes, actualBytes)).isTrue();
            assertThat(attrs.get("size")).isEqualTo(Long.valueOf(expectedBytes.length));
            assertThat(attrs.get("osgi.content")).isEqualTo(getSHA256(expectedBytes));
        }
        assertThat(foundZip).isTrue();
        assertThat(foundTgz).isTrue();
    }

    @Test
    public void testSHA256ComputationSanityCheck() throws Exception {
        // This test validates the SHA256 computation in this test class with the example SHA256 serializations
        // as listed here: http://en.wikipedia.org/wiki/SHA-2
        // SHA256("")
        // e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855

        // SHA256("The quick brown fox jumps over the lazy dog")
        // d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592

        // SHA256("The quick brown fox jumps over the lazy dog.")
        // ef537f25c895bfa782526529a9b63d97aa631564d5d789c2b765448c8635fb6c

        assertThat(getSHA256("".getBytes())).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(getSHA256("The quick brown fox jumps over the lazy dog".getBytes())).isEqualTo("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592");
        assertThat(getSHA256("The quick brown fox jumps over the lazy dog.".getBytes())).isEqualTo("ef537f25c895bfa782526529a9b63d97aa631564d5d789c2b765448c8635fb6c");
    }

    private Capability findSingleCapSingleReq(String ns, String filter) {
        Requirement req = new RequirementImpl(ns, filter);
        Map<Requirement, Collection<Capability>> res = findProvidersAllRepos(req);
        assertThat(res).hasSize(1);
        Collection<Capability> caps = res.get(req);
        if (caps.size() == 0)
            return null;

        assertThat(caps).hasSize(1);
        return caps.iterator().next();
    }

    private Map<Requirement, Collection<Capability>> findProvidersAllRepos(Requirement ... requirement) {
        Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
        for (Repository repository : repositoryServices) {
            Map<Requirement, Collection<Capability>> r =
                     repository.findProviders(Arrays.asList(requirement));

            for (Map.Entry<Requirement, Collection<Capability>> entry : r.entrySet()) {
                Collection<Capability> caps = result.get(entry.getKey());
                if (caps == null) {
                    result.put(entry.getKey(), entry.getValue());
                } else {
                    caps.addAll(entry.getValue());
                }
            }
        }
        return result;
    }
}
