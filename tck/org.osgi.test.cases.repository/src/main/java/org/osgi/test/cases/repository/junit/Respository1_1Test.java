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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.service.repository.RequirementExpression;

/**
 * @author David Bosschaert
 */
public class Respository1_1Test extends RepositoryTestBase {
    @Test
    public void testRequirementBuilderNamespace() throws Exception {
        RequirementBuilder builder = getRepo().newRequirementBuilder("something");
        Requirement req = builder.build();
        assertThat(req.getNamespace()).isEqualTo("something");
        assertThat(req.getResource()).isNull();
        assertThat(req.getAttributes().size()).isEqualTo(0);
        assertThat(req.getDirectives().size()).isEqualTo(0);
    }

    @Test
    public void testRequirementBuilderAttrsDirs() throws Exception {
        RequirementBuilder b1 = getRepo().newRequirementBuilder("org.acme.test");
        b1.addAttribute("a", "a1");
        b1.addAttribute("b", "b1");
        b1.addDirective("d", "d1");
        b1.addDirective("e", "e1");
        b1.addDirective("filter", "(a=b)");
        Requirement req1 = b1.build();

        assertThat(req1.getAttributes().size()).isEqualTo(2);
        assertThat(req1.getAttributes().get("a")).isEqualTo("a1");
        assertThat(req1.getAttributes().get("b")).isEqualTo("b1");
        assertThat(req1.getDirectives().size()).isEqualTo(3);
        assertThat(req1.getDirectives().get("d")).isEqualTo("d1");
        assertThat(req1.getDirectives().get("e")).isEqualTo("e1");
        assertThat(req1.getDirectives().get("filter")).isEqualTo("(a=b)");

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("a", "a1");
        attrs.put("b", "b1");
        Map<String, String> dirs = new HashMap<String, String>();
        dirs.put("d", "d1");
        dirs.put("e", "e1");
        dirs.put("filter", "(a=b)");
        RequirementBuilder b2 = getRepo().newRequirementBuilder("org.acme.test");
        b2.addAttribute("a", "xxx"); // will get replaced
        b2.addDirective("f", "yyy"); // will get replaced
        b2.setAttributes(attrs);
        b2.setDirectives(dirs);
        Requirement req2 = b2.build();

        assertThat(req2.getAttributes().size()).isEqualTo(2);
        assertThat(req2.getAttributes().get("a")).isEqualTo("a1");
        assertThat(req2.getAttributes().get("b")).isEqualTo("b1");
        assertThat(req2.getDirectives().size()).isEqualTo(3);
        assertThat(req2.getDirectives().get("d")).isEqualTo("d1");
        assertThat(req2.getDirectives().get("e")).isEqualTo("e1");
        assertThat(req2.getDirectives().get("filter")).isEqualTo("(a=b)");

        assertThat(req2).isEqualTo(req1);

        Requirement otherReq = getRepo().newRequirementBuilder("org.acme.test").build();
        assertThat(req1).isNotEqualTo(otherReq);
    }

    @Test
    public void testRequirementBuilderResource() throws Exception {
        RequirementBuilder builder = getRepo().newRequirementBuilder("osgi.test.namespace");
        Resource tr = new TestResource();
        builder.setResource(tr);
        Requirement req = builder.build();
        assertThat(req.getResource()).isSameAs(tr);
    }

    @Test
    public void testSimpleRequirementExpression() throws Exception {
        RequirementBuilder builder = getRepo().newRequirementBuilder("osgi.wiring.bundle");
        builder.addDirective("filter", "(&(osgi.wiring.bundle=org.osgi.test.cases.repository.tb1)(bundle-version=1.0.0.test))");

        RequirementExpression expr = builder.buildExpression();
        Set<Resource> result = findProvidersAllRepos(expr);
        assertThat(result.size()).isEqualTo(1);
        Resource res = result.iterator().next();

        List<Capability> identityCaps = res.getCapabilities("osgi.identity");
        assertThat(identityCaps.size()).isEqualTo(1);
        assertThat(identityCaps.get(0).getAttributes().get("osgi.identity")).isEqualTo("org.osgi.test.cases.repository.tb1");
    }

    @Test
    public void testAndExpression() throws Exception {
        RequirementBuilder builder1 = getRepo().newRequirementBuilder("osgi.wiring.package");
        builder1.addDirective("filter", "(&(osgi.wiring.package=org.osgi.test.cases.repository.tbX)(version>=7)(!(version>=8)))");
        RequirementExpression wiringReq = builder1.buildExpression();

        RequirementBuilder builder2 = getRepo().newRequirementBuilder("osgi.content");
        builder2.addDirective("filter", "(mime=application/vnd.osgi.bundle)");
        RequirementExpression contentReq = builder2.buildExpression();

        ExpressionCombiner ec = getRepo().getExpressionCombiner();
        RequirementExpression wiringAndContent = ec.and(wiringReq, contentReq);
        Set<Resource> result1 = findProvidersAllRepos(wiringAndContent);
        assertThat(result1).hasSize(2);

        boolean foundTB3 = false;
        boolean foundTB4 = false;
        for (Resource r : result1) {
            Object id = r.getCapabilities("osgi.identity").iterator().next().getAttributes().get("osgi.identity");
            if ("org.osgi.test.cases.repository.tb3".equals(id)) {
                foundTB3 = true;
            }
            if ("org.osgi.test.cases.repository.tb4".equals(id)) {
                foundTB4 = true;
            }
        }
        assertThat(foundTB3).isTrue();
        assertThat(foundTB4).isTrue();

        RequirementBuilder builder3 = getRepo().newRequirementBuilder("osgi.identity");
        builder3.addDirective("filter", "(license=http://opensource.org/licenses/EPL*)");
        RequirementExpression licenseReq = builder3.buildExpression();
        RequirementExpression andLicense = ec.and(wiringAndContent, licenseReq);
        Set<Resource> result2 = findProvidersAllRepos(andLicense);
        assertThat(result2).hasSize(1);
        Resource resource = result2.iterator().next();
        Object id = resource.getCapabilities("osgi.identity").iterator().next().getAttributes().get("osgi.identity");
        assertThat(id).isEqualTo("org.osgi.test.cases.repository.tb4");
    }

    @Test
    public void testMultiAndExpression() throws Exception {
        RequirementBuilder b1 = getRepo().newRequirementBuilder("osgi.content");
        b1.addDirective("filter", "(mime=application/vnd.osgi.bundle)");
        IdentityExpression re1 = b1.buildExpression();
        RequirementBuilder b2 = getRepo().newRequirementBuilder("osgi.wiring.package");
        b2.addDirective("filter", "(osgi.wiring.package=org.osgi.test.cases.repository.tbX)");
        IdentityExpression re2 = b2.buildExpression();
        RequirementBuilder b3 = getRepo().newRequirementBuilder("osgi.identity");
        b3.addDirective("filter", "(license=http://opensource.org/licenses/Apache-2.0)");
        IdentityExpression re3 = b3.buildExpression();

        Set<Resource> result = findProvidersAllRepos(getRepo().getExpressionCombiner().and(re1, re2, re3));
        assertThat(result.size()).isEqualTo(1);
        Resource res = result.iterator().next();
        
        List<Capability> identityCaps = res.getCapabilities("osgi.identity");
        assertThat(identityCaps.size()).isEqualTo(1);
        assertThat(identityCaps.get(0).getAttributes().get("osgi.identity")).isEqualTo("org.osgi.test.cases.repository.tb3");
    }

    @Test
    public void testNotExpression() throws Exception {
        ExpressionCombiner ec = getRepo().getExpressionCombiner();
        RequirementBuilder builder = getRepo().newRequirementBuilder("osgi.foo.bar");
        builder.addDirective("filter", "(myattr=myotherval)");
        RequirementExpression req = builder.buildExpression();
        Set<Resource> result1 = findProvidersAllRepos(req);
        assertThat(result1.size()).isEqualTo(1);
        Resource res1 = result1.iterator().next();
        assertThat(res1.getCapabilities("osgi.identity").iterator().next()
                .getAttributes().get("osgi.identity")).isEqualTo("org.osgi.test.cases.repository.tb2");

        Set<Resource> result2 = findProvidersAllRepos(ec.not(req));
        assertThat(result2.size() > 0).isTrue();
        for (Resource r : result2) {
            Object id = r.getCapabilities("osgi.identity").iterator().next().getAttributes().get("osgi.identity");
            assertThat(!"org.osgi.test.cases.repository.tb2".equals(id)).isTrue();
        }
    }

    @Test
    public void testOrExpression() throws Exception {
        RequirementBuilder builder1 = getRepo().newRequirementBuilder("osgi.test.namespace");
        builder1.addDirective("filter", "(osgi.test.namespace=*)");
        RequirementExpression req1 = builder1.buildExpression();

        RequirementBuilder builder2 = getRepo().newRequirementBuilder("osgi.identity");
        builder2.addDirective("filter", "(type=someresource)");
        RequirementExpression req2 = builder2.buildExpression();

        Set<Resource> result = findProvidersAllRepos(getRepo().getExpressionCombiner().or(req1, req2));
        assertThat(result).hasSize(2);

        boolean foundTB1 = false;
        boolean foundRes = false;
        for (Resource r : result) {
            Object id = r.getCapabilities("osgi.identity").iterator().next().getAttributes().get("osgi.identity");
            if ("org.osgi.test.cases.repository.tb1".equals(id)) {
                foundTB1 = true;
            }
            if ("org.osgi.test.cases.repository.testresource".equals(id)) {
                foundRes = true;
            }
        }
        assertThat(foundTB1).isTrue();
        assertThat(foundRes).isTrue();
    }

    @Test
    public void testMultiOrExpression() throws Exception {
        RequirementBuilder b1 = getRepo().newRequirementBuilder("osgi.identity");
        b1.addDirective("filter", "(osgi.identity=org.osgi.test.cases.repository.tb1)");
        IdentityExpression re1 = b1.buildExpression();
        RequirementBuilder b2 = getRepo().newRequirementBuilder("osgi.foo.bar");
        b2.addDirective("filter", "(myattr=myotherval)");
        IdentityExpression re2 = b2.buildExpression();
        RequirementBuilder b3 = getRepo().newRequirementBuilder("osgi.wiring.bundle");
        b3.addDirective("filter", "(osgi.wiring.bundle=org.osgi.test.cases.repository.tb4)");
        IdentityExpression re3 = b3.buildExpression();

        Set<Resource> result = findProvidersAllRepos(getRepo().getExpressionCombiner().or(re1, re2, re3));
        assertThat(result).hasSize(3);

        boolean foundTB1 = false;
        boolean foundTB2 = false;
        boolean foundTB4 = false;
        for (Resource r : result) {
            Object id = r.getCapabilities("osgi.identity").iterator().next().getAttributes().get("osgi.identity");
            if ("org.osgi.test.cases.repository.tb1".equals(id)) {
                foundTB1 = true;
            }
            if ("org.osgi.test.cases.repository.tb2".equals(id)) {
                foundTB2 = true;
            }
            if ("org.osgi.test.cases.repository.tb4".equals(id)) {
                foundTB4 = true;
            }
        }
        assertThat(foundTB1).isTrue();
        assertThat(foundTB2).isTrue();
        assertThat(foundTB4).isTrue();
    }

    @Test
    public void testCombinedExpression() throws Exception {
        RequirementBuilder builder1 = getRepo().newRequirementBuilder("osgi.wiring.package");
        builder1.addDirective("filter", "(osgi.wiring.package=org.osgi.test.cases.repository.tbX)");
        RequirementExpression req1 = builder1.buildExpression();
        RequirementBuilder builder2 = getRepo().newRequirementBuilder("osgi.wiring.bundle");
        builder2.addDirective("filter", "(osgi.wiring.bundle=org.osgi.test.cases.repository.tb4)");
        RequirementExpression req2 = builder2.buildExpression();

        ExpressionCombiner ec = getRepo().getExpressionCombiner();
        RequirementExpression andReq = ec.and(req1, req2);
        RequirementExpression notReq = ec.not(andReq);
        RequirementExpression andNotReq = ec.and(req1, notReq);

        RequirementExpression req3 = getRepo().newRequirementBuilder("osgi.test.namespace").buildExpression();
        RequirementExpression complexReq = ec.or(req3, andNotReq);

        Set<Resource> result = findProvidersAllRepos(complexReq);
        assertThat(result).hasSize(2);

        boolean foundTB1 = false;
        boolean foundTB3 = false;
        for (Resource r : result) {
            Object id = r.getCapabilities("osgi.identity").iterator().next().getAttributes().get("osgi.identity");
            if ("org.osgi.test.cases.repository.tb1".equals(id)) {
                foundTB1 = true;
            }
            if ("org.osgi.test.cases.repository.tb3".equals(id)) {
                foundTB3 = true;
            }
        }
        assertThat(foundTB1).isTrue();
        assertThat(foundTB3).isTrue();
    }

    private Set<Resource> findProvidersAllRepos(RequirementExpression expr) throws Exception {
        Set<Resource> resources = new HashSet<Resource>();
        for (Repository repository : repositoryServices) {
            resources.addAll(repository.findProviders(expr).getValue());
        }
        return resources;
    }

    private Repository getRepo() {
        return repositoryServices.get(0);
    }

    public static final class TestResource implements Resource {
        public List<Capability> getCapabilities(String namespace) {
            return null;
        }

        public List<Requirement> getRequirements(String namespace) {
            return null;
        }
    }
}
