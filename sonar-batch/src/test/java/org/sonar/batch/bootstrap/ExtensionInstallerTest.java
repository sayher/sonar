/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Plugin;
import org.sonar.api.ServerExtension;
import org.sonar.api.SonarPlugin;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrapper.EnvironmentInformation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExtensionInstallerTest {

  private static final PluginMetadata METADATA = mock(PluginMetadata.class);

  private static Map<PluginMetadata, Plugin> newPlugin(final Class... classes) {
    Map<PluginMetadata, Plugin> result = Maps.newHashMap();
    result.put(METADATA,
      new SonarPlugin() {
        public List<Class> getExtensions() {
          return Arrays.asList(classes);
        }
      }
    );
    return result;
  }

  @Test
  public void shouldInstallExtensionsWithBatchInstantiationStrategy() {
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPluginsByMetadata()).thenReturn(newPlugin(BatchService.class, ProjectService.class, ServerService.class));
    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"), new Settings());

    installer.install(container, InstantiationStrategy.PER_BATCH);

    assertThat(container.getComponentByType(BatchService.class)).isNotNull();
    assertThat(container.getComponentByType(ProjectService.class)).isNull();
    assertThat(container.getComponentByType(ServerService.class)).isNull();
  }

  @Test
  public void shouldInstallProvidersWithBatchInstantiationStrategy() {
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPluginsByMetadata()).thenReturn(newPlugin(BatchServiceProvider.class, ProjectServiceProvider.class));
    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"), new Settings());

    installer.install(container, InstantiationStrategy.PER_BATCH);

    assertThat(container.getComponentByType(BatchService.class)).isNotNull();
    assertThat(container.getComponentByType(ProjectService.class)).isNull();
    assertThat(container.getComponentByType(ServerService.class)).isNull();
  }


  @Test
  public void shouldNotInstallPluginsOnNonSupportedEnvironment() {
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPluginsByMetadata()).thenReturn(newPlugin(MavenService.class, BuildToolService.class));

    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(pluginRepository, new EnvironmentInformation("ant", "1.7"), new Settings());

    installer.install(container, InstantiationStrategy.PER_PROJECT);

    assertThat(container.getComponentByType(MavenService.class)).isNull();
    assertThat(container.getComponentByType(BuildToolService.class)).isNotNull();
  }

  @Test
  public void should_disable_maven_extensions_if_virtual_module_in_maven_project() {
    Project project = mock(Project.class);
    when(project.getPom()).thenReturn(null);
    BatchPluginRepository pluginRepository = mock(BatchPluginRepository.class);
    when(pluginRepository.getPluginsByMetadata()).thenReturn(newPlugin(MavenService.class));

    ComponentContainer container = new ComponentContainer();
    container.addSingleton(project);
    ExtensionInstaller installer = new ExtensionInstaller(pluginRepository, new EnvironmentInformation("maven", "2.2.1"), new Settings());

    installer.install(container, InstantiationStrategy.PER_PROJECT);

    assertThat(container.getComponentByType(MavenService.class)).isNull();
  }

  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  public static class BatchService implements BatchExtension {

  }

  public static class ProjectService implements BatchExtension {

  }

  public static class ServerService implements ServerExtension {

  }

  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  public static class BatchServiceProvider extends ExtensionProvider implements BatchExtension {

    @Override
    public Object provide() {
      return Arrays.<Object>asList(BatchService.class, ServerService.class);
    }
  }

  public static class ProjectServiceProvider extends ExtensionProvider implements BatchExtension {
    @Override
    public Object provide() {
      return ProjectService.class;
    }
  }

  @SupportedEnvironment("maven")
  public static class MavenService implements BatchExtension {

  }

  @SupportedEnvironment({"maven", "ant", "gradle"})
  public static class BuildToolService implements BatchExtension {

  }

}
