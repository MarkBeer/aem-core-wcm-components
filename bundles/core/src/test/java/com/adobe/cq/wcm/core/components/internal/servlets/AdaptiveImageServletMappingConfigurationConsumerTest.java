/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2018 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.internal.servlets;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Hashtable;
import javax.servlet.Servlet;

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.adobe.cq.wcm.core.components.context.CoreComponentTestContext;
import com.adobe.cq.wcm.core.components.testing.Utils;
import com.day.cq.dam.api.handler.store.AssetStore;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdaptiveImageServletMappingConfigurationConsumerTest {

    @Rule
    public final AemContext slingContext = CoreComponentTestContext.createContext();

    @Before
    public void setUp() {
        AssetStore assetStore = mock(AssetStore.class);
        slingContext.registerService(AssetStore.class, assetStore);
    }

    @Test
    public void testConfigurationConsumer() throws Exception {
        AdaptiveImageServletMappingConfigurationConsumer configurationConsumer = new AdaptiveImageServletMappingConfigurationConsumer();
        slingContext.registerInjectActivateService(configurationConsumer);
        AdaptiveImageServletMappingConfigurationFactory config1 = new AdaptiveImageServletMappingConfigurationFactory();
        config1.configure(new AdaptiveImageServletMappingConfigurationFactory.Config() {
            @Override
            public String[] resource_types() {
                return new String[]{"a/b/c"};
            }

            @Override
            public String[] selectors() {
                return new String[]{"a"};
            }

            @Override
            public String[] extensions() {
                return new String[]{"jpeg"};
            }

            @Override
            public int defaultResizeWidth() {
                return AdaptiveImageServlet.DEFAULT_RESIZE_WIDTH;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }
        });

        AdaptiveImageServletMappingConfigurationFactory config2 = new AdaptiveImageServletMappingConfigurationFactory();
        config2.configure(new AdaptiveImageServletMappingConfigurationFactory.Config() {
            @Override
            public String[] resource_types() {
                return new String[] {"d/e/f"};
            }

            @Override
            public String[] selectors() {
                return new String[]{"a"};
            }

            @Override
            public String[] extensions() {
                return new String[]{"jpeg"};
            }

            @Override
            public int defaultResizeWidth() {
                return 1280;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }
        });

        ServiceRegistration<AdaptiveImageServletMappingConfigurationFactory> registration1 = slingContext.bundleContext().registerService
                (AdaptiveImageServletMappingConfigurationFactory.class, config1, new Hashtable() {{
                        put(Constants.SERVICE_PID, "pid1");
                    }});
        ServiceRegistration<AdaptiveImageServletMappingConfigurationFactory> registration2 = slingContext.bundleContext().registerService
                (AdaptiveImageServletMappingConfigurationFactory.class, config2, new Hashtable() {{
                    put(Constants.SERVICE_PID, "pid2");
                }});
        Collection<ServiceReference<Servlet>> servletServiceReferences = slingContext.bundleContext().getServiceReferences(Servlet.class,
                "(sling.servlet.resourceTypes=a/b/c)");
        assertEquals(1, servletServiceReferences.size());
        ServiceReference<Servlet> servletReference = servletServiceReferences.iterator().next();
        Servlet ais = slingContext.bundleContext().getService(servletReference);
        assertTrue(ais instanceof AdaptiveImageServlet);
    }

    @Test
    public void testConfigurationConsumerWithPreviousAISConfig() throws Exception {
        Configuration aisConfiguration = mock(Configuration.class);
        when(aisConfiguration.getProperties()).thenReturn(new Hashtable<String, Object>(){{
            put("defaultResizeWidth", 1000);
        }});
        ConfigurationAdmin configurationAdmin = mock(ConfigurationAdmin.class);
        when(configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + AdaptiveImageServlet.class.getName() + ")"))
                .thenReturn(new Configuration[]{aisConfiguration});


        AdaptiveImageServletMappingConfigurationConsumer configurationConsumer = new AdaptiveImageServletMappingConfigurationConsumer();

        // override the default provided mock object, since it doesn't support #listConfigurations
        Utils.setInternalState(configurationConsumer, "configurationAdmin", configurationAdmin);
        slingContext.registerService(configurationConsumer);
        MockOsgi.activate(configurationConsumer, slingContext.bundleContext());

        AdaptiveImageServletMappingConfigurationFactory config1 = new AdaptiveImageServletMappingConfigurationFactory();
        config1.configure(new AdaptiveImageServletMappingConfigurationFactory.Config() {
            @Override
            public String[] resource_types() {
                return new String[]{"a/b/c"};
            }

            @Override
            public String[] selectors() {
                return new String[]{"a"};
            }

            @Override
            public String[] extensions() {
                return new String[]{"jpeg"};
            }

            @Override
            public int defaultResizeWidth() {
                return AdaptiveImageServlet.DEFAULT_RESIZE_WIDTH;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }
        });


        slingContext.bundleContext().registerService(AdaptiveImageServletMappingConfigurationFactory.class, config1, new Hashtable<>());

        Collection<ServiceReference<Servlet>> servletServiceReferences = slingContext.bundleContext().getServiceReferences(Servlet.class,
                "(sling.servlet.resourceTypes=a/b/c)");
        assertEquals(1, servletServiceReferences.size());
        ServiceReference<Servlet> servletReference = servletServiceReferences.iterator().next();
        Servlet ais = slingContext.bundleContext().getService(servletReference);
        assertTrue(ais instanceof AdaptiveImageServlet);
    }
}
